package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.I18nService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
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
    @FXML private Label brandLink;
    @FXML private Button btnFontSizeIn;
    @FXML private Button btnFontSizeOut;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");

    // scale state for this label
    private double basePx;            // computed after CSS is applied
    private double scale = 1.0;       // 1.0 = default
    private static final double STEP = 0.10;
    private static final double MIN  = 1.00;
    private static final double MAX  = 1.50;

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
            basePx = brandLink.getFont().getSize();  // picks up 24px from .brand in Home.css
            applyBrandScale();
            reflectButtons();
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
    private void onReload() { System.out.println("Reload Card clicked"); }
    @FXML
    private void onInfo()   { System.out.println("Info clicked"); }
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

    @FXML private void onFontSizeIn()  { scale = Math.min(MAX, scale + STEP); applyBrandScale(); reflectButtons(); }
    @FXML private void onFontSizeOut() { scale = Math.max(MIN, scale - STEP); applyBrandScale(); reflectButtons(); }

    private void applyBrandScale() {
        double px = basePx * scale;
        // Inline style overrides stylesheet rule (.brand) every time
        setInlineFontSize(brandLink, px);
    }

    private void reflectButtons() {
        btnFontSizeOut.setDisable(scale <= MIN);
        btnFontSizeIn.setDisable(scale >= MAX);
    }

    // ----- helpers -----
    private static void setInlineFontSize(Label node, double px) {
        // keep any existing inline styles except font-size, then add our font-size
        String s = node.getStyle();
        if (s == null) s = "";
        s = s.replaceAll("(?i)-fx-font-size\\s*:\\s*[^;]+;?", "");
        node.setStyle(s + String.format("-fx-font-size: %.1fpx;", px));
    }
}
