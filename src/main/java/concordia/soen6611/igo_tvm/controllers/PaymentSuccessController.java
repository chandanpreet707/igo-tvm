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

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class PaymentSuccessController {

    private final I18nService i18n;
    @FXML private Button printBtn;
    @FXML private Button doneBtn;
    @FXML private Label successTitle;
    @FXML private Label printingLine;
    @FXML private Label printBtnLabel;
    @FXML private Label doneBtnLabel;
    @FXML private Label helpLabel;

    @FXML private Label confirmationLabel;


    @FXML private Label brandLink, successTitleLabel,printingLineLabel, receiptInfoLabel, thankYouLabel, volumeLabel, clockLabel;
    @FXML private javafx.scene.Parent root;
    private final ApplicationContext appContext;
    private final PaymentSession paymentSession;

    public PaymentSuccessController(ApplicationContext appContext, I18nService i18n, PaymentSession paymentSession) {
        this.appContext = appContext;
        this.i18n = i18n;
        this.paymentSession = paymentSession;
    }
    private Timeline clock;
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a");
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

    private void updateTexts() {
        successTitle.setText(i18n.get("paymentSuccess.success"));
        printingLine.setText(i18n.get("paymentSuccess.printing"));
        printBtnLabel.setText(i18n.get("paymentSuccess.printReceipt"));
        doneBtnLabel.setText(i18n.get("paymentSuccess.done"));
        helpLabel.setText(i18n.get("help"));
        confirmationLabel.setText(i18n.get("paymentSuccess.confirmation"));
    }

    /* ===== Actions ===== */

    @FXML
    private void onPrintReceipt(ActionEvent event) {
        // Modal popup
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Receipt");
        alert.setHeaderText(null);
        alert.setContentText("Receipt printed successfully.\n"
                + "Redirection in 5 seconds...");
        // show non-blocking
        alert.show();

        // disable actions while waiting
        setButtonsDisabled(true);

        // after 5s, close modal & go Home
        PauseTransition wait = new PauseTransition(Duration.seconds(5));
        wait.setOnFinished(e -> {
            alert.close();
            paymentSession.clear();
            goHome((Node) e.getSource());
        });
        wait.play();
    }

    @FXML
    private void onDone(ActionEvent event) {
        // we are finished with this order
        paymentSession.clear();
        goHome((Node) event.getSource());
    }

    /* ===== Navigation helper ===== */
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

    private void setButtonsDisabled(boolean b) {
        if (printBtn != null) printBtn.setDisable(b);
        if (doneBtn  != null) doneBtn.setDisable(b);
    }



    /* Optional: footer volume/help handlers if you want */
    public void onVolume(ActionEvent e) { /* no-op */ }

    @FXML
    private void onBrandClick(MouseEvent event) {
        goWelcomeScreen((Node) event.getSource());
    }

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

    @FXML
    private void onHelpClick() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Need help?");
        alert.setHeaderText(null);

        // ---- Header row (icon + title)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("🛠");
        icon.getStyleClass().add("help-icon");
        Label title = new Label("If you run into any issues");
        title.getStyleClass().add("help-title");
        header.getChildren().addAll(icon, title);

        // ---- Body (contact lines + copy buttons)
        VBox body = new VBox(8);
        body.getChildren().addAll(
                contactRow("Phone:", "+1 (514) 555-0137"),
                contactRow("Email:", "support@stm.example")
        );

        VBox content = new VBox(14, header, body);
        content.getStyleClass().add("help-content");

        DialogPane pane = alert.getDialogPane();
        pane.setContent(content);

        // Optional: reuse your modal CSS if you have it
        try {
            pane.getStylesheets().add(
                    getClass().getResource("/styles/Modal.css").toExternalForm()
            );
        } catch (Exception ignored) {}
        pane.getStyleClass().add("help-modal");

        alert.getButtonTypes().setAll(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));
        Node closeBtn = pane.lookupButton(alert.getButtonTypes().get(0));
        closeBtn.getStyleClass().add("help-close-btn");

        alert.showAndWait();
    }

    // Small helper to render a row with a copy button
    private HBox contactRow(String labelText, String value) {
        Label label = new Label(labelText);
        label.getStyleClass().add("help-label");

        Label val = new Label(value);
        val.getStyleClass().add("help-value");

        Button copy = new Button("Copy");
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
