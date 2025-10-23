package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.ContrastManager;
import concordia.soen6611.igo_tvm.Services.I18nService;
import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.Services.TextZoomService;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Controller for the "Payment Success" screen shown after a successful transaction.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Display success/printing messages and provide actions to print the receipt or finish.</li>
 *   <li>Localize all texts via {@link I18nService} and register accessibility helpers
 *       ({@link TextZoomService}, {@link ContrastManager}).</li>
 *   <li>Maintain a live clock, and handle navigation to Home/Welcome screens.</li>
 *   <li>Clear {@link PaymentSession} when leaving this screen (print completion or Done).</li>
 * </ul>
 * <p>
 * Scope: Spring {@code prototype}; a fresh controller instance per view load.
 */
@Controller
@org.springframework.context.annotation.Scope("prototype")
public class PaymentSuccessController {

    /** i18n service for localized strings and formatting. */
    private final I18nService i18n;

    /** Receipt print and Done action buttons. */
    @FXML private Button printBtn;
    @FXML private Button doneBtn;

    /** Title and status lines at the top of the screen. */
    @FXML private Label successTitle;
    @FXML private Label printingLine;

    /** Labels adjacent to the print/done buttons (localized). */
    @FXML private Label printBtnLabel;
    @FXML private Label doneBtnLabel;

    /** "Help" affordance label in the footer. */
    @FXML private Label helpLabel;

    /** Confirmation/thanks line. */
    @FXML private Label confirmationLabel;

    /** Header/aux labels including brand link and live clock. */
    @FXML private Label brandLink, successTitleLabel,printingLineLabel, receiptInfoLabel, thankYouLabel, volumeLabel, clockLabel;

    /** Root node for attaching contrast handling. */
    @FXML private javafx.scene.Parent root;

    /** Spring application context used for controller-factory-backed navigation. */
    private final ApplicationContext appContext;

    /** Session container used to clear state on exit. */
    private final PaymentSession paymentSession;

    /**
     * Constructs the controller with required collaborators.
     *
     * @param appContext      Spring application context for navigation
     * @param i18n            internationalization service
     * @param paymentSession  session model to be cleared once the flow completes
     */
    public PaymentSuccessController(ApplicationContext appContext, I18nService i18n, PaymentSession paymentSession) {
        this.appContext = appContext;
        this.i18n = i18n;
        this.paymentSession = paymentSession;
    }

    /** Live clock timeline for the header. */
    private Timeline clock;

    /** Clock format used by {@link #clockLabel}. */
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a");

    /**
     * JavaFX initialization hook.
     * <ul>
     *   <li>Starts the live header clock.</li>
     *   <li>Applies localized text to all visible labels/buttons.</li>
     *   <li>Registers nodes with {@link TextZoomService} and attaches {@link ContrastManager}.</li>
     * </ul>
     */
    @FXML
    private void initialize() {

        clock = new Timeline(
                new KeyFrame(Duration.ZERO,
                        e -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        updateTexts();

        // Register text nodes for zooming
        Platform.runLater(() -> {
            TextZoomService.get().register(brandLink, confirmationLabel, successTitleLabel,
                    printingLineLabel, receiptInfoLabel, thankYouLabel, helpLabel, volumeLabel, clockLabel,
                    printBtn, doneBtn);
        });
        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
        });
    }

    /**
     * Applies localized strings to visible labels and buttons.
     */
    private void updateTexts() {
        successTitle.setText(i18n.get("paymentSuccess.success"));
        printingLine.setText(i18n.get("paymentSuccess.printing"));
        printBtnLabel.setText(i18n.get("paymentSuccess.printReceipt"));
        doneBtnLabel.setText(i18n.get("paymentSuccess.done"));
        helpLabel.setText(i18n.get("help"));
        confirmationLabel.setText(i18n.get("paymentSuccess.confirmation"));
    }

    /* ===== Actions ===== */

    /**
     * Handles the "Print receipt" action:
     * <ol>
     *   <li>Shows a localized modal indicating the receipt is being/has been printed.</li>
     *   <li>Disables action buttons while waiting.</li>
     *   <li>After 5 seconds, closes the modal, clears the {@link PaymentSession}, and navigates Home.</li>
     * </ol>
     *
     * @param event click event from the Print button
     */
    @FXML
    private void onPrintReceipt(ActionEvent event) {
        // Build an i18n modal
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(i18n.get("paymentSuccess.receipt.title")); // e.g., "Receipt"
        alert.setHeaderText(null);
        alert.setContentText(
                i18n.get("paymentSuccess.receipt.printed") + "\n" +  // "Receipt printed successfully."
                        i18n.get("paymentSuccess.redirect.in5")               // "Redirection in 5 seconds..."
        );
        alert.show();

        // Disable actions while waiting
        setButtonsDisabled(true);

        // Capture the originating Node *now*
        final Node origin = (Node) event.getSource();

        // After 5s, close modal & go Home
        PauseTransition wait = new PauseTransition(Duration.seconds(5));
        wait.setOnFinished(ae -> {
            alert.close();
            paymentSession.clear();
            goHome(origin);
        });
        wait.play();
    }

