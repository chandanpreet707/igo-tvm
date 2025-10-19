package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.ContrastManager;
import concordia.soen6611.igo_tvm.Services.I18nService;
import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.Services.TextZoomService;
import concordia.soen6611.igo_tvm.models.OrderSummary;
import javafx.animation.PauseTransition;
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
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class PaymentController {

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

    public PaymentController(ApplicationContext appContext, PaymentSession paymentSession, I18nService i18n) {
        this.appContext = appContext;
        this.paymentSession = paymentSession;
        this.i18n = i18n;
    }

    @FXML
    private void initialize() {
        System.out.println("Payment selected: " + Method.CARD);
        // 1) Display the exact total from the previous page
        setTotalDueFromSession();

        // 2) Default visual state
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
//        showTapHintIfNeeded();
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

    /* ===== Total Due helper ===== */
    private void setTotalDueFromSession() {
        OrderSummary o = paymentSession != null ? paymentSession.getCurrentOrder() : null;

        double total = (o != null) ? o.getTotal() : 0.0;

        // Format like the mock: English uses $12.34 ; French uses 12,34 $
        String en = NumberFormat.getCurrencyInstance(Locale.CANADA).format(total);         // $12.34
        String fr = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH).format(total); // 12,34 $

        // Compose bilingual line
//        totalDueLabel.setText(String.format("Total Due: %s | Total à Payer: %s", en, fr));
        Locale current = i18n.getLocale();
        NumberFormat fmt = current.getLanguage().equals("fr") ?
                NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH) :
                NumberFormat.getCurrencyInstance(Locale.CANADA);
        String amount = fmt.format(total);
        totalDueLabel.setText(i18n.get("payment.totalDue", amount));



    }

    /* ====== Nav helpers ====== */
    private void goTo(String fxmlPath, ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(appContext::getBean);
            Parent view = loader.load();
            ((Node) event.getSource()).getScene().setRoot(view);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /* ====== Selection actions ====== */
    @FXML
    public void onSelectCard() {
        System.out.println("Payment selected: " + Method.CARD);
        selected = Method.CARD;
        applySelectionStyles();
//        showTapHintIfNeeded();
    }

    @FXML
    public void onSelectCash() {
        selected = Method.CASH;
        applySelectionStyles();
//        showTapHintIfNeeded();
    }

    private void applySelectionStyles() {
        // clear selection classes
        cardBtn.getStyleClass().removeAll("pm-tile--selected");
        cashBtn.getStyleClass().removeAll("pm-tile--selected");

        // apply selection class to chosen tile
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

    /* ====== Confirm flow ====== */
    @FXML
    public void onConfirm(ActionEvent event) {
        // show processing UI briefly
        showTapHintIfNeeded();
//        String next = (selected == Method.CARD) ? "/Fxml/PaymentSuccess.fxml": "/Fxml/CashSubmission.fxml";
        if(selected == Method.CARD){
            processingIndicator.setVisible(true);
            processingIndicator.setManaged(true);
            processingLabel.setVisible(true);
            processingLabel.setManaged(true);
            confirmBtn.setDisable(true);
            cardBtn.setDisable(true);
            cashBtn.setDisable(true);
            PauseTransition pause = new PauseTransition(Duration.seconds(5.5));
            // simulate processing delay
            pause.setOnFinished(e -> {
                goTo("/Fxml/PaymentSuccess.fxml", event);
            });
            pause.play();
        } else {
            goTo("/Fxml/CashSubmission.fxml", event);
        }

    }

    /* ====== Footer buttons ====== */
    public void onCancelPayment(ActionEvent event) {
//        goTo("/Fxml/BuyNewTicket.fxml", event);
        // same action for Back/Cancel
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
        // hook your volume control here
    }

    @FXML
    private void onBrandClick(MouseEvent event) {
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
