package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.*;
import concordia.soen6611.igo_tvm.models.OrderSummary;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JavaFX controller for the “Buy New Ticket” flow.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Initialize and localize the UI (via {@link I18nService}).</li>
 *   <li>Bind rider/trip selection controls and quantity to ticket pricing (via {@link FareRateService}).</li>
 *   <li>Compute subtotal, tax, and total in real time and render them.</li>
 *   <li>Persist the current order into {@link PaymentSession} and navigate to the payment screen.</li>
 *   <li>Provide accessibility helpers (text zoom via {@code TextZoomService} and contrast via {@code ContrastManager}).</li>
 * </ul>
 * Notes:
 * <ul>
 *   <li>Quantity is only editable for <em>Single Trip</em>. For all other trip types quantity is forced to 1.</li>
 *   <li>Taxes are applied using the composite tax rate supplied by {@link FareRateService#getTax()}.</li>
 *   <li>This controller uses Spring’s prototype scope so each navigation creates a fresh instance.</li>
 * </ul>
 *
 * @Mohammad Al-Shariar
 * @since 1.0
 */
@Controller
@org.springframework.context.annotation.Scope("prototype")
public class BuyNewTicketController {

    /** Left menu: Weekly button (injected via FXML). */
    public Button menuWeeklyBtn;

    /** Label for the tax header. */
    public Label taxLabel;

    @FXML private Button incBtn;
    @FXML private Button decBtn;

    // ==== Headings and labels ====
    @FXML private Label buyNewTicketLabel;
    @FXML private Label totalLabel;
    @FXML private Label riderTypeLabel;
    @FXML private Label tripTypeLabel;
    @FXML private Label priceLabel;
    @FXML private Label quantityLabel;

    // ==== Left menu (quick trip-type selectors) ====
    @FXML private Button menuSingleBtn;
    @FXML private Button menuDayBtn;
    @FXML private Button menuMonthlyBtn;
    @FXML private Button menuWeekendBtn;

    // ==== Misc. UI ====
    @FXML private Label clockLabel;
    @FXML private Label questionLabel;
    @FXML private Label helpLabel;

    // ==== Rider type ====
    @FXML private ToggleButton adultBtn, studentBtn, seniorBtn;
    @FXML private ToggleGroup riderGroup;

    // ==== Trip type ====
    @FXML private ToggleButton tripSingle, tripDay, tripMonthly, tripWeekend, tripWeekly;
    @FXML private ToggleGroup tripGroup;

    // ==== Pricing I/O ====
    @FXML private TextField qtyField;
    @FXML private Label unitValueLabel, totalValue, taxValue;
    @FXML private Button makePaymentBtn;
    @FXML private Button backBtn;

    // ==== Infrastructure & services ====
    @Autowired private ApplicationContext appContext;
    @Autowired private PaymentSession paymentSession;
    @Autowired private I18nService i18n;
    @Autowired private FareRateService fareRateService;

    /** Ticking clock that updates the header time every second. */
    private Timeline clock;

    /** Clock format used by the on-screen clock. */
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a");

    //    @FXML private Button btnFontSizeIn, btnFontSizeOut;

    @FXML private Label brandLink;
    @FXML private javafx.scene.Parent root;

    // ===============================================================

    /**
     * JavaFX lifecycle hook invoked after FXML fields are injected.
     * <p>
     * Initializes:
     * <ul>
     *   <li>Header clock and periodic updates.</li>
     *   <li>Default rider (Adult) and trip (Single Trip) selections (if none chosen).</li>
     *   <li>Listeners to recompute pricing on selection/quantity changes.</li>
     *   <li>Quantity field constraints (digits only, minimum of 1).</li>
     *   <li>Localization of all visible labels and buttons.</li>
     *   <li>Accessibility helpers: {@code TextZoomService} registration and {@code ContrastManager} attachment.</li>
     * </ul>
     */
    @FXML
    private void initialize() {
        clock = new Timeline(
                new KeyFrame(Duration.ZERO,
                        e -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        if (riderGroup.getSelectedToggle() == null && adultBtn != null) adultBtn.setSelected(true);
        if (tripGroup.getSelectedToggle() == null && tripSingle != null) tripSingle.setSelected(true);

        riderGroup.selectedToggleProperty().addListener((o, ov, nv) -> recalc());
        tripGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            updateQtyAvailability();
            recalc();
        });
        updateQtyAvailability();

        qtyField.textProperty().addListener((o, oldV, newV) -> {
            if (!newV.matches("\\d*")) {
                qtyField.setText(oldV);
                return;
            }
            if (newV.isEmpty() || newV.equals("0")) {
                qtyField.setText("1");
                return;
            }
            recalc();
        });
        recalc();

        Platform.runLater(() -> {
            var zoom = TextZoomService.get();
            zoom.register(brandLink, buyNewTicketLabel, questionLabel, helpLabel, clockLabel, menuSingleBtn,
                    taxValue, menuDayBtn, menuMonthlyBtn, menuWeekendBtn, menuWeeklyBtn, riderTypeLabel,
                    tripTypeLabel, priceLabel, quantityLabel, totalLabel, adultBtn, studentBtn, seniorBtn, tripSingle,
                    tripDay, tripMonthly, tripWeekend, tripWeekly, qtyField, unitValueLabel, totalValue, makePaymentBtn,
                    backBtn, incBtn, decBtn, taxLabel);
        });

        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
        });
        updateTexts();
    }

    /**
     * Localizes all visible strings from the {@link I18nService}.
     * <p>
     * Called at initialization; call again if the app language changes at runtime.
     */
    private void updateTexts() {
        buyNewTicketLabel.setText(i18n.get("buyNewTicket.title"));
        questionLabel.setText(i18n.get("buyNewTicket.question"));

        // Rider buttons (no tourist)
        adultBtn.setText(i18n.get("buyNewTicket.adult"));
        studentBtn.setText(i18n.get("buyNewTicket.student"));
        seniorBtn.setText(i18n.get("buyNewTicket.senior"));

        // Trip types (no multiple)
        tripSingle.setText(i18n.get("buyNewTicket.single"));
        tripDay.setText(i18n.get("buyNewTicket.day"));
        tripMonthly.setText(i18n.get("buyNewTicket.monthly"));
        tripWeekend.setText(i18n.get("buyNewTicket.weekend"));
        tripWeekly.setText(i18n.get("buyNewTicket.tripWeekly"));

        // Labels
        riderTypeLabel.setText(i18n.get("buyNewTicket.riderType"));
        tripTypeLabel.setText(i18n.get("buyNewTicket.tripType"));
        priceLabel.setText(i18n.get("buyNewTicket.priceEach"));
        quantityLabel.setText(i18n.get("buyNewTicket.quantity"));
        taxLabel.setText(i18n.get("buyNewTicket.tax"));
        totalLabel.setText(i18n.get("buyNewTicket.total"));

        // Left menu (no multiple)
        menuSingleBtn.setText(i18n.get("buyNewTicket.menuSingle"));
        menuDayBtn.setText(i18n.get("buyNewTicket.menuDay"));
        menuMonthlyBtn.setText(i18n.get("buyNewTicket.menuMonthly"));
        menuWeekendBtn.setText(i18n.get("buyNewTicket.menuWeekend"));
        menuWeeklyBtn.setText(i18n.get("buyNewTicket.menuWeeklyBtn"));

        makePaymentBtn.setText(i18n.get("buyNewTicket.makePayment"));
        helpLabel.setText(i18n.get("help"));
    }

    /**
     * Enables/disables quantity controls based on selected trip type.
     * <p>
     * For <em>Single Trip</em>, quantity is user-editable; otherwise it is disabled and forced to 1.
     */
    private void updateQtyAvailability() {
        boolean single = (tripSingle != null && tripSingle.isSelected());
        // lock the field and buttons when not Single Trip
        qtyField.setDisable(!single);
        qtyField.setEditable(single);
        incBtn.setDisable(!single);
        decBtn.setDisable(!single);

        if (!single) {
            // force quantity to 1 for non-single types
            if (!"1".equals(qtyField.getText())) {
                qtyField.setText("1");
            }
        }
    }

    /**
     * Event handler for rider toggle changes. Triggers a price recalculation.
     *
     * @param e action event fired by rider selection change
     */
    @FXML
    private void onRiderTypeChange(ActionEvent e) {recalc();}

    /**
     * Event handler for trip toggle changes. Adjusts quantity availability and recalculates price.
     *
     * @param e action event fired by trip selection change
     */
    @FXML
    private void onTripChange(ActionEvent e) {
        updateQtyAvailability();
        recalc();
    }

    /**
     * Left-menu shortcut: selects <em>Single Trip</em>, updates quantity availability, and recalculates.
     * @param e menu action
     */
    @FXML private void onMenuSingle(ActionEvent e) { if (tripSingle != null) { tripSingle.setSelected(true); updateQtyAvailability(); recalc(); } }

    /**
     * Left-menu shortcut: selects <em>Day Pass</em>, updates quantity availability, and recalculates.
     * @param e menu action
     */
    @FXML private void onMenuDay(ActionEvent e)    { if (tripDay != null)    { tripDay.setSelected(true);    updateQtyAvailability(); recalc(); } }

    /**
     * Left-menu shortcut: selects <em>Monthly Pass</em>, updates quantity availability, and recalculates.
     * @param e menu action
     */
    @FXML private void onMenuMonthly(ActionEvent e){ if (tripMonthly != null){ tripMonthly.setSelected(true); updateQtyAvailability(); recalc(); } }

    /**
     * Left-menu shortcut: selects <em>Weekend Pass</em>, updates quantity availability, and recalculates.
     * @param e menu action
     */
    @FXML private void onMenuWeekend(ActionEvent e){ if (tripWeekend != null){ tripWeekend.setSelected(true); updateQtyAvailability(); recalc(); } }

    /**
     * Left-menu shortcut: selects <em>Weekly Pass</em>, updates quantity availability, and recalculates.
     * @param e menu action
     */
    @FXML private void onMenuWeekly(ActionEvent e) { if (tripWeekly != null) { tripWeekly.setSelected(true);  updateQtyAvailability(); recalc(); } }

    /**
     * Increments quantity by 1 (minimum enforced elsewhere).
     */
    @FXML
    private void incrementQty() {
        qtyField.setText(String.valueOf(qty() + 1));
    }

    /**
     * Decrements quantity by 1 but never below 1.
     */
    @FXML
    private void decrementQty() {
        qtyField.setText(String.valueOf(Math.max(1, qty() - 1)));
    }

    /**
     * Parses the quantity from the text field.
     *
     * @return at least 1; defaults to 1 if the field is invalid
     */
    private int qty() {
        try {
            return Math.max(1, Integer.parseInt(qtyField.getText()));
        } catch (NumberFormatException ex) {
            return 1;
        }
    }

    /**
     * Recomputes and renders unit price, tax, and total based on the current rider, trip, and quantity.
     * <p>
     * Tax is computed using {@link FareRateService#getTax()} and rounding is applied to 2 decimals.
     */
    private void recalc() {
        double unit = currentUnitPrice();
        int q = qty();

        double subtotal = unit * q;
        double tax      = round2(subtotal * fareRateService.getTax());
        double total    = round2(subtotal + tax);

        unitValueLabel.setText(String.format("$%.2f", unit));
        taxValue.setText(String.format("$%.2f", tax));
        totalValue.setText(String.format("$%.2f", total));
    }

    /**
     * Rounds a value to two decimal places using {@link Math#round(double)} on cents.
     *
     * @param v raw value
     * @return value rounded to 2 decimals
     */
    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /**
     * Retrieves the current unit price from {@link FareRateService} for the selected rider and trip.
     *
     * @return unit price for a single ticket/pass (pre-tax)
     */
    private double currentUnitPrice() {
        String rider = selectedRiderName();
        String trip  = selectedTripName();
        // Fare service returns base price for one ticket (no multiple scaling needed anymore)
        return fareRateService.getRate(rider, trip);
    }

    /**
     * Finalizes the current selection into an {@link OrderSummary}, stores it in {@link PaymentSession},
     * and navigates to the payment screen.
     * <p>
     * Quantity rules:
     * <ul>
     *   <li>For Single Trip, quantity is user-selected.</li>
     *   <li>For other trip types, quantity is forced to 1.</li>
     * </ul>
     *
     * @param event the action event from the “Make Payment” button
     */
    @FXML
    private void onMakePayment(ActionEvent event) {
        String rider = selectedRiderName();
        String trip = selectedTripName();
        int trips = 1;
        int q = qty();
        double unit = currentUnitPrice();

        double subtotal = unit * q;
        double tax      = round2(subtotal * fareRateService.getTax());
        double total    = round2(subtotal + tax);

        // Save current order in the session
        paymentSession.setOrigin(PaymentSession.Origin.BUY_TICKET);
        paymentSession.setCurrentOrder(new OrderSummary(rider, trip, trips, q, unit, total));

        // Navigate to the Payment page
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
     * Resolves the selected rider name for pricing and persistence.
     *
     * @return one of {@code "Adult"}, {@code "Student"}, {@code "Senior"}; defaults to {@code "Adult"} if none selected
     */
    private String selectedRiderName() {
        if (adultBtn != null && adultBtn.isSelected()) return "Adult";
        if (studentBtn != null && studentBtn.isSelected()) return "Student";
        if (seniorBtn != null && seniorBtn.isSelected()) return "Senior";
        return "Adult";
    }

    /**
     * Resolves the selected trip type for pricing and persistence.
     *
     * @return one of {@code "Single Trip"}, {@code "Day Pass"}, {@code "Monthly Pass"},
     * {@code "Weekend Pass"}, {@code "Weekly Pass"}; defaults to {@code "Single Trip"} if none selected
     */
    private String selectedTripName() {
        if (tripSingle != null && tripSingle.isSelected()) return "Single Trip";
        if (tripDay != null && tripDay.isSelected()) return "Day Pass";
        if (tripMonthly != null && tripMonthly.isSelected()) return "Monthly Pass";
        if (tripWeekend != null && tripWeekend.isSelected()) return "Weekend Pass";
        if (tripWeekly != null && tripWeekly.isSelected()) return "Weekly Pass";
        return "Single Trip";
    }

    /**
     * Navigates back to the home screen.
     *
     * @param event action event from the “Back” button
     */
    public void onBack(ActionEvent event) {
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
     * Stops the ticking clock. Call when the controller is being torn down to avoid leaks.
     */
    public void shutdown() {
        if (clock != null) clock.stop();
    }

    /**
     * Clears the {@link PaymentSession} and returns to the welcome screen when the brand is clicked.
     *
     * @param event mouse click on the brand area
     */
    @FXML
    private void onBrandClick(MouseEvent event) {
        paymentSession.clear();
        goWelcomeScreen((Node) event.getSource());
    }

    /**
     * Replaces the current scene root with the Welcome screen.
     *
     * @param anyNodeInScene any node that belongs to the current scene
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

    /**
     * Volume/TTS action handler placeholder.
     * <p>
     * Implement text-to-speech or volume control wiring here if required by the kiosk.
     *
     * @param actionEvent click event
     */
    public void onVolume(ActionEvent actionEvent) {
    }

    /**
     * Displays a localized help dialog with contact information and a copy-to-clipboard action.
     * <p>
     * Styles are applied from {@code /styles/Modal.css} if available.
     */
    @FXML
    private void onHelpClick() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(i18n.get("home.help.dialogTitle"));  // i18n
        alert.setHeaderText(null);

        // ---- Header row (icon + localized title)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon  = new Label("🛠");
        icon.getStyleClass().add("help-icon");
        Label title = new Label(i18n.get("home.help.header"));  // i18n
        title.getStyleClass().add("help-title");
        header.getChildren().addAll(icon, title);

        // ---- Body (localized labels)
        VBox body = new VBox(8);
        body.getChildren().addAll(
                contactRow(i18n.get("home.help.phone"), "+1 (514) 555-0137"),
                contactRow(i18n.get("home.help.email"), "support@stm.example")
        );

        VBox content = new VBox(14, header, body);
        content.getStyleClass().add("help-content");

        DialogPane pane = alert.getDialogPane();
        pane.setContent(content);

        // Optional: keep your modal CSS
        try {
            pane.getStylesheets().add(getClass().getResource("/styles/Modal.css").toExternalForm());
        } catch (Exception ignored) {}
        pane.getStyleClass().add("help-modal");

        // Localized Close button
        ButtonType closeType = new ButtonType(i18n.get("home.help.close"), ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(closeType);
        Node closeBtn = pane.lookupButton(closeType);
        if (closeBtn != null) closeBtn.getStyleClass().add("help-close-btn");

        alert.showAndWait();
    }

    /**
     * Builds a single help row containing a label, value, and a “Copy” button that
     * writes the given value to the system clipboard.
     *
     * @param labelText localized field label (e.g., “Phone”)
     * @param value     field value (e.g., phone number or email)
     * @return a left-aligned {@link HBox} representing one help entry row
     */
    private HBox contactRow(String labelText, String value) {
        Label label = new Label(labelText);
        label.getStyleClass().add("help-label");

        Label val = new Label(value);
        val.getStyleClass().add("help-value");

        Button copy = new Button(i18n.get("home.help.copy")); // i18n
        copy.getStyleClass().add("help-copy-btn");
        copy.setOnAction(e -> {
            ClipboardContent cc = new ClipboardContent();
            cc.putString(value);
            Clipboard.getSystemClipboard().setContent(cc);
        });

        HBox row = new HBox(10, label, val, copy);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }
}
