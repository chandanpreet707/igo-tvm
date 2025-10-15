package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.Services.PaymentService;
import concordia.soen6611.igo_tvm.models.OrderSummary;
import concordia.soen6611.igo_tvm.models.Payment;
import javafx.animation.PauseTransition;
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
import java.util.Locale;

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    enum Method { CARD, CASH }

    @FXML private Button cardBtn, cashBtn, confirmBtn;
    @FXML private ProgressIndicator processingIndicator;
    @FXML private Label processingLabel, totalDueLabel, tapInsertHint;

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
        setTotalDueFromSession();
        applySelectionStyles();
    }

    private void setTotalDueFromSession() {
        OrderSummary o = paymentSession != null ? paymentSession.getCurrentOrder() : null;
        double total = (o != null) ? o.getTotal() : 0.0;
        logger.debug("Total due from session: {}", total);
        String en = NumberFormat.getCurrencyInstance(Locale.CANADA).format(total);
        String fr = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH).format(total);
        totalDueLabel.setText(String.format("Total Due: %s | Total à Payer: %s", en, fr));
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
    public void onSelectCard() {
        selected = Method.CARD;
        logger.info("Payment method selected: CARD");
        applySelectionStyles();
    }

    @FXML
    public void onSelectCash() {
        selected = Method.CASH;
        logger.info("Payment method selected: CASH");
        applySelectionStyles();
    }

    private void applySelectionStyles() {
        cardBtn.getStyleClass().removeAll("pm-tile--selected");
        cashBtn.getStyleClass().removeAll("pm-tile--selected");
        if (selected == Method.CARD) {
            if (!cardBtn.getStyleClass().contains("pm-tile--selected")) {
                cardBtn.getStyleClass().add("pm-tile--selected");
            }
        } else {
            if (!cashBtn.getStyleClass().contains("pm-tile--selected")) {
                cashBtn.getStyleClass().add("pm-tile--selected");
            }
        }
    }

    private void showTapHintIfNeeded() {
        boolean show = selected == Method.CARD;
        tapInsertHint.setVisible(show);
        tapInsertHint.setManaged(show);
    }

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
            processingIndicator.setVisible(true);
            processingIndicator.setManaged(true);
            processingLabel.setVisible(true);
            processingLabel.setManaged(true);
            confirmBtn.setDisable(true);
            cardBtn.setDisable(true);
            cashBtn.setDisable(true);
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
            pause.play();
        } else {
            logger.info("Processing cash payment...");
            paymentService.processPayment();
            goTo("/Fxml/CashSubmission.fxml", event);
        }
    }

    public void onCancelPayment(ActionEvent event) {
        logger.info("Cancel payment pressed.");
        paymentService.cancelPayment();
        goTo("/Fxml/BuyNewTicket.fxml", event);
    }

    public void onVolume(ActionEvent actionEvent) {
        logger.info("Volume button pressed.");
        // hook your volume control here
    }
}
