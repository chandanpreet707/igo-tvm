package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.ContrastManager;
import concordia.soen6611.igo_tvm.Services.I18nService;
import concordia.soen6611.igo_tvm.Services.TextZoomService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controller for the kiosk Home (landing) screen.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Initialize and localize UI strings via {@link I18nService}.</li>
 *   <li>Maintain a live header clock.</li>
 *   <li>Provide navigation to the Buy Ticket and Card Reload flows.</li>
 *   <li>Expose accessibility controls (text zoom and contrast) and reflect their state.</li>
 *   <li>Show localized Information and Help dialogs with styled content.</li>
 * </ul>
 * <p>
 * Scope: Spring {@code prototype}—a fresh controller instance per view load.
 */
@Controller
@org.springframework.context.annotation.Scope("prototype")
public class HomeController {

    /** Clickable brand label; navigates to the welcome screen. */
    @FXML private Label brandLink;
    /** Button opening the Information dialog. */
    @FXML private Button informationButton;
    /** Root container for the scene; used to attach contrast manager. */
    @FXML private BorderPane root;
    /** "Home" title label. */
    @FXML private Label homeLabel;
    /** Prompt/subtitle label. */
    @FXML private Label promptLabel;
    /** "Help" label. */
    @FXML private Label helpLabel;
    /** Header clock label, updated every second. */
    @FXML private Label clockLabel;
    /** Primary action: navigate to Buy New Ticket. */
    @FXML private Button buyBtn;
    /** Primary action: navigate to Card Reload. */
    @FXML private Button reloadBtn;
    /** Language switch to English. */
    @FXML private Button btnEN;
    /** Language switch to French. */
    @FXML private Button btnFR;

    /** Volume button (placeholder handler). */
    @FXML private Button volumeBtn;
    /** "Information" section header label. */
    @FXML private Label informationLabel;

    /** Live clock timeline. */
    private Timeline clock;
    /** Internationalization service for UI strings and locale updates. */
    private final I18nService i18n;
    /** Spring application context used as controller factory during navigation. */
    private final ApplicationContext appContext;
    /** Clock format for header time display. */
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");

    /** Zoom-in and zoom-out controls. */
    @FXML private Button btnFontSizeIn, btnFontSizeOut;

    /** Shortcut label near Buy button. */
    @FXML private Label buyNewTicketLabel;
    /** Shortcut label near Reload button. */
    @FXML private Label reloadCardLabel;
    /** Contrast adjustment buttons. */
    @FXML private Button btnContrastUp, btnContrastDown;

    /**
     * Constructs the controller with required collaborators.
     *
     * @param i18n       i18n service to resolve localized strings and track locale
     * @param appContext Spring application context for controller-factory navigation
     */
    public HomeController(I18nService i18n, ApplicationContext appContext) {
        this.i18n = i18n;
        this.appContext = appContext;
    }

    /**
     * JavaFX initialization hook.
     * <ul>
     *   <li>Starts live clock updates.</li>
     *   <li>Sets accessibility text on primary buttons.</li>
     *   <li>Applies localized texts and re-applies on locale change.</li>
     *   <li>Registers nodes with {@link TextZoomService} and attaches {@link ContrastManager}.</li>
     *   <li>Reflects current zoom/contrast state in the buttons’ enablement.</li>
     * </ul>
     */
    @FXML
    private void initialize() {
        // Live clock
        clock = new Timeline(
                new KeyFrame(Duration.ZERO, e ->
                        clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // Accessibility
        buyBtn.setAccessibleText(i18n.get("home.buyBtn.accessible"));
        reloadBtn.setAccessibleText(i18n.get("home.reloadBtn.accessible"));

        updateTexts();
        i18n.localeProperty().addListener((obs, oldL, newL) -> {
            System.out.println("Locale changed from " + oldL + " to " + newL);
            updateTexts();
        });

        Platform.runLater(() -> {
            var zoom = TextZoomService.get();
            zoom.register(brandLink, homeLabel, promptLabel, helpLabel, clockLabel, buyNewTicketLabel, reloadCardLabel, informationButton);
            reflectZoomButtons();
        });

        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
            reflectContrastButtons();
        });
    }

