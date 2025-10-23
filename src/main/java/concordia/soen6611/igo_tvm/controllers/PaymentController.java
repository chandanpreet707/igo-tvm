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

/**
 * Controller for the Payment screen where the user selects a payment method
 * (Card, Cash, or Mobile Wallet) and proceeds with the transaction.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Read the total due from {@link PaymentSession} and render it in the UI.</li>
 *   <li>Allow the user to select a payment method and reflect selection styling.</li>
 *   <li>Kick off the appropriate payment flow via {@link PaymentService}.</li>
 *   <li>Handle navigation to follow-up screens (Card processing success, Cash submission, Mobile Wallet flow).</li>
 *   <li>Register accessibility helpers ({@link TextZoomService}, {@link ContrastManager}) and localize labels via {@link I18nService}.</li>
 * </ul>
 * <p>
 * Scope: Spring {@code prototype}; a fresh instance per view load.
 */
@Controller
@org.springframework.context.annotation.Scope("prototype")
public class PaymentController {

    /** Logger for payment lifecycle events and navigation. */
    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    /** Label displayed on the Mobile Wallet tile/button. */
    public Label mobileWalletBtnLabel;
    /** Mobile Wallet selection button. */
    public Button mobileWalletBtn;

    /** Supported payment methods for this screen. */
    enum Method { CARD, CASH, MOBILE_WALLET}

    /** Card, Cash selection buttons and action buttons (Confirm/Back). */
    @FXML private Button cardBtn, cashBtn, confirmBtn, backBtn;
    /** Spinner shown while card payment is processing. */
    @FXML private ProgressIndicator processingIndicator;
    /** Processing label, total due label, and hint for tap/insert actions. */
    @FXML private Label processingLabel, totalDueLabel, tapInsertHint;
    /** Screen title label. */
    @FXML private Label paymentLabel;
    /** "Select payment method" label. */
    @FXML private Label selectMethodLabel;
    /** Text labels inside the Cash and Card tiles. */
    @FXML private Label cashBtnLabel;
    @FXML private Label cardBtnLabel;
//    @FXML private Tooltip backBtnTooltip;

    /** Spring application context for controller-factory-backed navigation. */
    private final ApplicationContext appContext;
    /** Session container holding order information and origin screen. */
    private final PaymentSession paymentSession;
    /** Currently selected payment method; defaults to CARD. */
    private Method selected = Method.CARD; // default
    /** Brand link, clock label, and accessibility labels. */
    @FXML private Label brandLink, clockLabel, payWithCashLabel, payWithCardLabel;
    /** Root node for attaching contrast handling. */
    @FXML private javafx.scene.Parent root;
    /** i18n service for localized strings and current locale. */
    private final I18nService i18n;
    /** Ticking timeline used to update the header clock once per second. */
    private Timeline clock;
    /** "Help" label. */
    @FXML private Label helpLabel;
    /** Clock format used for the header clock. */
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");
    /** Payment service orchestrating the current payment. */
    @Autowired
    private PaymentService paymentService;

    /**
     * Constructs the controller with required collaborators.
     *
     * @param appContext     Spring application context for navigation
     * @param paymentSession session state with current order and origin
     * @param i18n           internationalization service
     */
    public PaymentController(ApplicationContext appContext, PaymentSession paymentSession, I18nService i18n) {
        this.appContext = appContext;
        this.paymentSession = paymentSession;
        this.i18n = i18n;
    }

    /**
     * JavaFX initialization hook.
     * <ul>
     *   <li>Starts a live clock in the header.</li>
     *   <li>Reads and displays the total due from session.</li>
     *   <li>Applies selection styles to the default method.</li>
     *   <li>Registers nodes with {@link TextZoomService} and attaches {@link ContrastManager}.</li>
     *   <li>Applies localized strings to UI elements.</li>
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

        setTotalDueFromSession();
        applySelectionStyles();

        Platform.runLater(() -> {
            var zoom = TextZoomService.get();
            zoom.register(brandLink,paymentLabel, clockLabel, selectMethodLabel, cashBtnLabel, mobileWalletBtnLabel, cardBtnLabel,
                    totalDueLabel, processingLabel, tapInsertHint, confirmBtn, backBtn);
        });
        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
        });
        updateTexts();
    }

    /**
     * Localizes all visible labels and button texts for the current locale.
     */
    private void updateTexts() {
        paymentLabel.setText(i18n.get("payment.title"));
        selectMethodLabel.setText(i18n.get("payment.selectMethod"));
        cashBtnLabel.setText(i18n.get("payment.payWithCash"));
        mobileWalletBtnLabel.setText(i18n.get("payment.payWithMobileWallet"));
        cardBtnLabel.setText(i18n.get("payment.creditDebit"));
        tapInsertHint.setText(i18n.get("payment.tapInsert"));
        processingLabel.setText(i18n.get("payment.processing"));
        backBtn.setText(i18n.get("payment.cancel"));
        confirmBtn.setText(i18n.get("payment.confirm"));
    }

