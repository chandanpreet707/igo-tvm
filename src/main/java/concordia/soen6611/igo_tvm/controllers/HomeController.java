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
    private Timeline clock;
    private final I18nService i18n;
    private final ApplicationContext appContext;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");

    @FXML private Button btnFontSizeIn, btnFontSizeOut;

    @FXML private Label brandLink;
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
        buyBtn.setAccessibleText("Buy new ticket / Acheter un nouveau titre");
        reloadBtn.setAccessibleText("Reload card / Recharger une carte");

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

//
        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
            reflectContrastButtons();
        });
    }

    @FXML private void onBuyTicket(ActionEvent event) {
        System.out.println("Buy New Ticket clicked; controller: " + System.identityHashCode(this));
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/BuyNewTicket.fxml"));
            loader.setControllerFactory(appContext::getBean);  // CRITICAL
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
    private void onVolume() { System.out.println("Volume clicked"); }

    // Call if you navigate away from this view
    public void shutdown() {
        if (clock != null) clock.stop();
    }

    @FXML
    public void onLanguageChange(ActionEvent event) {
        Object src = event.getSource();
        if (src == btnEN) {
            System.out.println("English button clicked");
            i18n.setLocale(Locale.ENGLISH);
        } else if (src == btnFR) {
            System.out.println("French button clicked");
            i18n.setLocale(Locale.FRENCH);
        }
        // Not strictly needed if you listen above, but harmless:
        updateTexts();
    }

    private void updateTexts() {
        System.out.println("=== updateTexts called ===");
        homeLabel.setText(i18n.get("home"));
        promptLabel.setText(i18n.get("prompt"));
        helpLabel.setText(i18n.get("help"));
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
        alert.setTitle("Information");
        alert.setHeaderText(null); // we'll use our own styled header row
        if (owner != null) alert.initOwner(owner);

        // ---- Styled header row (icon + title)
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        Label icon = new Label("ℹ");
        icon.getStyleClass().add("info-icon");

        Label title = new Label("How to use this ticket machine");
        title.getStyleClass().add("info-title");

        header.getChildren().addAll(icon, title);

        // ---- Body copy
        VBox bullets = new VBox(8);
        bullets.getStyleClass().add("info-list");
        // give the whole list a right margin of 32px
        VBox.setMargin(bullets, new Insets(0, 32, 0, 0));

        bullets.getChildren().addAll(
                item("Select “Buy New Ticket” or “Reload Card”."),
                item("Choose rider type (Adult, Student, Senior, Tourist) and the fare."),
                item("Adjust quantity, then tap “Make Payment”."),
                item("Pick payment method (Card or Cash) and follow the prompts."),
                item("Collect your ticket and (optionally) print a receipt.")
        );

        VBox content = new VBox(14, header, bullets);
        content.getStyleClass().add("info-content");

        // Put content into the dialog
        DialogPane pane = alert.getDialogPane();
        pane.setContent(content);

        // ---- Apply CSS to the dialog only
        pane.getStylesheets().add(
                getClass().getResource("/styles/Modal.css").toExternalForm()
        );
        pane.getStyleClass().add("info-modal"); // root class for this dialog

        // Single Close button
        alert.getButtonTypes().setAll(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));

        // Optional: style the Close button via CSS class
        Node closeBtn = pane.lookupButton(alert.getButtonTypes().get(0));
        closeBtn.getStyleClass().add("info-close-btn");

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

}
