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

/**
 * Controller for the welcome (splash) screen.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Display localized welcome/title/subtitle text using {@link I18nService}.</li>
 *   <li>Allow the user to switch between English and French.</li>
 *   <li>Navigate to the Home screen when the user starts.</li>
 * </ul>
 * <p>
 * Scope: standard Spring {@code @Controller}. Controllers are provided through
 * {@link FXMLLoader#setControllerFactory(javafx.util.Callback)} with the {@link ApplicationContext}.
 */
@Controller
public class WelcomeScreenController {

    /** Internationalization service providing string lookups and locale state. */
    private final I18nService i18n;

    /** Top-line welcome text. */
    @FXML private Label welcomeLabel;
    /** Main title label. */
    @FXML private Label titleLabel;
    /** Subtitle/description label. */
    @FXML private Label subtitleLabel;
    /** Label preceding language selection controls. */
    @FXML private Label languageSelectLabel;
    /** Primary action button to begin the purchase flow. */
    @FXML private Button startButton;
    /** Button to switch locale to English. */
    @FXML private Button englishButton;
    /** Button to switch locale to French. */
    @FXML private Button frenchButton;

    /** Spring application context, used as controller factory for navigation. */
    private final ApplicationContext appContext;

    /**
     * Constructs the controller with required collaborators.
     *
     * @param i18n        the internationalization service
     * @param appContext  Spring application context used for controller-factory navigation
     */
    public WelcomeScreenController(I18nService i18n, ApplicationContext appContext) {
        this.i18n = i18n;
        this.appContext = appContext;
    }

    /**
     * JavaFX lifecycle hook.
     * <ul>
     *   <li>Applies initial localized text to UI labels/buttons.</li>
     *   <li>Registers a locale listener to re-apply text when the language changes.</li>
     * </ul>
     */
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

    /**
     * Handles clicks on the language buttons and updates the current locale.
     * Re-applies localized strings afterward.
     *
     * @param event the action event from a language button
     */
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

    /**
     * Applies localized strings to all visible labels and buttons on this screen.
     * Uses keys: {@code welcome}, {@code title}, {@code subtitle}, {@code selectLanguage}, {@code start}.
     */
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

    /**
     * Starts the purchase flow by navigating to the Home screen.
     * The current scene root is replaced with the Home view.
     *
     * @param event action event from the Start button
     */
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
