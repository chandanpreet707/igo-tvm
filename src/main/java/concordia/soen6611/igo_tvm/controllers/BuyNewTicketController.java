package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.FareRateService;
import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.models.OrderSummary;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
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
    @FXML private Label promptLabel;
    @FXML private Label clockLabel;
    @FXML private Label questionLabel;

    @FXML private ToggleButton adultBtn, studentBtn, seniorBtn, touristBtn;
    @FXML private ToggleGroup  riderGroup;

    @FXML private ToggleButton tripSingle, tripMulti, tripDay, tripMonthly, tripWeekend;
    @FXML private ToggleGroup  tripGroup;

    @FXML private Label multiCountLabel;
    @FXML private ComboBox<Integer> multiCountCombo;

    @FXML private TextField qtyField;
    @FXML private Label unitPriceValue, totalValue;
    @FXML private Button makePaymentBtn;
    @FXML private Button backBtn;

    private final ApplicationContext appContext;
    private final PaymentSession paymentSession;

    @Autowired
    private FareRateService fareRateService;

    private Timeline clock;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a");

    public BuyNewTicketController(ApplicationContext appContext, PaymentSession paymentSession) {
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
        unitPriceValue.setText(String.format("$%.2f", unit));
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
        paymentSession.setCurrentOrder(new OrderSummary(rider, trip, trips, quantity, unit));

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

    public void onVolume(ActionEvent actionEvent) {
    }
}
