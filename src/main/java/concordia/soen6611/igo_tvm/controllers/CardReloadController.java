package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.ContrastManager;
import concordia.soen6611.igo_tvm.Services.I18nService;
import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.Services.TextZoomService;
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
    @FXML private Button startReadBtn;
    @FXML private ProgressIndicator readProgress;
    @FXML private Label readStatus;
    private Timeline clock;
    @FXML private Label clockLabel;
    private final I18nService i18n;

    private final ApplicationContext appContext;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");

    private final PaymentSession paymentSession;

    public CardReloadController(I18nService i18n, ApplicationContext appContext, PaymentSession paymentSession) {
        this.i18n = i18n;
        this.appContext = appContext;
        this.paymentSession = paymentSession;
    }
    @FXML
    private void initialize() {
        updateTexts();
        i18n.localeProperty().addListener((obs, oldL, newL) -> {
            System.out.println("Locale changed from " + oldL + " to " + newL);
            updateTexts();
        });

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
        // UI: show spinner & status
        startReadBtn.setDisable(true);
        readProgress.setVisible(true);
        readProgress.setManaged(true);
        readStatus.setText(i18n.get("cardReload.readingStartedMessage"));

        // Simulate 10-second read
        PauseTransition wait = new PauseTransition(Duration.seconds(5));
        wait.setOnFinished(e -> {
            readStatus.setText(i18n.get("cardReload.readingDoneMessage"));
            readProgress.setVisible(false);
            readProgress.setManaged(false);

            // Inform user
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle(i18n.get("cardReload.modalTitle"));
            ok.setHeaderText(null);
            ok.setContentText(i18n.get("cardReload.readingSuccessfulMessage"));

            Button okButton = (Button) ok.getDialogPane().lookupButton(ButtonType.OK);
            if (okButton != null) {
                okButton.setText(i18n.get("cardReload.ok"));
            }

            ok.show();
            // Short pause so they see the dialog, then go to next page
            PauseTransition after = new PauseTransition(Duration.seconds(2));
            after.setOnFinished(x -> {
                ok.close();
                goNext((Node) event.getSource());
            });
            after.play();
        });
        wait.play();
    }

    /** Load your next page after reading completes. */
    private void goNext(Node source) {
        try {
            // TODO: change to your real "next" screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/CardReloadAmount.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent next = loader.load();
            source.getScene().setRoot(next);
        } catch (Exception ex) {
            ex.printStackTrace();
            // Fallback: just re-enable the button if navigation fails
            startReadBtn.setDisable(false);
        }
    }
}
