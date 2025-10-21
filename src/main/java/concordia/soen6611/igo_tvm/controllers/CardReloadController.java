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
        if (startReadBtn != null) {
            startReadBtn.setTooltip(new Tooltip(i18n.get("cardReload.startTooltip")));
        }
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
    private void onStartReading(ActionEvent event) {
        // --- Localized option labels
        String L_SUCCESS  = i18n.get("cardReload.sim.success");
        String L_NETWORK  = i18n.get("cardReload.sim.network");
        String L_HARDWARE = i18n.get("cardReload.sim.hardware");
        String L_DATABASE = i18n.get("cardReload.sim.database");
        String L_USER     = i18n.get("cardReload.sim.user");

        // Map shown label -> internal code (stable)
        java.util.Map<String, String> labelToCode = new java.util.LinkedHashMap<>();
        labelToCode.put(L_SUCCESS,  "success");
        labelToCode.put(L_NETWORK,  "network");
        labelToCode.put(L_HARDWARE, "hardware");
        labelToCode.put(L_DATABASE, "database");
        labelToCode.put(L_USER,     "user");

        // Build the dialog with localized strings
        ChoiceDialog<String> dialog = new ChoiceDialog<>(
                L_SUCCESS,
                L_SUCCESS, L_NETWORK, L_HARDWARE, L_DATABASE, L_USER
        );
        dialog.setTitle(i18n.get("cardReload.sim.title"));
        dialog.setHeaderText(i18n.get("cardReload.sim.header"));
        dialog.setContentText(i18n.get("cardReload.sim.prompt"));

        // Localize buttons
        ButtonType okType = new ButtonType(i18n.get("common.ok"), ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType(i18n.get("common.cancel"), ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().setAll(okType, cancelType);

        java.util.Optional<String> result = dialog.showAndWait();
        if (result.isEmpty()) return;

        // Translate back to internal code
        String choice = labelToCode.getOrDefault(result.get(), "success");

        // UI: show spinner & status
        startReadBtn.setDisable(true);
        readProgress.setVisible(true);
        readProgress.setManaged(true);
        readStatus.setText(i18n.get("cardReload.readingStartedMessage"));

        cardReloadService.readCardAsync(false)
                .thenCompose(v -> {
                    if (!"success".equals(choice)) {
                        java.util.concurrent.CompletableFuture<Void> failed = new java.util.concurrent.CompletableFuture<>();
                        switch (choice) {
                            case "network":  failed.completeExceptionally(new NetworkException(i18n.get("cardReload.networkException")));  break;
                            case "hardware": failed.completeExceptionally(new HardwareException(i18n.get("cardReload.hardwareException"))); break;
                            case "database": failed.completeExceptionally(new DatabaseException(i18n.get("cardReload.databaseException"))); break;
                            case "user":     failed.completeExceptionally(new UserException(i18n.get("cardReload.networkException")));         break;
                        }
                        return failed;
                    }
                    return java.util.concurrent.CompletableFuture.completedFuture(null);
                })
                .thenRun(() -> Platform.runLater(() -> {
                    readStatus.setText(i18n.get("cardReload.readingDoneMessage"));
                    readProgress.setVisible(false);
                    readProgress.setManaged(false);

                    Alert ok = new Alert(Alert.AlertType.INFORMATION);
                    ok.setTitle(i18n.get("cardReload.modalTitle"));
                    ok.setHeaderText(null);
                    ok.setContentText(i18n.get("cardReload.readingSuccessfulMessage"));

                    // Localize OK button
                    ButtonType okOnly = new ButtonType(i18n.get("common.ok"), ButtonBar.ButtonData.OK_DONE);
                    ok.getButtonTypes().setAll(okOnly);

                    ok.show();

                    PauseTransition after = new PauseTransition(Duration.seconds(2));
                    after.setOnFinished(x -> {
                        ok.close();
                        goNext(((Node) event.getSource()));
                    });
                    after.play();
                }))
                .exceptionally(t -> {
                    Throwable cause = (t instanceof java.util.concurrent.CompletionException && t.getCause() != null) ? t.getCause() : t;
                    Platform.runLater(() -> {
                        if (cause instanceof AbstractCustomException) {
                            ExceptionDialog.show((AbstractCustomException) cause, ((Node) event.getSource()).getScene().getWindow(), appContext);
                        } else {
                            ExceptionDialog.showAsUserException(cause, ((Node) event.getSource()).getScene().getWindow(), appContext);
                        }
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
