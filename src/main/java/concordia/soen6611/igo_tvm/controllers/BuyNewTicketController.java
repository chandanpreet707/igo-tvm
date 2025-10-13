package concordia.soen6611.igo_tvm.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.util.Duration;
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

    // Rider segmented buttons (4 options)
    @FXML private ToggleButton adultBtn, studentBtn, seniorBtn, touristBtn;
    @FXML private ToggleGroup  riderGroup;     // injected via <fx:define>

    // Trip type segmented buttons (5 options)
    @FXML private ToggleButton tripSingle, tripMulti, tripDay, tripMonthly, tripWeekend;
    @FXML private ToggleGroup  tripGroup;      // injected via <fx:define>

    // Multiple Trip controls
    @FXML private Label multiCountLabel;
    @FXML private ComboBox<Integer> multiCountCombo;

    // Qty & totals
    @FXML private TextField qtyField;
    @FXML private Label unitPriceValue, totalValue;
    @FXML private Button addToCartBtn;
    @FXML private Button backBtn;

    private final ApplicationContext appContext;

    private Timeline clock;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a");

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
    // ===============================================================

    public BuyNewTicketController(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    @FXML
    private void initialize() {
        // Live clock
        clock = new Timeline(
                new KeyFrame(Duration.ZERO,
                        e -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // Defaults
        if (riderGroup.getSelectedToggle() == null && adultBtn != null)   adultBtn.setSelected(true);
        if (tripGroup.getSelectedToggle()  == null && tripSingle != null) tripSingle.setSelected(true);

        // Init multiple-trip combo (1..10, default 1)
        for (int i = 1; i <= 10; i++) multiCountCombo.getItems().add(i);
        multiCountCombo.setValue(1);

        // Show/hide multiple-trip chooser depending on selection
        bindMultipleTripVisibility();
        multiCountCombo.setOnAction(e -> recalc());

        // React to changes
        riderGroup.selectedToggleProperty().addListener((o, ov, nv) -> recalc());
        tripGroup.selectedToggleProperty().addListener((o, ov, nv) -> {
            bindMultipleTripVisibility();
            recalc();
        });

        // Quantity numeric guard
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
        // When hidden, managed=false removes it from layout spacing
        multiCountLabel.setVisible(show);
        multiCountLabel.setManaged(show);
        multiCountCombo.setVisible(show);
        multiCountCombo.setManaged(show);
    }

    // Handlers from segmented controls
    @FXML private void onRiderTypeChange(ActionEvent e) { recalc(); }
    @FXML private void onTripChange(ActionEvent e)      { /* handled via listener; keep for FXML */ }

    // Left menu shortcuts → select trip type
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
        // Selected visuals via CSS (.seg-btn:selected)
    }

    private double currentUnitPrice() {
        // Determine the rider’s single-trip price
        double single = riderSinglePrice();

        if (tripSingle.isSelected())  return single;
        if (tripMulti.isSelected())   return single * multiTrips();  // scale by chosen trip count
        if (tripDay.isSelected())     return riderDayPrice();
        if (tripMonthly.isSelected()) return riderMonthlyPrice();
        if (tripWeekend.isSelected()) return riderWeekendPrice();

        return single;
    }

    private double riderSinglePrice() {
        if (adultBtn   != null && adultBtn.isSelected())   return ADULT_SINGLE;
        if (studentBtn != null && studentBtn.isSelected()) return STUDENT_SINGLE;
        if (seniorBtn  != null && seniorBtn.isSelected())  return SENIOR_SINGLE;
        if (touristBtn != null && touristBtn.isSelected()) return TOURIST_SINGLE;
        return ADULT_SINGLE;
    }

    private double riderDayPrice() {
        if (adultBtn   != null && adultBtn.isSelected())   return ADULT_DAY;
        if (studentBtn != null && studentBtn.isSelected()) return STUDENT_DAY;
        if (seniorBtn  != null && seniorBtn.isSelected())  return SENIOR_DAY;
        if (touristBtn != null && touristBtn.isSelected()) return TOURIST_DAY;
        return ADULT_DAY;
    }

    private double riderMonthlyPrice() {
        if (adultBtn   != null && adultBtn.isSelected())   return ADULT_MONTHLY;
        if (studentBtn != null && studentBtn.isSelected()) return STUDENT_MONTHLY;
        if (seniorBtn  != null && seniorBtn.isSelected())  return SENIOR_MONTHLY;
        if (touristBtn != null && touristBtn.isSelected()) return TOURIST_MONTHLY;
        return ADULT_MONTHLY;
    }

    private double riderWeekendPrice() {
        if (adultBtn   != null && adultBtn.isSelected())   return ADULT_WEEKEND;
        if (studentBtn != null && studentBtn.isSelected()) return STUDENT_WEEKEND;
        if (seniorBtn  != null && seniorBtn.isSelected())  return SENIOR_WEEKEND;
        if (touristBtn != null && touristBtn.isSelected()) return TOURIST_WEEKEND;
        return ADULT_WEEKEND;
    }

    @FXML
    private void addToCart() {
        System.out.printf("Added: %s - %s%s x%d @ %s (total %s)%n",
                selectedRiderName(),
                selectedTripName(),
                tripMulti.isSelected() ? (" (" + multiTrips() + " trips)") : "",
                qty(),
                unitPriceValue.getText(),
                totalValue.getText());
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

    // Back to Home
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
}
