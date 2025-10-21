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

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class HomeController {

    @FXML private Label brandLink;
    @FXML private Button informationButton;
    @FXML private BorderPane root;
    @FXML private Label homeLabel;
    @FXML private Label promptLabel;
    @FXML private Label helpLabel;
    @FXML private Label clockLabel;
    @FXML private Button buyBtn;
    @FXML private Button reloadBtn;
    @FXML private Button btnEN;
    @FXML private Button btnFR;

    @FXML private Button volumeBtn;
    @FXML private Label informationLabel;

    private Timeline clock;
    private final I18nService i18n;
    private final ApplicationContext appContext;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");

    @FXML private Button btnFontSizeIn, btnFontSizeOut;

    @FXML private Label buyNewTicketLabel;
    @FXML private Label reloadCardLabel;
    @FXML private Button btnContrastUp, btnContrastDown;

    public HomeController(I18nService i18n, ApplicationContext appContext) {
        this.i18n = i18n;
        this.appContext = appContext;
    }

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
    @FXML
    private void onVolume() { /* handle volume */ }

    public void shutdown() {
        if (clock != null) clock.stop();
    }

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

    @FXML private void onFontSizeIn()  { TextZoomService.get().zoomIn();  reflectZoomButtons(); }
    @FXML private void onFontSizeOut() { TextZoomService.get().zoomOut(); reflectZoomButtons(); }

    private void reflectZoomButtons() {
        double s = TextZoomService.get().getScale();
        btnFontSizeOut.setDisable(s <= 1.00);
        btnFontSizeIn.setDisable(s >= 1.50);
    }

    @FXML private void onContrastUp() {
        ContrastManager.getInstance().increase();
        reflectContrastButtons();
    }

    @FXML private void onContrastDown() {
        ContrastManager.getInstance().decrease();
        reflectContrastButtons();
    }

    private void reflectContrastButtons() {
        double lvl = ContrastManager.getInstance().getLevel();
        btnContrastDown.setDisable(lvl <= -0.40);
        btnContrastUp.setDisable(lvl >=  0.60);
    }

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
