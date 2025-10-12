package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.I18nService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
public class HomeController {

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
    }

    // --- Actions (wire to your navigation) ---
    @FXML private void onBuy()    { System.out.println("Buy New Ticket clicked"); }
    @FXML private void onReload() { System.out.println("Reload Card clicked"); }
    @FXML private void onInfo()   { System.out.println("Info clicked"); }
    @FXML private void onVolume() { System.out.println("Volume clicked"); }

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
}
