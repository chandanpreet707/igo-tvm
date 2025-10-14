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
import java.util.Locale;

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class CashSubmissionController {
    @FXML private Label totalDueLabel;
    @FXML private Label instructionLabel;
    @FXML private Label insertedValue;
    @FXML
    private Label remainingValue;
    @FXML private ProgressIndicator processingIndicator;
    @FXML private ImageView cashIllustration;
    private final ApplicationContext appContext;
    private final PaymentSession paymentSession;

    private double total;       // amount due
    private double inserted;    // simulated inserted cash
    private Timeline ticker;    // counts cash up

    public CashSubmissionController(ApplicationContext appContext,
                                    PaymentSession paymentSession) {
        this.appContext = appContext;
        this.paymentSession = paymentSession;
    }

    @FXML
    private void initialize() {
        // 1) Read total from session (fallback 0.0)
        OrderSummary o = paymentSession != null ? paymentSession.getCurrentOrder() : null;
        total = (o != null) ? o.getTotal() : 0.0;

        // 2) Show bilingual total
        NumberFormat en = NumberFormat.getCurrencyInstance(Locale.CANADA);
        NumberFormat fr = NumberFormat.getCurrencyInstance(Locale.CANADA_FRENCH);
        totalDueLabel.setText(String.format("Total Due: %s | Total à Payer: %s",
                en.format(total), fr.format(total)));

        // 3) Initialize amounts
        inserted = 0.0;
        updateAmounts();

        // 4) Simulate bills/coins being counted every ~700ms
        ticker = new Timeline(new KeyFrame(Duration.millis(1000), e -> stepInsert()));
        ticker.setCycleCount(Animation.INDEFINITE);
        ticker.play();
    }

    /** Simulate a cash insert step. */
    private void stepInsert() {
        // simple step ladder: 5, 2, 1 to reach total cleanly
        double remaining = Math.max(0.0, total - inserted);
        double step = remaining >= 5.0 ? 5.0 : remaining >= 2.0 ? 2.0 : 1.0;

        // If we’re within < 1, use exact remainder to finish
        if (remaining > 0 && remaining < 1.0) step = remaining;

        inserted = Math.min(total, inserted + step);
        updateAmounts();

        if (inserted >= total - 1e-9) {
            ticker.stop();
            // brief success modal, then back to Home
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

    private void updateAmounts() {
        NumberFormat en = NumberFormat.getCurrencyInstance(Locale.CANADA);
        insertedValue.setText(en.format(inserted));
        double rem = Math.max(0.0, total - inserted);
        remainingValue.setText(en.format(rem));
    }

    private void goWelcomePage() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/welcome-screen.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent view = loader.load();
            // any visible node works; use any control you have on this scene
            totalDueLabel.getScene().setRoot(view);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /* ===== Footer handlers ===== */
    public void onVolume(ActionEvent actionEvent) { /* optional */ }

    public void onCancelCashPayment(ActionEvent actionEvent) {
        // stop ticker if user cancels
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
