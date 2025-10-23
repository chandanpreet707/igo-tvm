package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.I18nService;
import concordia.soen6611.igo_tvm.Services.ContrastManager;
import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.Services.TextZoomService;
import concordia.soen6611.igo_tvm.models.OrderSummary;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Controller for the Cash Submission screen.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Read order total from {@link PaymentSession} and present bilingual total due.</li>
 *   <li>Simulate cash insertion over time and update inserted/remaining amounts.</li>
 *   <li>Display localized UI text via {@link I18nService} and react to locale changes.</li>
 *   <li>Hook up accessibility helpers: {@link TextZoomService} and {@link ContrastManager}.</li>
 *   <li>Navigate to the welcome screen on success/brand click or back to Home on cancel.</li>
 * </ul>
 * Behavior:
 * <ul>
 *   <li>Cash counting is simulated using a {@link Timeline} ticking every second.</li>
 *   <li>When inserted amount reaches total, a success dialog is shown and the app returns to welcome.</li>
 * </ul>
 */
@Controller
@org.springframework.context.annotation.Scope("prototype")
public class CashSubmissionController {

    /** i18n service providing localized strings and current locale. */
    private final I18nService i18n;

    /** Brand link and clock labels in the header. */
    @FXML private Label brandLink, clockLabel;

    /** Bilingual "Total Due" label. */
    @FXML private Label totalDueLabel;

    /** Instruction text for inserting cash. */
    @FXML private Label instructionLabel;

    /** Current inserted amount (localized currency). */
    @FXML private Label insertedValue;

    /** Remaining amount to be paid (localized currency). */
    @FXML
    private Label remainingValue;

    /** Spinner shown while processing/printing after payment completes. */
    @FXML private ProgressIndicator processingIndicator;

    /** Illustration for cash insertion. */
    @FXML private ImageView cashIllustration;

    /** Spring application context for controller-factory-backed navigation. */
    private final ApplicationContext appContext;

    /** Session container holding the current {@link OrderSummary}. */
    private final PaymentSession paymentSession;

    /** Screen title label ("Cash Payment"). */
    @FXML private Label cashPaymentLabel;

    /** "Amount Inserted" label. */
    @FXML private Label amountInsertedLabel;

    /** "Remaining" label. */
    @FXML private Label remainingLabel;

    /** Back/Cancel button. */
    @FXML private Button backBtn;

    /** Amount due for this order (read from session). */
    private double total;       // amount due

    /** Simulated amount of cash inserted so far. */
    private double inserted;    // simulated inserted cash

    /** Timeline that simulates cash being counted in steps. */
    private Timeline ticker;    // counts cash up

    /** Root node of this scene, used for attaching contrast handling. */
    @FXML private javafx.scene.Parent root;

    /**
     * Constructs the controller with required collaborators.
     *
     * @param appContext      Spring application context for navigation
     * @param paymentSession  session storing current order and app state
     * @param i18n            internationalization service
     */
    public CashSubmissionController(ApplicationContext appContext,
                                    PaymentSession paymentSession, I18nService i18n) {
        this.appContext = appContext;
        this.paymentSession = paymentSession;
        this.i18n = i18n;
    }

    /**
     * JavaFX lifecycle hook. Initializes totals, starts the cash counting simulation,
     * registers accessibility helpers, and wires i18n updates.
     */
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

        Platform.runLater(() -> {
            // Register text nodes for zooming
            TextZoomService.get().register(brandLink, cashPaymentLabel, clockLabel, totalDueLabel, instructionLabel, insertedValue, remainingValue,
                    amountInsertedLabel, remainingLabel, backBtn);
        });

        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
        });

        i18n.localeProperty().addListener((obs, oldL, newL) -> {
            updateTexts();
            updateAmounts();
        });

        updateTexts();
    }

    /**
     * Applies localized strings to all visible text elements on the screen.
     * Re-run on locale changes to refresh labels and formatted values.
     */
    private void updateTexts() {
        cashPaymentLabel.setText(i18n.get("cashPayment.title"));
        java.util.Locale locale = i18n.getLocale();
        java.text.NumberFormat fmt = locale.getLanguage().equals("fr") ?
                java.text.NumberFormat.getCurrencyInstance(java.util.Locale.CANADA_FRENCH) :
                java.text.NumberFormat.getCurrencyInstance(java.util.Locale.CANADA);
        String totalText = i18n.get("cashPayment.totalDue", fmt.format(total));
        totalDueLabel.setText(totalText);
        instructionLabel.setText(i18n.get("cashPayment.instruction"));
        amountInsertedLabel.setText(i18n.get("cashPayment.amountInserted"));
        remainingLabel.setText(i18n.get("cashPayment.remaining"));
        backBtn.setText(i18n.get("cashPayment.cancel"));
    }


    /** Simulate a cash insert step. */
    /** Simulate a cash insert step with i18n messages. */
    /**
     * Simulates a single cash insertion step and updates the UI.
     * <p>
     * Uses a simple step ladder of 5, 2, and 1 to reach the total with minimal change.
     * If the remaining amount is &lt; 1, the exact remainder is inserted to finish cleanly.
     * When the inserted amount meets or exceeds the total, stops the ticker, hides the spinner,
     * shows a localized success dialog, clears the session, and navigates to the welcome screen.
     */
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

            // Hide spinner
            processingIndicator.setVisible(false);
            processingIndicator.setManaged(false);

            // i18n success modal
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle(i18n.get("cashPayment.modal.title"));       // e.g., "Cash Payment" / "Paiement en espèces"
            ok.setHeaderText(null);
            ok.setContentText(i18n.get("cashPayment.modal.received"));// e.g., "Payment received. Printing your ticket…"
            ok.show();

            PauseTransition wait = new PauseTransition(Duration.seconds(3));
            wait.setOnFinished(ev -> {
                ok.close();
                paymentSession.clear();
                goWelcomePage();
            });
            wait.play();
        }
    }

    /**
     * Updates the "inserted" and "remaining" amounts using the current locale's currency format.
     */
    private void updateAmounts() {
        java.util.Locale loc = i18n.getLocale();
        java.text.NumberFormat money =
                loc.getLanguage().equals("fr")
                        ? java.text.NumberFormat.getCurrencyInstance(java.util.Locale.CANADA_FRENCH)
                        : java.text.NumberFormat.getCurrencyInstance(java.util.Locale.CANADA);

        insertedValue.setText(money.format(inserted));
        double rem = Math.max(0.0, total - inserted);
        remainingValue.setText(money.format(rem));
    }

    /**
     * Navigates back to the welcome screen by replacing the current scene root.
     * Called when payment succeeds.
     */
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

    /**
     * Optional volume/TTS handler placeholder for kiosk accessibility.
     *
     * @param actionEvent event from a volume control UI element
     */
    public void onVolume(ActionEvent actionEvent) { /* optional */ }

    /**
     * Cancels the cash payment flow and navigates back to the Home screen.
     * Also stops the cash counting ticker if it is running.
     *
     * @param actionEvent click event from the Cancel/Back button
     */
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

    /**
     * Brand click handler—clears session and navigates to the welcome screen.
     *
     * @param event mouse click event from the brand link
     */
    @FXML
    private void onBrandClick(MouseEvent event) {
        paymentSession.clear();
        goWelcomeScreen((Node) event.getSource());
    }

    /**
     * Replaces the current scene root with the welcome screen.
     *
     * @param anyNodeInScene any node in the current scene (to obtain the scene)
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
