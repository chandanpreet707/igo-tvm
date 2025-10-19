package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.PaymentSession;
import concordia.soen6611.igo_tvm.models.OrderSummary;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class CardReloadAmountController {
    private final ApplicationContext appContext;
    private final PaymentSession paymentSession;
    @FXML private ComboBox<String> passTypeBox;
    @FXML private ComboBox<Integer> qtyBox;
    @FXML private Label qtyLabel, estTotalValue;
    private final NumberFormat CAD = NumberFormat.getCurrencyInstance(Locale.CANADA);



    public CardReloadAmountController(ApplicationContext appContext, PaymentSession paymentSession) {
        this.appContext = appContext;
        this.paymentSession = paymentSession;
    }

    @FXML
    private void initialize() {
        // Guard: if items not defined in FXML, you can populate—
        if (passTypeBox.getItems().isEmpty()) {
            passTypeBox.getItems().addAll("Single Pass", "Multiple Pass", "Weekly Pass", "Monthly Pass", "Day Pass");
        }
        if (qtyBox.getItems().isEmpty()) {
            for (int i = 1; i <= 10; i++) qtyBox.getItems().add(i);
        }
        qtyBox.getSelectionModel().select(Integer.valueOf(1));
        passTypeBox.getSelectionModel().selectFirst();

        passTypeBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            boolean multiple = isMultiple(n);
            qtyLabel.setVisible(multiple); qtyLabel.setManaged(multiple);
            qtyBox.setVisible(multiple);   qtyBox.setManaged(multiple);
            updateEstimate();
        });
        qtyBox.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> updateEstimate());

        updateEstimate();
    }

    // ===== helpers required by onMakePayment pattern =====
    private String selectedRiderName() {
        // On reload we typically don’t collect rider type here; default to Adult,
        // or replace with a real selection / session value when you add it.
        return "Adult";
    }

    private String selectedTripName() {
        String pt = passTypeBox.getSelectionModel().getSelectedItem();
        if (pt == null) return "Single Trip";
        switch (pt) {
            case "Multiple Pass": return "Multiple Trip";
            case "Weekly Pass":   return "Weekly Pass";
            case "Monthly Pass":  return "Monthly Pass";
            case "Day Pass":      return "Day Pass";
            default:              return "Single Trip";
        }
    }

    private boolean isMultiple(String passType) {
        return "Multiple Pass".equals(passType);
    }

    /** Number of trips when Multiple Trip is chosen (1..10), else 1 */
    private int multiTrips() {
        Integer val = qtyBox.getSelectionModel().getSelectedItem();
        return val == null ? 1 : val;
    }

    /** Quantity of *tickets/products* (for reload we sell one product at a time) */
    private int qty() {
        // For this screen the “Quantity” row in the mock is actually trips count.
        // Keep product quantity fixed at 1. If you add another quantity control later, return it here.
        return 1;
    }

    /** Unit price for ONE product, already scaled for multi-trips when Multiple Trip is chosen. */
    private double currentUnitPrice() {
        String pt = passTypeBox.getSelectionModel().getSelectedItem();
        if (pt == null) return 0.0;

        switch (pt) {
            case "Single Pass":   return 3.75;
            case "Day Pass":      return 11.00;
            case "Weekly Pass":   return 31.00;
            case "Monthly Pass":  return 94.00;
            case "Multiple Pass": return 3.75 * multiTrips(); // scale by trips
            default:              return 0.0;
        }
    }

    private void updateEstimate() {
        double unit = currentUnitPrice();
        int quantity = qty();
        estTotalValue.setText(CAD.format(unit * quantity));
    }

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

    public void onVolume(ActionEvent actionEvent) {
    }

    @FXML
    private void onProceedToPayment(ActionEvent event) {
        String rider = selectedRiderName();
        String trip  = selectedTripName();
        int trips    = isMultiple(passTypeBox.getSelectionModel().getSelectedItem()) ? multiTrips() : 1;
        int quantity = qty();
        double unit  = currentUnitPrice(); // already scaled for multiple trips

        // Save current order in the session
        paymentSession.setOrigin(PaymentSession.Origin.RELOAD_CARD);
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
}