    /**
     * Navigates to the Buy New Ticket screen.
     *
     * @param event click event from the Buy button
     */
    @FXML
    private void onBuyTicket(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/BuyNewTicket.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent view = loader.load();
            ((Node) event.getSource()).getScene().setRoot(view);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Navigates to the Card Reload screen.
     *
     * @param event click event from the Reload button
     */
    @FXML
    private void onReload(ActionEvent event) {
        System.out.println("Reload Card clicked");
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/CardReload.fxml"));
            loader.setControllerFactory(appContext::getBean);  // CRITICAL
            Parent view = loader.load();
            ((Node) event.getSource()).getScene().setRoot(view);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Volume button handler placeholder. Wire TTS or audio feedback here if required.
     */
    @FXML
    private void onVolume() { /* handle volume */ }

    /**
     * Stops the live clock. Call when tearing down the view.
     */
    public void shutdown() {
        if (clock != null) clock.stop();
    }

    /**
     * Language toggle handler for EN/FR. Updates the locale and reapplies localized text.
     *
     * @param event click event from the language button
     */
    @FXML
    public void onLanguageChange(ActionEvent event) {
        Object src = event.getSource();
        if (src == btnEN) {
            i18n.setLocale(Locale.ENGLISH);
        } else if (src == btnFR) {
            i18n.setLocale(Locale.FRENCH);
        }
        updateTexts();
    }

    /**
     * Applies localized text to labels, buttons, and tooltips.
     * Re-run whenever the locale changes.
     */
    private void updateTexts() {
        brandLink.setText(i18n.get("home.brand"));
        homeLabel.setText(i18n.get("home.title"));
        promptLabel.setText(i18n.get("home.prompt"));
        helpLabel.setText(i18n.get("home.help"));
//        buyBtn.setText(i18n.get("home.buyBtn.title"));
//        reloadBtn.setText(i18n.get("home.reloadBtn.title"));
        informationLabel.setText(i18n.get("home.information"));
        // Tooltips
        btnEN.setTooltip(new Tooltip(i18n.get("home.lang.en")));
        btnFR.setTooltip(new Tooltip(i18n.get("home.lang.fr")));
        informationButton.setTooltip(new Tooltip(i18n.get("home.info.tooltip")));
        volumeBtn.setTooltip(new Tooltip(i18n.get("home.volume.tooltip")));
    }

    /**
     * Brand click handler—navigates to the welcome screen.
     *
     * @param event mouse event from the brand label
     */
    @FXML
    private void onBrandClick(MouseEvent event) {
        goWelcomeScreen((Node) event.getSource());
    }

    /**
     * Loads the welcome screen and replaces the current scene root.
     *
     * @param anyNodeInScene any node in the current scene (to resolve the scene)
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
     * Shows a localized Information dialog with usage steps and optional custom CSS.
     */
    @FXML
    private void onInfo() {
        Window owner = buyBtn != null ? buyBtn.getScene().getWindow() : null;

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(i18n.get("home.info.dialogTitle"));     // "Information" / "Informations"
        alert.setHeaderText(null);
        if (owner != null) alert.initOwner(owner);

        // ---- Header row (icon + localized title)
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("ℹ");
        icon.getStyleClass().add("info-icon");

        Label title = new Label(i18n.get("home.info.title"));  // "How to use..." / "Comment utiliser..."
        title.getStyleClass().add("info-title");
        header.getChildren().addAll(icon, title);

        // ---- Body bullets (all localized)
        VBox bullets = new VBox(8);
        bullets.getStyleClass().add("info-list");
        VBox.setMargin(bullets, new Insets(0, 32, 0, 0));

        bullets.getChildren().addAll(
                item(i18n.get("home.info.step1")),
                item(i18n.get("home.info.step2")),
                item(i18n.get("home.info.step3")),
                item(i18n.get("home.info.step4")),
                item(i18n.get("home.info.step5"))
        );

        VBox content = new VBox(14, header, bullets);
        content.getStyleClass().add("info-content");

        DialogPane pane = alert.getDialogPane();
        pane.setContent(content);

        // keep your dialog CSS
        try {
            pane.getStylesheets().add(getClass().getResource("/styles/Modal.css").toExternalForm());
        } catch (Exception ignored) {}
        pane.getStyleClass().add("info-modal");

        // Localized Close button
        ButtonType closeType = new ButtonType(i18n.get("home.info.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(closeType);
        Node closeBtn = pane.lookupButton(closeType);
        if (closeBtn != null) closeBtn.getStyleClass().add("info-close-btn");

        alert.showAndWait();
    }

    /**
     * Creates a single bullet row (dot + text) for dialogs.
     *
     * @param text localized step text
     * @return left-aligned HBox representing one bullet row
     */
    // Small helper to create bullet rows
    private HBox item(String text) {
        Label dot = new Label("•");
        dot.getStyleClass().add("info-bullet");

        Label lbl = new Label(text);
        lbl.getStyleClass().add("info-text");

        HBox row = new HBox(10, dot, lbl);
        row.setAlignment(Pos.TOP_LEFT);
        return row;
    }

    /**
     * Zooms text size in and updates zoom button enabled state.
     */
    @FXML private void onFontSizeIn()  { TextZoomService.get().zoomIn();  reflectZoomButtons(); }

    /**
     * Zooms text size out and updates zoom button enabled state.
     */
    @FXML private void onFontSizeOut() { TextZoomService.get().zoomOut(); reflectZoomButtons(); }

    /**
     * Enables/disables zoom buttons based on {@link TextZoomService#getScale()} bounds.
     */
    private void reflectZoomButtons() {
        double s = TextZoomService.get().getScale();
        btnFontSizeOut.setDisable(s <= 1.00);
        btnFontSizeIn.setDisable(s >= 1.50);
    }

    /**
     * Increases contrast and refreshes contrast button states.
     */
    @FXML private void onContrastUp() {
        ContrastManager.getInstance().increase();
        reflectContrastButtons();
    }

    /**
     * Decreases contrast and refreshes contrast button states.
     */
    @FXML private void onContrastDown() {
        ContrastManager.getInstance().decrease();
        reflectContrastButtons();
    }

    /**
     * Enables/disables contrast buttons based on current {@link ContrastManager} level.
     */
    private void reflectContrastButtons() {
        double lvl = ContrastManager.getInstance().getLevel();
        btnContrastDown.setDisable(lvl <= -0.40);
        btnContrastUp.setDisable(lvl >=  0.60);
    }

    /**
     * Shows a localized Help dialog with contact rows and copy-to-clipboard actions.
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
     * Builds a single contact row with a label, value, and "Copy" button that copies the value to the clipboard.
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
