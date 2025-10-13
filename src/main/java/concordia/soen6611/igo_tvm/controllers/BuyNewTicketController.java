package concordia.soen6611.igo_tvm.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
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

    // Qty & totals
    @FXML private TextField qtyField;
    @FXML private Label unitPriceValue, totalValue;
    @FXML private Button addToCartBtn;
    @FXML private Button backBtn;

    private final ApplicationContext appContext;

    private Timeline clock;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a");

    // ======= Price table (set these to your real fares) =======
    // Adult
    private static final double ADULT_SINGLE   = 3.75;
    private static final double ADULT_MULTI    = 31.50;  // e.g., 10-trip
    private static final double ADULT_DAY      = 11.00;
    private static final double ADULT_MONTHLY  = 94.00;
    private static final double ADULT_WEEKEND  = 14.00;

    // Student
    private static final double STUDENT_SINGLE  = 3.00;
    private static final double STUDENT_MULTI   = 25.00;
    private static final double STUDENT_DAY     = 8.00;
    private static final double STUDENT_MONTHLY = 70.00;
    private static final double STUDENT_WEEKEND = 10.00;

    // Senior
    private static final double SENIOR_SINGLE   = 2.50;
    private static final double SENIOR_MULTI    = 22.00;
    private static final double SENIOR_DAY      = 7.00;
    private static final double SENIOR_MONTHLY  = 60.00;
    private static final double SENIOR_WEEKEND  = 9.00;

    // Tourist
    private static final double TOURIST_SINGLE  = 4.00;
    private static final double TOURIST_MULTI   = 35.00;
    private static final double TOURIST_DAY     = 12.00;
    private static final double TOURIST_MONTHLY = 99.00;
    private static final double TOURIST_WEEKEND = 16.00;
    // ==========================================================

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

        // React to changes
        riderGroup.selectedToggleProperty().addListener((o, ov, nv) -> recalc());
        tripGroup.selectedToggleProperty().addListener((o, ov, nv)  -> recalc());

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

    // Handlers from segmented controls
    @FXML private void onRiderTypeChange(ActionEvent e) { recalc(); }
    @FXML private void onTripChange(ActionEvent e)      { recalc(); }

    // Left menu shortcuts → select trip type
    @FXML private void onMenuSingle(ActionEvent e)  { if (tripSingle  != null) { tripSingle.setSelected(true);  recalc(); } }
    @FXML private void onMenuMulti(ActionEvent e)   { if (tripMulti   != null) { tripMulti.setSelected(true);   recalc(); } }
    @FXML private void onMenuDay(ActionEvent e)     { if (tripDay     != null) { tripDay.setSelected(true);     recalc(); } }
    @FXML private void onMenuMonthly(ActionEvent e) { if (tripMonthly != null) { tripMonthly.setSelected(true); recalc(); } }
    @FXML private void onMenuWeekend(ActionEvent e) { if (tripWeekend != null) { tripWeekend.setSelected(true); recalc(); } }

    @FXML private void incrementQty() { qtyField.setText(String.valueOf(qty() + 1)); }
    @FXML private void decrementQty() { qtyField.setText(String.valueOf(Math.max(1, qty() - 1))); }

    private int qty() {
        try { return Math.max(1, Integer.parseInt(qtyField.getText())); }
        catch (NumberFormatException ex) { return 1; }
    }

    private void recalc() {
        double unit = currentUnitPrice();
        int q = qty();
        unitPriceValue.setText(String.format("$%.2f", unit));
        totalValue.setText(String.format("$%.2f", unit * q));
        // CSS handles selected visuals: .seg-btn:selected { ... }
    }

    private double currentUnitPrice() {
        // Which rider?
        boolean isAdult   = adultBtn   != null && adultBtn.isSelected();
        boolean isStudent = studentBtn != null && studentBtn.isSelected();
        boolean isSenior  = seniorBtn  != null && seniorBtn.isSelected();
        boolean isTourist = touristBtn != null && touristBtn.isSelected();

        // Which trip?
        boolean tSingle   = tripSingle  != null && tripSingle.isSelected();
        boolean tMulti    = tripMulti   != null && tripMulti.isSelected();
        boolean tDay      = tripDay     != null && tripDay.isSelected();
        boolean tMonthly  = tripMonthly != null && tripMonthly.isSelected();
        boolean tWeekend  = tripWeekend != null && tripWeekend.isSelected();

        if (isAdult) {
            if (tSingle)  return ADULT_SINGLE;
            if (tMulti)   return ADULT_MULTI;
            if (tDay)     return ADULT_DAY;
            if (tMonthly) return ADULT_MONTHLY;
            if (tWeekend) return ADULT_WEEKEND;
        } else if (isStudent) {
            if (tSingle)  return STUDENT_SINGLE;
            if (tMulti)   return STUDENT_MULTI;
            if (tDay)     return STUDENT_DAY;
            if (tMonthly) return STUDENT_MONTHLY;
            if (tWeekend) return STUDENT_WEEKEND;
        } else if (isSenior) {
            if (tSingle)  return SENIOR_SINGLE;
            if (tMulti)   return SENIOR_MULTI;
            if (tDay)     return SENIOR_DAY;
            if (tMonthly) return SENIOR_MONTHLY;
            if (tWeekend) return SENIOR_WEEKEND;
        } else if (isTourist) {
            if (tSingle)  return TOURIST_SINGLE;
            if (tMulti)   return TOURIST_MULTI;
            if (tDay)     return TOURIST_DAY;
            if (tMonthly) return TOURIST_MONTHLY;
            if (tWeekend) return TOURIST_WEEKEND;
        }

        // Fallbacks (shouldn't hit if one rider & one trip are always selected)
        return ADULT_SINGLE;
    }

    @FXML
    private void addToCart() {
        System.out.printf("Added: %s - %s x%d @ %s (total %s)%n",
                selectedRiderName(),
                selectedTripName(),
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
