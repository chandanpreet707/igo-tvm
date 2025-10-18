package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.Services.PaymentService;
import concordia.soen6611.igo_tvm.models.OrderSummary;
import concordia.soen6611.igo_tvm.models.Payment;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    enum Method {CARD, CASH}

    @FXML
    private Button cardBtn, cashBtn, confirmBtn;
    @FXML
    private ProgressIndicator processingIndicator;
    @FXML
    private Label processingLabel, totalDueLabel, tapInsertHint, clockLabel;

    private Timeline clock;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a");

    private final ApplicationContext appContext;
    private final PaymentSession paymentSession;
    private Method selected = Method.CARD; // default

    @Autowired
    private PaymentService paymentService;

    public PaymentController(ApplicationContext appContext, PaymentSession paymentSession) {
        this.appContext = appContext;
        this.paymentSession = paymentSession;
    }

    @FXML
    private void initialize() {
        logger.info("Initializing PaymentController");
        // Start live clock
        clock = new Timeline(
                new KeyFrame(Duration.ZERO,
                        e -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // 1) Display total amount due from session
        setTotalDueFromSession();

        // 2) Default selection visuals
        applySelectionStyles();
        showTapHintIfNeeded();

        System.out.println("Default payment selected: " + Method.CARD);
    }

    /* ===== Total Due Helper ===== */
    private void setTotalDueFromSession() {
        OrderSummary o = paymentSession != null ? paymentSession.getCurrentOrder() : null;
        OrderSummary order = paymentSession != null ? paymentSession.getCurrentOrder() : null;
        double total = (order != null) ? order.getTotal() : 0.0;

        // Format bilingually
        String en = NumberFormat.getCurrencyInstance(Locale.CANADA).format(total);
        String fr = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH).format(total);
        totalDueLabel.setText(String.format("Total Due: %s | Total à Payer: %s", en, fr));
    }

    /* ===== Navigation Helper ===== */
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

    /* ===== Selection Actions ===== */
    @FXML
    public void onSelectCard() {
        selected = Method.CARD;
        logger.info("Payment method selected: CARD");
        applySelectionStyles();
        showTapHintIfNeeded();
        System.out.println("Payment selected: " + Method.CARD);
    }

    @FXML
    public void onSelectCash() {
        selected = Method.CASH;
        logger.info("Payment method selected: CASH");
        applySelectionStyles();
        showTapHintIfNeeded();
        System.out.println("Payment selected: " + Method.CASH);
    }

    private void applySelectionStyles() {
        // Clear previous selection styles
        cardBtn.getStyleClass().removeAll("pm-tile--selected");
        cashBtn.getStyleClass().removeAll("pm-tile--selected");

        // Apply current selection style
        if (selected == Method.CARD) {
            if (!cardBtn.getStyleClass().contains("pm-tile--selected"))
                cardBtn.getStyleClass().add("pm-tile--selected");
        } else {
            if (!cashBtn.getStyleClass().contains("pm-tile--selected"))
                cashBtn.getStyleClass().add("pm-tile--selected");
        }
    }

    private void showTapHintIfNeeded() {
        boolean show = selected == Method.CARD;
        tapInsertHint.setVisible(show);
        tapInsertHint.setManaged(show);
    }

    /* ===== Confirm Flow ===== */
    @FXML
    public void onConfirm(ActionEvent event) {
        logger.info("Confirm button pressed. Selected method: {}", selected);
        showTapHintIfNeeded();
        double total = paymentSession.getCurrentOrder() != null ? paymentSession.getCurrentOrder().getTotal() : 0.0;
        String method = selected == Method.CARD ? "Card" : "Cash";
        Payment payment = new Payment(method, total);
        paymentService.startPayment(method, total);

        if (selected == Method.CARD) {
            logger.info("Processing card payment...");

            if (selected == Method.CARD) {
                processingIndicator.setVisible(true);
                processingIndicator.setManaged(true);
                processingLabel.setVisible(true);
                processingLabel.setManaged(true);
                confirmBtn.setDisable(true);
                cardBtn.setDisable(true);
                cashBtn.setDisable(true);

                // Simulate processing delay (5.5 seconds)
                PauseTransition pause = new PauseTransition(Duration.seconds(5.5));
                pause.setOnFinished(e -> {
                    paymentService.processPayment();
                    Payment result = paymentService.getCurrentPayment();
                    logger.info("Card payment status: {}", result.getStatus());
                    if ("Completed".equals(result.getStatus())) {
                        processingLabel.setText("Payment successful! | Paiement réussi!");
                    } else {
                        processingLabel.setText("Payment failed! | Paiement échoué!");
                    }
                    goTo("/Fxml/PaymentSuccess.fxml", event);
                });
                pause.setOnFinished(e -> goTo("/Fxml/PaymentSuccess.fxml", event));
                pause.play();

            } else {
                logger.info("Processing cash payment...");
                paymentService.processPayment();
                // Go directly to cash submission flow
                goTo("/Fxml/CashSubmission.fxml", event);
            }
        }
    }
    public void onCancelPayment(ActionEvent event){
        logger.info("Cancel payment pressed.");
        paymentService.cancelPayment();
        goTo("/Fxml/BuyNewTicket.fxml", event);
    }

    public void onVolume(ActionEvent event){
        logger.info("Volume button pressed.");
        // hook your volume control here
        // Hook volume control logic here (optional)
    }
}
