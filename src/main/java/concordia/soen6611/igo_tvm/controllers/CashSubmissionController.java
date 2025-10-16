package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.models.OrderSummary;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
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
public class CashSubmissionController {

    /* ===== FXML Bindings ===== */
    @FXML private Label totalDueLabel;
    @FXML private Label clockLabel;
    @FXML private Label instructionLabel;
    @FXML private Label insertedValue;
    @FXML private Label remainingValue;
    @FXML private ProgressIndicator processingIndicator;
    @FXML private ImageView cashIllustration;

    /* ===== Dependencies ===== */
    private final ApplicationContext appContext;
    private final PaymentSession paymentSession;

    /* ===== Internal State ===== */
    private Timeline clock;
    private Timeline ticker;
    private double total;     // Total amount due
    private double inserted;  // Simulated cash inserted

    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a");

    /* ===== Constructor ===== */
    public CashSubmissionController(ApplicationContext appContext, PaymentSession paymentSession) {
        this.appContext = appContext;
        this.paymentSession = paymentSession;
    }

    /* ===== Initialization ===== */
    @FXML
    private void initialize() {
        initClock();
        initPaymentDisplay();
        startCashTicker();
    }

    /** Initializes and starts the live clock display. */
    private void initClock() {
        clock = new Timeline(
                new KeyFrame(Duration.ZERO,
                        e -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();
    }

    /** Initializes total amount and UI labels. */
    private void initPaymentDisplay() {
        // Retrieve total from session or fallback to 0.0
        OrderSummary order = (paymentSession != null) ? paymentSession.getCurrentOrder() : null;
        total = (order != null) ? order.getTotal() : 0.0;

        // Display bilingual total
        NumberFormat en = NumberFormat.getCurrencyInstance(Locale.CANADA);
        NumberFormat fr = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH);
        totalDueLabel.setText(String.format("Total Due: %s | Total à Payer: %s",
                en.format(total), fr.format(total)));

        // Initialize amounts
        inserted = 0.0;
        updateAmounts();
    }

    /** Starts the simulation of cash being inserted gradually. */
    private void startCashTicker() {
        ticker = new Timeline(new KeyFrame(Duration.millis(1000), e -> stepInsert()));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();
    }

    /** Simulates a single cash insertion step. */
    private void stepInsert() {
        double remaining = Math.max(0.0, total - inserted);
        double step = remaining >= 5.0 ? 5.0 : remaining >= 2.0 ? 2.0 : 1.0;

        // If less than 1 remains, finish exactly
        if (remaining > 0 && remaining < 1.0) step = remaining;

        inserted = Math.min(total, inserted + step);
        updateAmounts();

        // Once fully paid
        if (inserted >= total - 1e-9) {
            ticker.stop();
            processingIndicator.setVisible(false);
            processingIndicator.setManaged(false);

            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Cash Payment");
            ok.setHeaderText(null);
            ok.setContentText("Payment received. Printing your ticket…");
            ok.show();

            PauseTransition wait = new PauseTransition(Duration.seconds(3));
            wait.setOnFinished(ev -> {
                ok.close();
                goWelcomePage();
            });
            wait.play();
        }
    }

    /** Updates UI labels for inserted and remaining amounts. */
    private void updateAmounts() {
        NumberFormat en = NumberFormat.getCurrencyInstance(Locale.CANADA);
        insertedValue.setText(en.format(inserted));
        double remaining = Math.max(0.0, total - inserted);
        remainingValue.setText(en.format(remaining));
    }

    /** Navigates back to the welcome screen. */
    private void goWelcomePage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/welcome-screen.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent view = loader.load();
            totalDueLabel.getScene().setRoot(view);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /* ===== Footer Handlers ===== */
    public void onVolume(ActionEvent actionEvent) { /* Optional */ }

    public void onCancelCashPayment(ActionEvent actionEvent) {
        if (ticker != null) ticker.stop();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/Home.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent view = loader.load();
            ((Node) actionEvent.getSource()).getScene().setRoot(view);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
