package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.*;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class MobileWalletController {
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    @FXML
    private Label brandLink, clockLabel;
    @FXML private javafx.scene.Parent root;
    private final I18nService i18n;
    private Timeline clock;
    @FXML private Label mobileWalletLabel;

    @FXML private Label panelTitle, processingLabel;
    @FXML private ProgressIndicator ring;
    @FXML private Button startBtn, cancelBtn;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");
    @Autowired
    private PaymentService paymentService;
    private final ApplicationContext appContext;
    private final PaymentSession paymentSession;

    public MobileWalletController(I18nService i18n, ApplicationContext appContext, PaymentSession paymentSession) {
        this.i18n = i18n;
        this.appContext = appContext;
        this.paymentSession = paymentSession;
    }

    @FXML
    private void initialize() {
        logger.info("Initializing PaymentController");
        // Live clock
        clock = new Timeline(
                new KeyFrame(Duration.ZERO, e ->
                        clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();


        Platform.runLater(() -> {
            var zoom = TextZoomService.get();
            zoom.register(brandLink,mobileWalletLabel, clockLabel, panelTitle, processingLabel, startBtn, cancelBtn);
        });
        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
        });
        updateTexts();
    }

    private void updateTexts() {
        mobileWalletLabel.setText(i18n.get("mobileWalletPayment.title"));
        panelTitle.setText(i18n.get("mobileWalletPayment.panelLine"));
        processingLabel.setText(i18n.get("mobileWalletPayment.processingText"));
        cancelBtn.setText(i18n.get("mobileWalletPayment.cancelButtonText"));
    }

    public void onVolume(ActionEvent actionEvent) {
        logger.info("Volume button pressed.");
        // hook your volume control here
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

    @FXML
    private void onCancel(ActionEvent event) {
        goTo("/Fxml/Payment.fxml", event);
    }

    private void goTo(String fxmlPath, ActionEvent event) {
        try {
            logger.info("Navigating to {}", fxmlPath);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(appContext::getBean);
            Parent view = loader.load();
            ((Node) event.getSource()).getScene().setRoot(view);
        } catch (IOException ex) {
            logger.error("Navigation failed to {}: {}", fxmlPath, ex.getMessage());
            ex.printStackTrace();
        }
    }

    @FXML
    private void onStart(ActionEvent e) {
        // Show "Processing..." UI
        processingLabel.setVisible(true);
        processingLabel.setManaged(true);
        ring.setVisible(true);
        ring.setManaged(true);
        startBtn.setDisable(true);
        cancelBtn.setDisable(true);

        // Simulate 5 seconds processing, then success page
        PauseTransition wait = new PauseTransition(Duration.seconds(5));
        wait.setOnFinished(evt -> goTo("/Fxml/PaymentSuccess.fxml", e));
        wait.play();
    }
}
