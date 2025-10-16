package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.models.OrderSummary;
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
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class PaymentController {

    enum Method { CARD, CASH }

    @FXML private Button cardBtn, cashBtn, confirmBtn;
    @FXML private ProgressIndicator processingIndicator;
    @FXML private Label processingLabel, totalDueLabel, tapInsertHint, clockLabel;

    private Timeline clock;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a");

    private final ApplicationContext appContext;
    private final PaymentSession paymentSession;
    private Method selected = Method.CARD; // default

    public PaymentController(ApplicationContext appContext, PaymentSession paymentSession) {
        this.appContext = appContext;
        this.paymentSession = paymentSession;
    }

    @FXML
    private void initialize() {
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            loader.setControllerFactory(appContext::getBean);
            Parent view = loader.load();
            ((Node) event.getSource()).getScene().setRoot(view);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /* ===== Selection Actions ===== */
    @FXML
    public void onSelectCard() {
        selected = Method.CARD;
        applySelectionStyles();
        showTapHintIfNeeded();
        System.out.println("Payment selected: " + Method.CARD);
    }

    @FXML
    public void onSelectCash() {
        selected = Method.CASH;
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
        showTapHintIfNeeded();

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
            pause.setOnFinished(e -> goTo("/Fxml/PaymentSuccess.fxml", event));
            pause.play();

        } else {
            // Go directly to cash submission flow
            goTo("/Fxml/CashSubmission.fxml", event);
        }
    }

    /* ===== Footer Buttons ===== */
    public void onCancelPayment(ActionEvent event) {
        goTo("/Fxml/BuyNewTicket.fxml", event);
    }

    public void onVolume(ActionEvent actionEvent) {
        // Hook volume control logic here (optional)
    }
}