    /**
     * Finishes the success flow, clears the session, and returns to Home.
     *
     * @param event click event from the Done button
     */
    @FXML
    private void onDone(ActionEvent event) {
        // we are finished with this order
        paymentSession.clear();
        goHome((Node) event.getSource());
    }

    /* ===== Navigation helper ===== */

    /**
     * Loads and shows the Home screen by replacing the current scene's root.
     *
     * @param node any node currently in the scene (used to obtain the scene)
     */
    private void goHome(Node node) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/Home.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent view = loader.load();
            node.getScene().setRoot(view);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Enables or disables the Print/Done buttons.
     *
     * @param b {@code true} to disable; {@code false} to enable
     */
    private void setButtonsDisabled(boolean b) {
        if (printBtn != null) printBtn.setDisable(b);
        if (doneBtn  != null) doneBtn.setDisable(b);
    }

    /* Optional: footer volume/help handlers if you want */

    /**
     * Placeholder for a volume/TTS handler for kiosk accessibility.
     *
     * @param e action event from a volume control element
     */
    public void onVolume(ActionEvent e) { /* no-op */ }

    /**
     * Brand click handler—navigates to the welcome screen.
     *
     * @param event mouse click event from the brand label
     */
    @FXML
    private void onBrandClick(MouseEvent event) {
        goWelcomeScreen((Node) event.getSource());
    }

    /**
     * Loads the welcome screen and replaces the current scene root.
     *
     * @param anyNodeInScene any node in the active scene (to get the scene)
     */
    private void goWelcomeScreen(Node anyNodeInScene) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/welcome-screen.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent home = loader.load();
            anyNodeInScene.getScene().setRoot(home);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Shows a localized Help dialog with contact entries and copy-to-clipboard buttons.
     * Keeps custom modal CSS if available.
     */
    @FXML
    private void onHelpClick() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(i18n.get("home.help.dialogTitle"));  // i18n
        alert.setHeaderText(null);

        // ---- Header row (icon + localized title)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon  = new Label("🛠");
        icon.getStyleClass().add("help-icon");
        Label title = new Label(i18n.get("home.help.header"));  // i18n
        title.getStyleClass().add("help-title");
        header.getChildren().addAll(icon, title);

        // ---- Body (localized labels)
        VBox body = new VBox(8);
        body.getChildren().addAll(
                contactRow(i18n.get("home.help.phone"), "+1 (514) 555-0137"),
                contactRow(i18n.get("home.help.email"), "support@stm.example")
        );

        VBox content = new VBox(14, header, body);
        content.getStyleClass().add("help-content");

        DialogPane pane = alert.getDialogPane();
        pane.setContent(content);

        // Optional: keep your modal CSS
        try {
            pane.getStylesheets().add(getClass().getResource("/styles/Modal.css").toExternalForm());
        } catch (Exception ignored) {}
        pane.getStyleClass().add("help-modal");

        // Localized Close button
        ButtonType closeType = new ButtonType(i18n.get("home.help.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(closeType);
        Node closeBtn = pane.lookupButton(closeType);
        if (closeBtn != null) closeBtn.getStyleClass().add("help-close-btn");

        alert.showAndWait();
    }

    /**
     * Helper to build a single contact row (label, value, copy button) for dialogs.
     * Clicking the "Copy" button places the value onto the system clipboard.
     *
     * @param labelText localized field label (e.g., "Phone")
     * @param value     field value (e.g., phone number or email)
     * @return a left-aligned {@link HBox} representing one contact entry
     */
    // Small helper to render a row with a copy button
    private HBox contactRow(String labelText, String value) {
        Label label = new Label(labelText);
        label.getStyleClass().add("help-label");

        Label val = new Label(value);
        val.getStyleClass().add("help-value");

        Button copy = new Button(i18n.get("home.help.copy")); // i18n
        copy.getStyleClass().add("help-copy-btn");
        copy.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(value);
            Clipboard.getSystemClipboard().setContent(cc);
        });

        HBox row = new HBox(10, label, val, copy);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
