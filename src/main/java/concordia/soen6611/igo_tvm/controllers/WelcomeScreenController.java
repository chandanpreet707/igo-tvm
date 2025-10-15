// Java
package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.I18nService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
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
    private final ApplicationContext appContext;
    public WelcomeScreenController(I18nService i18n, ApplicationContext appContext) {
        this.i18n = i18n;
        this.appContext = appContext;
    }


    @FXML
    public void initialize() {
        System.out.println("Welcome Controller initialized: " + System.identityHashCode(this));
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
        try {
            // Load the Home page FXML
            System.out.println("Start purchase clicked. Current locale: " + i18n.getLocale());
            // Load the Home page FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/Home.fxml"));
            loader.setControllerFactory(appContext::getBean); // let Spring create controllers
            Parent homeRoot = loader.load();

            // EITHER: swap root (keeps size and styles)
            Scene scene = ((Node) event.getSource()).getScene();
            scene.setRoot(homeRoot);
        } catch (IOException ex) {
            ex.printStackTrace();
            // might show an alert instead of printing stack trace.
        }
    }
}
