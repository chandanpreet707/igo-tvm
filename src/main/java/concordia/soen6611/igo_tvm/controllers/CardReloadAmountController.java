package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.*;
import concordia.soen6611.igo_tvm.models.OrderSummary;
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

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class CardReloadAmountController implements Initializable {
    private final ApplicationContext appContext;
    private final PaymentSession paymentSession;

    // Header
    @FXML public Label brandLink;
    @FXML public Label clockLabel;

    // Left (card)
    @FXML public Label youCardLabel;
    @FXML public ImageView opusCardImage;
//    @FXML public Label opusCardLabel;
    @FXML public Label riderTypeTag;

    // Right (options)
    @FXML public Label reloadOptionLabel;
    @FXML public Label selectTypeLabel;
    @FXML public ComboBox<String> passTypeBox;
    @FXML public Label qtyLabel;
    @FXML public ComboBox<Integer> qtyBox;


    // Price breakdown
    @FXML public Label estUnitValue;
    @FXML public Label estSubtotalValue;
    @FXML public Label taxLineLabel;
    @FXML public Label taxValue;
    @FXML public Label estimatedTotalLabel;
    @FXML public Label estTotalValue;


    @FXML public Button proceedBtn;
    @FXML public BorderPane root;
    @FXML public Label reloadCardLabel;
    @FXML public Label helpLabel;
    public Label unitPriceLabel;
    public Label subTotalLabel;


    private Timeline clock;
    private final NumberFormat CAD = NumberFormat.getCurrencyInstance(Locale.CANADA);
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");
    private final I18nService i18n;

    @Autowired
    private FareRateService fareRateService;


    public CardReloadAmountController(ApplicationContext appContext, PaymentSession paymentSession, I18nService i18n) {
        this.appContext = appContext;
        this.paymentSession = paymentSession;
        this.i18n = i18n;
    }

    @FXML
    public void initialize(URL location, ResourceBundle resources) {
        riderTypeTag.setText("Adult"); // "Adult", "Student", etc.
        updateTexts();
        i18n.localeProperty().addListener((obs, oldL, newL) -> updateTexts());

        // Live clock
        clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // Guard: if items not defined in FXML, you can populate
        if (passTypeBox.getItems().isEmpty()) {
            passTypeBox.getItems().addAll("Single Pass", "Weekly Pass", "Monthly Pass", "Day Pass");
        }
        if (qtyBox.getItems().isEmpty()) {
            for (int i = 1; i <= 10; i++) qtyBox.getItems().add(i);
        }

        qtyBox.getSelectionModel().select(Integer.valueOf(1));
        passTypeBox.getSelectionModel().selectFirst();

        passTypeBox.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> updateEstimate());
        qtyBox.getSelectionModel().selectedItemProperty().addListener((o, ov, nv) -> updateEstimate());

        updateEstimate();

        Platform.runLater(() -> {
            var zoom = TextZoomService.get();
            zoom.register((Node) brandLink, reloadCardLabel, clockLabel, youCardLabel,opusCardImage, passTypeBox,
                    qtyLabel, qtyBox, estimatedTotalLabel, estTotalValue, proceedBtn, helpLabel);
        });

        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
        });
    }

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
//        passTypeList.set(passTypeList.indexOf(menuSinglePass),this.resources.getString("cardReloadAmount.menu.single.pass"));
        // Dynamic tax % in the line label
//        double taxPct = fareRateService.getTax() * 100.0; // e.g., 14.975
//        taxLineLabel.setText(String.format("Tax (%.3f%%):", taxPct));

    }

    // ===== helpers required by onMakePayment pattern =====

    private String selectedRider() {
        // If you store rider in session, fetch it; otherwise use tag text.
        String tag = riderTypeTag.getText();
        return (tag == null || tag.isBlank()) ? "Adult" : tag.trim();
    }
    private String selectedTripName() {
        String pt = passTypeBox.getSelectionModel().getSelectedItem();
        if (pt == null) return "Single Trip";
        switch (pt) {
            case "Weekly Pass":  return "Weekly Pass";
            case "Monthly Pass": return "Monthly Pass";
            case "Day Pass":     return "Day Pass";
            default:             return "Single Trip";
        }
    }

    private int quantity() {
        Integer q = qtyBox.getSelectionModel().getSelectedItem();
        return (q == null || q < 1) ? 1 : q;
    }

    /** Base price per ONE pass, by rider + trip. */
    private double unitPrice() {
        return fareRateService.getRate(selectedRider(), selectedTripName());
    }

    private static double round2(double v) { return Math.round(v * 100.0) / 100.0; }

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

        // Use the constructor your project expects:
        // If your OrderSummary has (rider, trip, trips, quantity, unitPrice, total):
        paymentSession.setCurrentOrder(new OrderSummary(rider, trip, trips, qty, unit, total));

        // If your OrderSummary signature is different, adjust accordingly.

        // Navigate
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/Payment.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent view = loader.load();
            ((Node) event.getSource()).getScene().setRoot(view);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    // Navigation helpers
    @FXML
    private void onBrandClick(MouseEvent event) { goWelcome((Node) event.getSource()); }

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

    @FXML
    private void onVolume(ActionEvent e) {}
}
