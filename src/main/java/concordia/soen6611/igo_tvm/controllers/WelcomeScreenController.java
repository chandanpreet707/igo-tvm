// Java
package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.I18nService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.springframework.stereotype.Controller;

import java.util.Locale;

@Controller
public class WelcomeScreenController {

    private final I18nService i18n;

    @FXML private Label welcomeLabel;
    @FXML private Label titleLabel;
    @FXML private Label subtitleLabel;
    @FXML private Label languageSelectLabel;
    @FXML private Button startButton;
    @FXML private Button englishButton;
    @FXML private Button frenchButton;

    public WelcomeScreenController(I18nService i18n) {
        this.i18n = i18n;
    }


    @FXML
    public void initialize() {
        System.out.println("Controller initialized");
        System.out.println("I18nService: " + i18n);
        System.out.println("MessageSource: " + i18n.messages);
        System.out.println("Current locale: " + i18n.getLocale());
        updateTexts();
        i18n.localeProperty().addListener((obs, oldL, newL) -> {
            System.out.println("Locale changed from " + oldL + " to " + newL);
            updateTexts();
        });
    }

    @FXML
    public void onLanguageChange(ActionEvent event) {
        Object src = event.getSource();
        if (src == englishButton) {
            System.out.println("English button clicked");
            i18n.setLocale(Locale.ENGLISH);
        } else if (src == frenchButton) {
            System.out.println("French button clicked");
            i18n.setLocale(Locale.FRENCH);
        }
        // Not strictly needed if you listen above, but harmless:
        updateTexts();
    }

    private void updateTexts() {
        System.out.println("=== updateTexts called ===");
        System.out.println("Current locale: " + i18n.getLocale());
        String welcome = i18n.get("welcome");
        System.out.println("'welcome' key -> " + welcome);
        welcomeLabel.setText(welcome);
        titleLabel.setText(i18n.get("title"));
        subtitleLabel.setText(i18n.get("subtitle"));
        languageSelectLabel.setText(i18n.get("selectLanguage"));
        startButton.setText(i18n.get("start"));
    }

    @FXML
    public void onStartPurchase(ActionEvent event) {
        System.out.println("Start purchase clicked. Current locale: " + i18n.getLocale());
        // TODO: navigate to next screen
    }
}