    /**
     * Reads the current {@link OrderSummary} from the session and renders the bilingual total due.
     * Falls back to {@code 0.0} if no order is present.
     */
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

    /**
     * Navigates to another FXML view using the Spring controller factory.
     *
     * @param fxmlPath target FXML resource (e.g., {@code "/Fxml/PaymentSuccess.fxml"})
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
     * Selects the Card payment method and applies selection styling.
     */
    @FXML
    public void onSelectCard() {
        selected = Method.CARD;
        logger.info("Payment method selected: CARD");
        applySelectionStyles();
    }

    /**
     * Selects the Cash payment method and applies selection styling.
     */
    @FXML
    public void onSelectCash() {
        selected = Method.CASH;
        logger.info("Payment method selected: CASH");
        applySelectionStyles();
    }

    /**
     * Selects the Mobile Wallet payment method and applies selection styling.
     */
    @FXML
    public void onSelectMobileWallet() {
        selected = Method.MOBILE_WALLET;
        logger.info("Payment method selected: MOBILE WALLET");
        applySelectionStyles();
    }

    /**
     * Applies or clears the {@code pm-tile--selected} CSS class on the method tiles
     * to reflect the active selection.
     */
    private void applySelectionStyles() {
        cardBtn.getStyleClass().removeAll("pm-tile--selected");
        cashBtn.getStyleClass().removeAll("pm-tile--selected");
        mobileWalletBtn.getStyleClass().removeAll("pm-tile--selected");
        if (selected == Method.CARD) {
            if (!cardBtn.getStyleClass().contains("pm-tile--selected")) {
                cardBtn.getStyleClass().add("pm-tile--selected");
            }
        } else if (selected == Method.MOBILE_WALLET) {
            if (!mobileWalletBtn.getStyleClass().contains("pm-tile--selected")) {
                mobileWalletBtn.getStyleClass().add("pm-tile--selected");
            }
        } else {
            if (!cashBtn.getStyleClass().contains("pm-tile--selected")) {
                cashBtn.getStyleClass().add("pm-tile--selected");
            }
        }
    }

    /**
     * Shows or hides the "tap/insert" hint depending on the selected method (shown for Card only).
     */
    private void showTapHintIfNeeded() {
        boolean show = selected == Method.CARD;
        tapInsertHint.setVisible(show);
        tapInsertHint.setManaged(show);
    }

    /**
     * Confirm button handler: starts the appropriate payment flow for the selected method.
     * <ul>
     *   <li><b>Card</b>: shows a processing spinner/label, simulates ~5.5s processing, then navigates to success.</li>
     *   <li><b>Mobile Wallet</b>: navigates to the Mobile Wallet screen.</li>
     *   <li><b>Cash</b>: navigates to the Cash Submission screen.</li>
     * </ul>
     *
     * @param event click event from the Confirm button
     */
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
            mobileWalletBtn.setDisable(true);

            paymentService.startPayment("Card", total);
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
        } else if (selected == Method.MOBILE_WALLET) {
            // Route to Mobile Wallet flow/screen
            paymentService.startPayment("MobileWallet", total);
            goTo("/Fxml/MobileWallet.fxml", event);

        } else {
            logger.info("Processing cash payment...");
            paymentService.processPayment();
            goTo("/Fxml/CashSubmission.fxml", event);
        }
    }

    /**
     * Cancels the current payment and navigates back to the originating flow.
     *
     * @param event click event from the Cancel button
     */
    public void onCancelPayment(ActionEvent event) {
        logger.info("Cancel payment pressed.");
        paymentService.cancelPayment();
        goBack((Node) event.getSource());
    }

    /**
     * Navigates back to the previous screen based on {@link PaymentSession.Origin}:
     * <ul>
     *   <li>{@code RELOAD_CARD} → {@code /Fxml/CardReloadAmount.fxml}</li>
     *   <li>{@code BUY_TICKET} (default) → {@code /Fxml/BuyNewTicket.fxml}</li>
     * </ul>
     * Falls back to Home if navigation fails.
     *
     * @param nodeInScene any node in the current scene (used to obtain the scene)
     */
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

    /**
     * Volume button handler placeholder. Wire kiosk audio/TTS if required.
     *
     * @param actionEvent event from a volume control
     */
    public void onVolume(ActionEvent actionEvent) {
        logger.info("Volume button pressed.");
        // hook your volume control here
    }

    /**
     * Brand click handler—clears session and navigates to the welcome screen.
     *
     * @param event mouse click event from the brand label
     */
    @FXML
    private void onBrandClick(MouseEvent event) {
        paymentSession.clear();
        goWelcomeScreen((Node) event.getSource());
    }

    /**
     * Replaces the current scene root with the welcome screen.
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
}
