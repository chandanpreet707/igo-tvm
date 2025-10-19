package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.ContrastManager;
import concordia.soen6611.igo_tvm.Services.I18nService;
import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.Services.PaymentService;
import concordia.soen6611.igo_tvm.Services.TextZoomService;
import concordia.soen6611.igo_tvm.models.OrderSummary;
import concordia.soen6611.igo_tvm.models.Payment;
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

    enum Method { CARD, CASH }

    @FXML private Button cardBtn, cashBtn, confirmBtn, backBtn;
    @FXML private ProgressIndicator processingIndicator;
    @FXML private Label processingLabel, totalDueLabel, tapInsertHint;
    @FXML private Label paymentLabel;
    @FXML private Label selectMethodLabel;
    @FXML private Label cashBtnLabel;
    @FXML private Label cardBtnLabel;
//    @FXML private Tooltip backBtnTooltip;

    private final ApplicationContext appContext;
    private final PaymentSession paymentSession;
    private Method selected = Method.CARD; // default
    @FXML private Label brandLink, clockLabel, payWithCashLabel, payWithCardLabel;
    @FXML private javafx.scene.Parent root;
    private final I18nService i18n;
    private Timeline clock;
    @FXML private Label helpLabel;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");
    @Autowired
    private PaymentService paymentService;

    public PaymentController(ApplicationContext appContext, PaymentSession paymentSession, I18nService i18n) {
        this.appContext = appContext;
        this.paymentSession = paymentSession;
        this.i18n = i18n;
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

        setTotalDueFromSession();
        applySelectionStyles();

        Platform.runLater(() -> {
            var zoom = TextZoomService.get();
            zoom.register(brandLink,paymentLabel, clockLabel, selectMethodLabel, cashBtnLabel, cardBtnLabel,
                    totalDueLabel, processingLabel, tapInsertHint, confirmBtn, backBtn);
        });
        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
        });
        updateTexts();
    }

    private void updateTexts() {
        paymentLabel.setText(i18n.get("payment.title"));
        selectMethodLabel.setText(i18n.get("payment.selectMethod"));
        cashBtnLabel.setText(i18n.get("payment.payWithCash"));
        cardBtnLabel.setText(i18n.get("payment.creditDebit"));
        tapInsertHint.setText(i18n.get("payment.tapInsert"));
        processingLabel.setText(i18n.get("payment.processing"));
        backBtn.setText(i18n.get("payment.cancel"));
        confirmBtn.setText(i18n.get("payment.confirm"));
    }

    private void setTotalDueFromSession() {
        OrderSummary o = paymentSession != null ? paymentSession.getCurrentOrder() : null;
        double total = (o != null) ? o.getTotal() : 0.0;
        logger.debug("Total due from session: {}", total);
        String en = NumberFormat.getCurrencyInstance(Locale.CANADA).format(total);
        String fr = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH).format(total);
        Locale current = i18n.getLocale();
        NumberFormat fmt = current.getLanguage().equals("fr") ?
                NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH) :
                NumberFormat.getCurrencyInstance(Locale.CANADA);
        String amount = fmt.format(total);
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
        goBack((Node) event.getSource());
    }


    private void goBack(Node nodeInScene) {
        String fxml;
        switch (paymentSession.getOrigin()) {
            case RELOAD_CARD:
                fxml = "/Fxml/CardReloadAmount.fxml"; // or CardReload.fxml if that’s where you want to return
                break;
            case BUY_TICKET:
            default:
                fxml = "/Fxml/BuyNewTicket.fxml";
                break;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
            loader.setControllerFactory(appContext::getBean);
            Parent view = loader.load();
            nodeInScene.getScene().setRoot(view);
        } catch (IOException ex) {
            ex.printStackTrace();
            // Hard fallback to Home if something goes wrong
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/Home.fxml"));
                loader.setControllerFactory(appContext::getBean);
                Parent home = loader.load();
                nodeInScene.getScene().setRoot(home);
            } catch (IOException ignored) {}
        }
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
}
