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

/**
 * Controller for the Mobile Wallet payment flow.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Initialize localized UI labels and accessibility helpers (text zoom, contrast).</li>
 *   <li>Show a live clock in the header.</li>
 *   <li>Simulate a mobile wallet processing step and navigate to success.</li>
 *   <li>Handle navigation back to the Payment screen or the welcome screen.</li>
 * </ul>
 * <p>
 * Scope: Spring {@code prototype}; a new instance is created per view load.
 */
@Controller
@org.springframework.context.annotation.Scope("prototype")
public class MobileWalletController {

    /** Class logger (aligned with PaymentController for consistency in log streams). */
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    /** Clickable brand link and header clock label. */
    @FXML
    private Label brandLink, clockLabel;

    /** Root node for attaching contrast management. */
    @FXML private javafx.scene.Parent root;

    /** i18n service for localized strings. */
    private final I18nService i18n;

    /** Ticking timeline for the live header clock. */
    private Timeline clock;

    /** Screen title label (e.g., "Mobile Wallet"). */
    @FXML private Label mobileWalletLabel;

    /** Panel title and "processing" status label. */
    @FXML private Label panelTitle, processingLabel;

    /** Progress ring shown during simulated processing. */
    @FXML private ProgressIndicator ring;

    /** Start and Cancel action buttons. */
    @FXML private Button startBtn, cancelBtn;

    /** Clock format used in the header. */
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");

    /** Payment service (injected; reserved for future real processing). */
    @Autowired
    private PaymentService paymentService;

    /** Spring application context for controller-factory-backed navigation. */
    private final ApplicationContext appContext;

    /** Session container for cross-screen state. */
    private final PaymentSession paymentSession;

    /**
     * Constructs the controller with required collaborators.
     *
     * @param i18n            internationalization service
     * @param appContext      Spring application context for navigation
     * @param paymentSession  session holder for current payment/order info
     */
    public MobileWalletController(I18nService i18n, ApplicationContext appContext, PaymentSession paymentSession) {
        this.i18n = i18n;
        this.appContext = appContext;
        this.paymentSession = paymentSession;
    }

    /**
     * JavaFX initialization hook.
     * <ul>
     *   <li>Starts the header clock.</li>
     *   <li>Registers nodes with {@link TextZoomService} and attaches {@link ContrastManager}.</li>
     *   <li>Applies localized UI texts.</li>
     * </ul>
     */
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

    /**
     * Applies localized strings to all visible text elements.
     */
    private void updateTexts() {
        mobileWalletLabel.setText(i18n.get("mobileWalletPayment.title"));
        panelTitle.setText(i18n.get("mobileWalletPayment.panelLine"));
        processingLabel.setText(i18n.get("mobileWalletPayment.processingText"));
        cancelBtn.setText(i18n.get("mobileWalletPayment.cancelButtonText"));
    }

    /**
     * Volume handler placeholder. Wire in kiosk audio/TTS if required.
     *
     * @param actionEvent event from a volume control
     */
    public void onVolume(ActionEvent actionEvent) {
        logger.info("Volume button pressed.");
        // hook your volume control here
    }

    /**
     * Brand click handler—clears the session and navigates to the welcome screen.
     *
     * @param event mouse event from the brand label
     */
    @FXML
    private void onBrandClick(MouseEvent event) {
        paymentSession.clear();
        goWelcomeScreen((Node) event.getSource());
    }

    /**
     * Replaces the scene root with the welcome screen.
     *
     * @param anyNodeInScene any node in the current scene (to resolve the scene)
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
     * Cancel button handler—returns to the Payment method selection screen.
     *
     * @param event click from the Cancel button
     */
    @FXML
    private void onCancel(ActionEvent event) {
        goTo("/Fxml/Payment.fxml", event);
    }

    /**
     * Helper to navigate to an FXML view by path using the Spring controller factory.
     *
     * @param fxmlPath target FXML resource path
     * @param event    originating action event (used to obtain the scene)
     */
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

    /**
     * Start button handler—shows the processing UI and simulates a 5s wallet transaction,
     * then navigates to the Payment Success screen.
     *
     * @param e click from the Start button
     */
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
