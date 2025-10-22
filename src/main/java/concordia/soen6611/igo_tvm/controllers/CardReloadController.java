package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.CardReloadService;
import concordia.soen6611.igo_tvm.Services.ContrastManager;
import concordia.soen6611.igo_tvm.Services.FareRateService;
import concordia.soen6611.igo_tvm.Services.I18nService;
import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.Services.TextZoomService;
import concordia.soen6611.igo_tvm.exceptions.*;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class CardReloadController {
    public BorderPane root;
    public Label brandLink;
    public Label tapYouCardLabel;
    public Label reloadCardLabel;
    @FXML
    private Button startReadBtn;
    @FXML
    private ProgressIndicator readProgress;
    @FXML
    private Label readStatus;
    private Timeline clock;
    @FXML
    private Label clockLabel;
    private final I18nService i18n;
    private final FareRateService fareRateService;
    private final ApplicationContext appContext;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");

    private final PaymentSession paymentSession;
    private final CardReloadService cardReloadService;

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

    private void updateTexts() {
        reloadCardLabel.setText(i18n.get("cardReload.title"));
        tapYouCardLabel.setText(i18n.get("cardReload.message"));
        readStatus.setText(i18n.get("cardReload.readyToReadMessage"));

    }

    public double getFare(String riderType, String passType) {
        return cardReloadService.getFare(riderType, passType);
    }

    @FXML
    private void onBrandClick(MouseEvent event) {
        paymentSession.clear();
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

    public void onVolume(ActionEvent actionEvent) {
    }


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
