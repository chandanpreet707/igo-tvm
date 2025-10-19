package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.FareRateService;
import concordia.soen6611.igo_tvm.Services.I18nService;
import concordia.soen6611.igo_tvm.Services.ContrastManager;
import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.Services.TextZoomService;
import concordia.soen6611.igo_tvm.models.OrderSummary;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class BuyNewTicketController {

    @FXML private Label buyNewTicketLabel;
    @FXML private Label totalLabel;
    @FXML private Label riderTypeLabel;
    @FXML private Label tripTypeLabel;
    @FXML private Label priceLabel;
    @FXML private Label quantityLabel;
    @FXML private Button menuSingleBtn;
    @FXML private Button menuMultiBtn;
    @FXML private Button menuDayBtn;
    @FXML private Button menuMonthlyBtn;
    @FXML private Button menuWeekendBtn;
    @FXML private Label promptLabel;
    @FXML private Label clockLabel;
    @FXML private Label questionLabel;
    @FXML private Label helpLabel;

    @FXML private ToggleButton adultBtn, studentBtn, seniorBtn, touristBtn;
    @FXML private ToggleGroup  riderGroup;

    @FXML private ToggleButton tripSingle, tripMulti, tripDay, tripMonthly, tripWeekend;
    @FXML private ToggleGroup  tripGroup;

    @FXML private Label multiCountLabel;
    @FXML private ComboBox<Integer> multiCountCombo;

    @FXML private TextField qtyField;
    @FXML private Label unitValueLabel, totalValue;
    @FXML private Button makePaymentBtn;
    @FXML private Button backBtn;

    private final ApplicationContext appContext;
    private final PaymentSession paymentSession;
    private final I18nService i18n;


    @Autowired
    private FareRateService fareRateService;

    private Timeline clock;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a");

    public BuyNewTicketController(ApplicationContext appContext, PaymentSession paymentSession) {
    // ======= Price table (per rider; set to your real fares) =======
    // Adult
    private static final double ADULT_SINGLE   = 3.75;
    private static final double ADULT_DAY      = 11.00;
    private static final double ADULT_MONTHLY  = 94.00;
    private static final double ADULT_WEEKEND  = 14.00;

    // Student
    private static final double STUDENT_SINGLE  = 3.00;
    private static final double STUDENT_DAY     = 8.00;
    private static final double STUDENT_MONTHLY = 70.00;
    private static final double STUDENT_WEEKEND = 10.00;

    // Senior
    private static final double SENIOR_SINGLE   = 2.50;
    private static final double SENIOR_DAY      = 7.00;
    private static final double SENIOR_MONTHLY  = 60.00;
    private static final double SENIOR_WEEKEND  = 9.00;

    // Tourist
    private static final double TOURIST_SINGLE  = 4.00;
    private static final double TOURIST_DAY     = 12.00;
    private static final double TOURIST_MONTHLY = 99.00;
    private static final double TOURIST_WEEKEND = 16.00;
//    @FXML private Button btnFontSizeIn, btnFontSizeOut;
    @FXML private Label brandLink;
    @FXML private javafx.scene.Parent root;
    // ===============================================================

    public BuyNewTicketController(I18nService i18n, ApplicationContext appContext, PaymentSession paymentSession) {
        this.i18n = i18n;
        this.appContext = appContext;
        this.paymentSession = paymentSession;
    }

    @FXML
    private void initialize() {
        clock = new Timeline(
                new KeyFrame(Duration.ZERO,
                        e -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        if (riderGroup.getSelectedToggle() == null && adultBtn != null)   adultBtn.setSelected(true);
        if (tripGroup.getSelectedToggle()  == null && tripSingle != null) tripSingle.setSelected(true);

        for (int i = 1; i <= 10; i++) multiCountCombo.getItems().add(i);
        multiCountCombo.setValue(1);

        bindMultipleTripVisibility();
        multiCountCombo.setOnAction(e -> recalc());

        riderGroup.selectedToggleProperty().addListener((o, ov, nv) -> recalc());
        tripGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            bindMultipleTripVisibility();
            recalc();
        });

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
                    menuMultiBtn, menuDayBtn, menuMonthlyBtn, menuWeekendBtn, riderTypeLabel,
                    tripTypeLabel, priceLabel, quantityLabel, totalLabel, adultBtn, studentBtn, seniorBtn,
                    touristBtn, tripSingle, tripMulti, tripDay, tripMonthly, tripWeekend, qtyField, unitValueLabel, totalValue, makePaymentBtn, backBtn);
//            reflectZoomButtons();
        });

        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
        });
        updateTexts();
    }

    private void updateTexts() {
        buyNewTicketLabel.setText(i18n.get("buyNewTicket.title"));
        questionLabel.setText(i18n.get("buyNewTicket.question"));
        adultBtn.setText(i18n.get("buyNewTicket.adult"));
        studentBtn.setText(i18n.get("buyNewTicket.student"));
        seniorBtn.setText(i18n.get("buyNewTicket.senior"));
        touristBtn.setText(i18n.get("buyNewTicket.tourist"));
        tripSingle.setText(i18n.get("buyNewTicket.single"));
        tripMulti.setText(i18n.get("buyNewTicket.multi"));
        tripDay.setText(i18n.get("buyNewTicket.day"));
        tripMonthly.setText(i18n.get("buyNewTicket.monthly"));
        tripWeekend.setText(i18n.get("buyNewTicket.weekend"));
        multiCountLabel.setText(i18n.get("buyNewTicket.quantity"));
        makePaymentBtn.setText(i18n.get("buyNewTicket.makePayment"));
        totalLabel.setText(i18n.get("buyNewTicket.total"));
        riderTypeLabel.setText(i18n.get("buyNewTicket.riderType"));
        tripTypeLabel.setText(i18n.get("buyNewTicket.tripType"));
        priceLabel.setText(i18n.get("buyNewTicket.priceEach"));
        quantityLabel.setText(i18n.get("buyNewTicket.quantity"));
        menuSingleBtn.setText(i18n.get("buyNewTicket.menuSingle"));
        menuMultiBtn.setText(i18n.get("buyNewTicket.menuMulti"));
        menuDayBtn.setText(i18n.get("buyNewTicket.menuDay"));
        menuMonthlyBtn.setText(i18n.get("buyNewTicket.menuMonthly"));
        menuWeekendBtn.setText(i18n.get("buyNewTicket.menuWeekend"));
        helpLabel.setText(i18n.get("help"));
    }


    private void bindMultipleTripVisibility() {
        boolean show = tripMulti != null && tripMulti.isSelected();
        multiCountLabel.setVisible(show);
        multiCountLabel.setManaged(show);
        multiCountCombo.setVisible(show);
        multiCountCombo.setManaged(show);
    }

    @FXML private void onRiderTypeChange(ActionEvent e) { recalc(); }
    @FXML private void onTripChange(ActionEvent e)      { /* handled via listener */ }

    @FXML private void onMenuSingle(ActionEvent e)  { if (tripSingle  != null) { tripSingle.setSelected(true);  recalc(); bindMultipleTripVisibility(); } }
    @FXML private void onMenuMulti(ActionEvent e)   { if (tripMulti   != null) { tripMulti.setSelected(true);   recalc(); bindMultipleTripVisibility(); } }
    @FXML private void onMenuDay(ActionEvent e)     { if (tripDay     != null) { tripDay.setSelected(true);     recalc(); bindMultipleTripVisibility(); } }
    @FXML private void onMenuMonthly(ActionEvent e) { if (tripMonthly != null) { tripMonthly.setSelected(true); recalc(); bindMultipleTripVisibility(); } }
    @FXML private void onMenuWeekend(ActionEvent e) { if (tripWeekend != null) { tripWeekend.setSelected(true); recalc(); bindMultipleTripVisibility(); } }

    @FXML private void incrementQty() { qtyField.setText(String.valueOf(qty() + 1)); }
    @FXML private void decrementQty() { qtyField.setText(String.valueOf(Math.max(1, qty() - 1))); }

    private int qty() {
        try { return Math.max(1, Integer.parseInt(qtyField.getText())); }
        catch (NumberFormatException ex) { return 1; }
    }

    private int multiTrips() {
        Integer v = multiCountCombo.getValue();
        return (v == null || v < 1) ? 1 : v;
    }

    private void recalc() {
        double unit = currentUnitPrice();
        int q = qty();
        unitValueLabel.setText(String.format("$%.2f", unit));
        totalValue.setText(String.format("$%.2f", unit * q));
    }

    private double currentUnitPrice() {
        String rider = selectedRiderName();
        String trip = selectedTripName();
        double baseRate = fareRateService.getRate(rider, trip);

        if (tripMulti != null && tripMulti.isSelected()) {
            return baseRate * multiTrips();
        }
        return baseRate;
    }

    @FXML
    private void onMakePayment(ActionEvent event) {
        String rider = selectedRiderName();
        String trip  = selectedTripName();
        int trips = tripMulti != null && tripMulti.isSelected() ? multiTrips() : 1;
        int quantity = qty();
        double unit = currentUnitPrice();
        double unit = currentUnitPrice(); // already scaled for multiple trips


        // Save current order in the session
        paymentSession.setOrigin(PaymentSession.Origin.BUY_TICKET);
        paymentSession.setCurrentOrder(new OrderSummary(rider, trip, trips, quantity, unit));

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

    private String selectedRiderName() {
        if (adultBtn   != null && adultBtn.isSelected())   return "Adult";
        if (studentBtn != null && studentBtn.isSelected()) return "Student";
        if (seniorBtn  != null && seniorBtn.isSelected())  return "Senior";
        if (touristBtn != null && touristBtn.isSelected()) return "Tourist";
        return "Adult";
    }

    private String selectedTripName() {
        if (tripSingle  != null && tripSingle.isSelected())  return "Single Trip";
        if (tripMulti   != null && tripMulti.isSelected())   return "Multiple Trip";
        if (tripDay     != null && tripDay.isSelected())     return "Day Pass";
        if (tripMonthly != null && tripMonthly.isSelected()) return "Monthly Pass";
        if (tripWeekend != null && tripWeekend.isSelected()) return "Weekend Pass";
        return "Single Trip";
    }

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
    public void shutdown() { if (clock != null) clock.stop(); }
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

    public void onVolume(ActionEvent actionEvent) {
    }
}
