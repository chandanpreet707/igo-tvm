package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.*;
import concordia.soen6611.igo_tvm.models.OrderSummary;
import concordia.soen6611.igo_tvm.models.PassType;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Controller for the "Reload Card → Choose Amount" screen.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Localize and initialize UI controls and accessibility helpers.</li>
 *   <li>Allow the user to pick a {@link PassType} and (if applicable) a quantity.</li>
 *   <li>Estimate unit price, subtotal, tax, and total using {@link FareRateService}.</li>
 *   <li>Persist the current selection into {@link PaymentSession} and navigate to payment.</li>
 * </ul>
 *
 * Behavior notes:
 * <ul>
 *   <li>Quantity is enabled only for {@link PassType#SINGLE}; for all other pass types it is forced to 1.</li>
 *   <li>Composite tax is retrieved from {@link FareRateService#getTax()}.</li>
 *   <li>A live clock updates every second in the header.</li>
 * </ul>
 *
 * Scope: Spring {@code prototype}; each navigation instantiates a fresh controller.
 */
@Controller
@org.springframework.context.annotation.Scope("prototype")
public class CardReloadAmountController implements Initializable {

    /** Spring application context used as controller factory for navigation. */
    private final ApplicationContext appContext;

    /** Session object for persisting origin and current order across screens. */
    private final PaymentSession paymentSession;

    // ===== Header =====

    /** Clickable brand label; returns to welcome screen. */
    @FXML public Label brandLink;

    /** Clock label in the header; updated every second. */
    @FXML public Label clockLabel;

    // ===== Left (card) =====

    /** "Your card" heading label. */
    @FXML public Label youCardLabel;

    /** OPUS (or transit) card image preview. */
    @FXML public ImageView opusCardImage;

    /** Rider type tag rendered near the card ("Adult", "Student", etc.). */
    @FXML public Label riderTypeTag;

    // ===== Right (options) =====

    /** "Reload options" or similar heading label. */
    @FXML public Label reloadOptionLabel;

    /** Label for pass type drop-down. */
    @FXML public Label selectTypeLabel;

    /** Pass type selector (SINGLE, WEEKLY, MONTHLY, DAY). */
    @FXML public ComboBox<PassType> passTypeBox;

    /** Label for quantity selector. */
    @FXML public Label qtyLabel;

    /** Quantity selector for number of items (only enabled for SINGLE). */
    @FXML public ComboBox<Integer> qtyBox;

    // ===== Price breakdown =====

    /** Estimated unit price value (localized as CAD). */
    @FXML public Label estUnitValue;

    /** Estimated subtotal value (unit × qty). */
    @FXML public Label estSubtotalValue;

    /** "Tax" line label. */
    @FXML public Label taxLineLabel;

    /** Estimated tax value. */
    @FXML public Label taxValue;

    /** "Estimated total" label. */
    @FXML public Label estimatedTotalLabel;

    /** Estimated total value (subtotal + tax). */
    @FXML public Label estTotalValue;

    // ===== Footer / general =====

    /** Proceed-to-payment button. */
    @FXML public Button proceedBtn;

    /** Root container; used by accessibility helpers for contrast. */
    @FXML public BorderPane root;

    /** Screen title ("Reload your card"). */
    @FXML public Label reloadCardLabel;

    /** "Help" label or affordance. */
    @FXML public Label helpLabel;

    /** Unit price label text (localized). */
    public Label unitPriceLabel;

    /** Subtotal label text (localized). */
    public Label subTotalLabel;

    /** Ticking timeline for the header clock. */
    private Timeline clock;

    /** CAD currency formatter used for all displayed monetary amounts. */
    private final NumberFormat CAD = NumberFormat.getCurrencyInstance(Locale.CANADA);

    /** Clock format shown in the header. */
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");

    /** i18n service providing localized strings and locale change notifications. */
    private final I18nService i18n;

    /** Fare service returning base rates and tax percentage. */
    @Autowired
    private FareRateService fareRateService;

    /**
     * Constructs the controller with required Spring-managed collaborators.
     *
     * @param appContext     application context used for controller factory during navigation
     * @param paymentSession session model to persist current order and origin
     * @param i18n           localization service for UI text and locale changes
     */
    public CardReloadAmountController(ApplicationContext appContext, PaymentSession paymentSession, I18nService i18n) {
        this.appContext = appContext;
        this.paymentSession = paymentSession;
        this.i18n = i18n;
    }

    /**
     * JavaFX lifecycle hook for {@link Initializable}.
     * <p>
     * Initializes:
     * <ul>
     *   <li>Rider tag (defaults to "Adult" if none supplied).</li>
     *   <li>Header clock and its periodic updates.</li>
     *   <li>Pass type and quantity combo boxes (with defaults).</li>
     *   <li>Listeners to keep estimates in sync with selections.</li>
     *   <li>Text zoom and contrast accessibility helpers.</li>
     *   <li>Localization for visible text and combo box cells; re-applies on locale change.</li>
     * </ul>
     */
    @Override
    @FXML
    public void initialize(URL location, ResourceBundle resources) {
        riderTypeTag.setText("Adult"); // "Adult", "Student", etc.

        // Live clock
        clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> clockLabel.setText(LocalDateTime.now().format(CCLOCK_FMT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // Populate enum items once
        if (passTypeBox.getItems().isEmpty()) {
            passTypeBox.getItems().addAll(PassType.SINGLE, PassType.WEEKLY, PassType.MONTHLY, PassType.DAY);
        }
        passTypeBox.getSelectionModel().selectFirst();

        if (qtyBox.getItems().isEmpty()) {
            for (int i = 1; i <= 10; i++) qtyBox.getItems().add(i);
        }

        qtyBox.getSelectionModel().select(Integer.valueOf(1));
        passTypeBox.getSelectionModel().selectFirst();

        passTypeBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            updateQtyAvailability();
            updateEstimate();
        });
        qtyBox.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> updateEstimate());

        updateEstimate();

        Platform.runLater(() -> {
            var zoom = TextZoomService.get();
            zoom.register((Node) brandLink, reloadCardLabel, clockLabel, youCardLabel, opusCardImage, passTypeBox,
                    qtyLabel, qtyBox, estimatedTotalLabel, estTotalValue, proceedBtn, helpLabel);
        });

        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
        });

        // Localize UI
        localizePassTypeCombo();
        updateTexts();

        // React to locale changes
        i18n.localeProperty().addListener((obs, oldL, newL) -> {
            updateTexts();
            localizePassTypeCombo();
        });
    }

    /**
     * Applies localized strings to static labels and buttons.
     * <p>
     * Call again if the locale changes at runtime.
     */
    private void updateTexts() {
        reloadCardLabel.setText(i18n.get("cardReloadAmount.title"));
        youCardLabel.setText(i18n.get("cardReloadAmount.youCard"));
//        opusCardLabel.setText(i18n.get("cardReloadAmount.opusCard"));
        reloadOptionLabel.setText(i18n.get("cardReloadAmount.reloadOption"));
        selectTypeLabel.setText(i18n.get("cardReloadAmount.selectType"));
        qtyLabel.setText(i18n.get("cardReloadAmount.qty"));
        estimatedTotalLabel.setText(i18n.get("cardReloadAmount.estimatedTotal"));
        proceedBtn.setText(i18n.get("cardReloadAmount.proceed"));
        unitPriceLabel.setText(i18n.get("cardReloadAmount.unitPrice"));
        subTotalLabel.setText(i18n.get("cardReloadAmount.subTotal"));
        taxLineLabel.setText(i18n.get("cardReloadAmount.taxLabel"));
    }

    /**
     * Localizes {@link #passTypeBox} prompt and items, both for the popup rows and the visible button cell.
     */
    private void localizePassTypeCombo() {
        // Localized prompt and field label
        selectTypeLabel.setText(i18n.get("cardReloadAmount.selectType"));
        passTypeBox.setPromptText(i18n.get("cardReloadAmount.selectPassPrompt", new Object[]{}));

        // Each row in popup
        passTypeBox.setCellFactory(cb -> new ListCell<>() {
            @Override protected void updateItem(PassType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : i18n.get(item.key()));
            }
        });

        // The visible "button" part
        ListCell<PassType> buttonCell = new ListCell<>() {
            @Override protected void updateItem(PassType item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : i18n.get(item.key()));
            }
        };
        passTypeBox.setButtonCell(buttonCell);
    }

    /**
     * Enables or disables the quantity selector based on the chosen pass type.
     * <p>
     * Quantity is enabled only for {@link PassType#SINGLE}; otherwise, the value is forced to 1.
     */
    private void updateQtyAvailability() {
        boolean enableQty = selectedPassType() == PassType.SINGLE;
        qtyBox.setDisable(!enableQty);
        qtyLabel.setDisable(!enableQty);
        if (!enableQty) {
            qtyBox.getSelectionModel().select(Integer.valueOf(1));
        }
    }

    // ===== Helpers (selection & pricing) =====

    /**
     * Resolves the current rider label (defaults to "Adult" if missing).
     *
     * @return rider string used by the fare service
     */
    private String selectedRider() {
        String tag = riderTypeTag.getText();
        return (tag == null || tag.isBlank()) ? "Adult" : tag.trim();
    }

    /**
     * Gets the currently selected pass type; defaults to {@link PassType#SINGLE}.
     *
     * @return currently selected {@link PassType}
     */
    private PassType selectedPassType() {
        PassType pt = passTypeBox.getSelectionModel().getSelectedItem();
        return pt == null ? PassType.SINGLE : pt;
    }

    /**
     * Maps the selected pass type to the fare service trip name keys.
     *
     * @return one of "Weekly Pass", "Monthly Pass", "Day Pass", or "Single Trip"
     */
    private String selectedTripName() {
        switch (selectedPassType()) {
            case WEEKLY:  return "Weekly Pass";
            case MONTHLY: return "Monthly Pass";
            case DAY:     return "Day Pass";
            case SINGLE:
            default:      return "Single Trip";
        }
    }

    /**
     * Retrieves the chosen quantity; returns 1 if none is selected.
     *
     * @return quantity &ge; 1
     */
    private int quantity() {
        Integer q = qtyBox.getSelectionModel().getSelectedItem();
        return (q == null || q < 1) ? 1 : q;
    }

    /**
     * Returns the base unit price for the currently selected rider and pass type.
     *
     * @return pre-tax unit price for one pass/ticket
     */
    private double unitPrice() {
        return fareRateService.getRate(selectedRider(), selectedTripName());
    }

    /**
     * Rounds to two decimal places.
     *
     * @param v value to round
     * @return {@code v} rounded to two decimals
     */
    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

    /**
     * Recomputes the price estimate (unit, subtotal, tax, total) and updates the UI labels.
     * <p>
     * Uses {@link #unitPrice()}, {@link #quantity()}, and {@link FareRateService#getTax()}.
     * Values are formatted in Canadian dollars.
     */
    private void updateEstimate() {
        double unit = unitPrice();
        int qty = quantity();

        double subtotal = unit * qty;
        double tax      = round2(subtotal * fareRateService.getTax());
        double total    = round2(subtotal + tax);

        estUnitValue.setText(CAD.format(unit));
        estSubtotalValue.setText(CAD.format(subtotal));
        taxValue.setText(CAD.format(tax));
        estTotalValue.setText(CAD.format(total));
    }

    // ===== Event handlers =====

    /**
     * Persists the current selection into {@link PaymentSession} as an {@link OrderSummary}
     * and navigates to the Payment screen.
     * <p>
     * Quantity rules:
     * <ul>
     *   <li>{@link PassType#SINGLE}: user-selected quantity.</li>
     *   <li>Other pass types: quantity is forced to 1.</li>
     * </ul>
     *
     * @param event click event from the "Proceed" button
     */
    @FXML
    private void onProceedToPayment(ActionEvent event) {
        String rider = selectedRider();
        String trip  = selectedTripName();
        int trips    = 1;                 // no “Multiple Pass” anymore
        int qty      = quantity();
        double unit  = unitPrice();

        double subtotal = unit * qty;
        double tax      = round2(subtotal * fareRateService.getTax());
        double total    = round2(subtotal + tax);

        // Save order in session
        paymentSession.setOrigin(PaymentSession.Origin.RELOAD_CARD);

        // If your OrderSummary has (rider, trip, trips, quantity, unitPrice, total):
        paymentSession.setCurrentOrder(new OrderSummary(rider, trip, trips, qty, unit, total));

        // Navigate to payment
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/Payment.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent view = loader.load();
            ((Node) event.getSource()).getScene().setRoot(view);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Brand click handler. Navigates to the welcome screen.
     *
     * @param event mouse event from the brand link
     */
    @FXML
    private void onBrandClick(MouseEvent event) { goWelcome((Node) event.getSource()); }

    /**
     * Loads and shows the welcome screen in the current scene.
     *
     * @param n any node in the current scene (used to resolve the scene/root)
     */
    private void goWelcome(Node n) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/welcome-screen.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent home = loader.load();
            n.getScene().setRoot(home);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Back button handler. Navigates to the Home screen.
     *
     * @param event click from the "Back" button
     */
    @FXML
    private void onBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/Home.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent home = loader.load();
            ((Node) event.getSource()).getScene().setRoot(home);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Placeholder for volume/text-to-speech control integration.
     * Implement if kiosk requires audible guidance.
     *
     * @param e action event from a volume control
     */
    @FXML
    private void onVolume(ActionEvent e) {}

    // ===== Internal constant typo fix (internal use only) =====
    // Corrected local reference to the clock format within initialize.
    private static final DateTimeFormatter CCLOCK_FMT = CLOCK_FMT;
}
