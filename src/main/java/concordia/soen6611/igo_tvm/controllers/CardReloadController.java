package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.*;
import concordia.soen6611.igo_tvm.exceptions.*;
import concordia.soen6611.igo_tvm.models.ExceptionDialog;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Controller for the "Card Reload" entry screen.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Initialize localized UI, accessibility helpers (text zoom and contrast), and a live header clock.</li>
 *   <li>Start a simulated/async card read via {@link CardReloadService#readCardAsync(boolean)}.</li>
 *   <li>Optionally simulate error scenarios and display user-friendly exception dialogs.</li>
 *   <li>On success, navigate to the reload amount screen; on failure, reset the UI.</li>
 * </ul>
 * <p>
 * Notes:
 * <ul>
 *   <li>All user-visible strings come from {@link I18nService}; listeners re-apply text on locale change.</li>
 *   <li>Known errors extend {@link AbstractCustomException} and are shown via {@link ExceptionDialog}.</li>
 * </ul>
 */
@Controller
@org.springframework.context.annotation.Scope("prototype")
public class CardReloadController {

    /** Root container; used to attach contrast manager. */
    public BorderPane root;

    /** Clickable brand link; returns to the welcome screen. */
    public Label brandLink;

    /** Instructional label prompting the user to tap their card. */
    public Label tapYouCardLabel;

    /** Screen title label. */
    public Label reloadCardLabel;

    /** Button to start the card-reading sequence. */
    @FXML
    private Button startReadBtn;

    /** Progress indicator shown while the card read is in progress. */
    @FXML
    private ProgressIndicator readProgress;

    /** Status label reflecting the current read state (ready/reading/done/failed). */
    @FXML
    private Label readStatus;

    /** Live clock timeline that updates {@link #clockLabel} every second. */
    private Timeline clock;

    /** Header clock label showing current date/time. */
    @FXML
    private Label clockLabel;

    /** i18n service to resolve localized strings and watch locale changes. */
    private final I18nService i18n;

    /** Fare service (not used directly here for pricing but injected for consistency/potential use). */
    private final FareRateService fareRateService;

    /** Spring application context for controller-factory backed navigation. */
    private final ApplicationContext appContext;

    /** Clock display format. */
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");

    /** Session model used to carry origin/state between screens. */
    private final PaymentSession paymentSession;

    /** Service that performs (or simulates) card read operations. */
    private final CardReloadService cardReloadService;

    /**
     * Constructs the controller with required collaborators.
     *
     * @param i18n               internationalization service
     * @param fareRateService    fare rate service
     * @param appContext         Spring application context for navigation
     * @param paymentSession     session container for cross-screen state
     * @param cardReloadService  async/simulated card read service
     */
    public CardReloadController(I18nService i18n,
                                FareRateService fareRateService,
                                ApplicationContext appContext,
                                PaymentSession paymentSession,
                                CardReloadService cardReloadService) {
        this.i18n = i18n;
        this.fareRateService = fareRateService;
        this.appContext = appContext;
        this.paymentSession = paymentSession;
        this.cardReloadService = cardReloadService;
    }

    /**
     * JavaFX initialization hook. Sets up:
     * <ul>
     *   <li>Localized text for all visible labels.</li>
     *   <li>A locale-change listener to re-apply localized text.</li>
     *   <li>A live clock that refreshes {@link #clockLabel} every second.</li>
     *   <li>Accessibility helpers: {@link TextZoomService} and {@link ContrastManager}.</li>
     * </ul>
     */
    @FXML
    private void initialize() {
        updateTexts();
        i18n.localeProperty().addListener((obs, oldL, newL) -> updateTexts());

        // Live clock
        clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        Platform.runLater(() -> {
            var zoom = TextZoomService.get();
            zoom.register(brandLink, reloadCardLabel, clockLabel, tapYouCardLabel, readStatus, reloadCardLabel);
        });

        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
        });
    }

    /**
     * Applies localized text to visible labels on this screen.
     * <p>
     * Called on initialization and again whenever the locale changes.
     */
    private void updateTexts() {
        reloadCardLabel.setText(i18n.get("cardReload.title"));
        tapYouCardLabel.setText(i18n.get("cardReload.message"));
        readStatus.setText(i18n.get("cardReload.readyToReadMessage"));

    }

    /**
     * Delegates to {@link CardReloadService} to obtain a fare for a given rider/pass combination.
     *
     * @param riderType rider type (e.g., "Adult", "Student")
     * @param passType  pass type (e.g., "Single Trip", "Monthly Pass")
     * @return fare amount for one unit of the specified rider/pass
     */
    public double getFare(String riderType, String passType) {
        return cardReloadService.getFare(riderType, passType);
    }

    /**
     * Brand click handler. Clears the {@link PaymentSession} and navigates to the welcome screen.
     *
     * @param event mouse event originating from the brand link
     */
    @FXML
    private void onBrandClick(MouseEvent event) {
        paymentSession.clear();
        goWelcomeScreen((Node) event.getSource());
    }

    /**
     * Loads and displays the welcome screen by replacing the current scene's root.
     *
     * @param anyNodeInScene any node belonging to the current scene
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
     * Back button handler. Navigates to the Home screen.
     *
     * @param event action event from the Back button
     */
    public void onBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/Home.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent home = loader.load();
            ((Node) event.getSource()).getScene().setRoot(home);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Placeholder for volume/TTS integration if kiosk requires audible guidance.
     *
     * @param actionEvent action event from a volume control
     */
    public void onVolume(ActionEvent actionEvent) {
    }

    /**
     * Starts the (simulated) card reading flow. Shows a scenario picker to simulate success
     * or specific error types, updates the UI to a "reading" state, and handles completion:
     * <ul>
     *   <li>On success: shows an info alert, then navigates to the amount-selection screen.</li>
     *   <li>On failure: displays a localized {@link ExceptionDialog} and resets the UI.</li>
     * </ul>
     *
     * @param event action event from the "Start Reading" button
     */
    @FXML
    private void onStartReading(javafx.event.ActionEvent event) {
        Map<String, String> optionMap = new HashMap<>();
        optionMap.put("success", i18n.get("cardReload.option.success"));
        optionMap.put("network", i18n.get("cardReload.option.network"));
        optionMap.put("hardware", i18n.get("cardReload.option.hardware"));
        optionMap.put("database", i18n.get("cardReload.option.database"));
        optionMap.put("user", i18n.get("cardReload.option.user"));

        List<String> translatedOptions = new ArrayList<>(optionMap.values());

        String defaultTranslatedOption = optionMap.get("success");

        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                defaultTranslatedOption,
                translatedOptions
        );

        dialog.setTitle(i18n.get("cardReload.simulation.title"));
        dialog.setHeaderText(i18n.get("cardReload.simulation.header"));
        dialog.setContentText(i18n.get("cardReload.simulation.scenario"));

        Optional<String> result = dialog.showAndWait();

        if (result.isEmpty()) {
            return;
        }

        String translatedChoice = result.get();

        String choice = optionMap.entrySet().stream()
                .filter(entry -> entry.getValue().equals(translatedChoice))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse("success"); // Default to success if mapping somehow fails

        startReadBtn.setDisable(true);
        readProgress.setVisible(true);
        readProgress.setManaged(true);
        readStatus.setText(i18n.get("cardReload.readingStartedMessage"));

        cardReloadService.readCardAsync(false) // Use false for normal flow
                .thenCompose(v -> {
                    // After the read completes, check if we should simulate an exception
                    if (!choice.equals("success")) {
                        // Create a failed future with the appropriate exception
                        CompletableFuture<Void> failedFuture = new CompletableFuture<>();
                        switch (choice) {
                            case "network":
                                // Translated message
                                failedFuture.completeExceptionally(new NetworkException(
                                        i18n.get("cardReload.error.network")
                                ));
                                break;
                            case "hardware":
                                // Translated message
                                failedFuture.completeExceptionally(new HardwareException(
                                        i18n.get("cardReload.error.hardware")
                                ));
                                break;
                            case "database":
                                // Translated message
                                failedFuture.completeExceptionally(new DatabaseException(
                                        i18n.get("cardReload.error.database")
                                ));
                                break;
                            case "user":
                                // Translated message
                                failedFuture.completeExceptionally(new UserException(
                                        i18n.get("cardReload.error.user")
                                ));
                                break;
                        }
                        return failedFuture; // <-- Return the potentially failed future
                    }
                    return CompletableFuture.completedFuture(null);
                })
                .thenRun(() -> Platform.runLater(() -> {
                    readStatus.setText(i18n.get("cardReload.readingDoneMessage"));
                    readProgress.setVisible(false);
                    readProgress.setManaged(false);

                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle(i18n.get("cardReload.modalTitle"));
                    ok.setHeaderText(null);
                    ok.setContentText(i18n.get("cardReload.readingSuccessfulMessage"));

                    Button okButton = (Button) ok.getDialogPane().lookupButton(ButtonType.OK);
                    if (okButton != null) {
                        okButton.setText(i18n.get("cardReload.ok"));
                    }

                    ok.show();

                    PauseTransition after = new PauseTransition(Duration.seconds(2));
                    after.setOnFinished(x -> {
                        ok.close();
                        goNext((Node) event.getSource());
                    });
                    after.play();
                }))
                .exceptionally(t -> {
                    Throwable cause = (t instanceof CompletionException && t.getCause() != null) ? t.getCause() : t;
                    Platform.runLater(() -> {
                        // Show dialog for known AbstractCustomException or wrap otherwise
                        if (cause instanceof AbstractCustomException) {
                            ExceptionDialog.show((AbstractCustomException) cause, ((Node) event.getSource()).getScene().getWindow(), appContext);
                        } else {
                            ExceptionDialog.showAsUserException(cause, ((Node) event.getSource()).getScene().getWindow(), appContext);
                        }
                        // Reset UI
                        startReadBtn.setDisable(false);
                        readProgress.setVisible(false);
                        readProgress.setManaged(false);
                        readStatus.setText(i18n.get("cardReload.readingFailedMessage"));
                    });
                    return null;
                });
    }

    /**
     * Navigates to the amount-selection screen after a successful read.
     * <p>
     * If loading fails, re-enables the start button to allow the user to retry.
     *
     * @param source any node in the current scene (used to resolve the scene/root)
     */
    private void goNext(Node source) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/CardReloadAmount.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent next = loader.load();
            source.getScene().setRoot(next);
        } catch (Exception ex) {
            ex.printStackTrace();
            startReadBtn.setDisable(false);
        }
    }
}
