package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.ContrastManager;
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

    private final ApplicationContext appContext;
    private final PaymentSession paymentSession;
    private Method selected = Method.CARD; // default
    @FXML private Label brandLink,paymentLabel, clockLabel, selectPaymentMethodLabel, payWithCashLabel, payWithCardLabel;
    @FXML private javafx.scene.Parent root;

    public PaymentController(ApplicationContext appContext, PaymentSession paymentSession) {
        this.appContext = appContext;
        this.paymentSession = paymentSession;
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
            zoom.register(brandLink,paymentLabel, clockLabel, selectPaymentMethodLabel, payWithCashLabel, payWithCardLabel,
                    totalDueLabel, processingLabel, tapInsertHint, confirmBtn, backBtn);
        });
        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
        });
    }

    /* ===== Total Due helper ===== */
    private void setTotalDueFromSession() {
        OrderSummary o = paymentSession != null ? paymentSession.getCurrentOrder() : null;

        double total = (o != null) ? o.getTotal() : 0.0;

        // Format like the mock: English uses $12.34 ; French uses 12,34 $
        String en = NumberFormat.getCurrencyInstance(Locale.CANADA).format(total);         // $12.34
        String fr = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH).format(total); // 12,34 $

        // Compose bilingual line
        totalDueLabel.setText(String.format("Total Due: %s | Total à Payer: %s", en, fr));
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
        goTo("/Fxml/BuyNewTicket.fxml", event);
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
