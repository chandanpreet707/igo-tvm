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

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class BuyNewTicketController {

    @FXML
    private Label buyNewTicketLabel;
    @FXML
    private Label totalLabel;
    @FXML
    private Label riderTypeLabel;
    @FXML
    private Label tripTypeLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label quantityLabel;
    @FXML
    private Button menuSingleBtn;
    @FXML
    private Button menuMultiBtn;
    @FXML
    private Button menuDayBtn;
    @FXML
    private Button menuMonthlyBtn;
    @FXML
    private Button menuWeekendBtn;
    @FXML
    private Label clockLabel;
    @FXML
    private Label questionLabel;
    @FXML
    private Label helpLabel;

    @FXML
    private ToggleButton adultBtn, studentBtn, seniorBtn;
    @FXML
    private ToggleGroup riderGroup;

    @FXML
    private ToggleButton tripSingle, tripMulti, tripDay, tripMonthly, tripWeekend;
    @FXML
    private ToggleGroup tripGroup;

    @FXML
    private Label multiCountLabel;
    @FXML
    private ComboBox<Integer> multiCountCombo;

    @FXML
    private TextField qtyField;
    @FXML
    private Label unitValueLabel, totalValue;
    @FXML
    private Button makePaymentBtn;
    @FXML
    private Button backBtn;

    @Autowired
    private  ApplicationContext appContext;
    @Autowired
    private PaymentSession paymentSession;
    @Autowired
    private I18nService i18n;


    @Autowired
    private FareRateService fareRateService;

    private Timeline clock;
    private static final DateTimeFormatter CLOCK_FMT = DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a");

    //    @FXML private Button btnFontSizeIn, btnFontSizeOut;
    @FXML
    private Label brandLink;
    @FXML
    private javafx.scene.Parent root;
    // ===============================================================


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
                     tripSingle, tripMulti, tripDay, tripMonthly, tripWeekend, qtyField, unitValueLabel, totalValue, makePaymentBtn, backBtn);
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

    @FXML
    private void onRiderTypeChange(ActionEvent e) {
        recalc();
    }

    @FXML
    private void onTripChange(ActionEvent e) { /* handled via listener */ }

    @FXML
    private void onMenuSingle(ActionEvent e) {
        if (tripSingle != null) {
            tripSingle.setSelected(true);
            recalc();
            bindMultipleTripVisibility();
        }
    }

    @FXML
    private void onMenuMulti(ActionEvent e) {
        if (tripMulti != null) {
            tripMulti.setSelected(true);
            recalc();
            bindMultipleTripVisibility();
        }
    }

    @FXML
    private void onMenuDay(ActionEvent e) {
        if (tripDay != null) {
            tripDay.setSelected(true);
            recalc();
            bindMultipleTripVisibility();
        }
    }

    @FXML
    private void onMenuMonthly(ActionEvent e) {
        if (tripMonthly != null) {
            tripMonthly.setSelected(true);
            recalc();
            bindMultipleTripVisibility();
        }
    }

    @FXML
    private void onMenuWeekend(ActionEvent e) {
        if (tripWeekend != null) {
            tripWeekend.setSelected(true);
            recalc();
            bindMultipleTripVisibility();
        }
    }

    @FXML
    private void incrementQty() {
        qtyField.setText(String.valueOf(qty() + 1));
    }

    @FXML
    private void decrementQty() {
        qtyField.setText(String.valueOf(Math.max(1, qty() - 1)));
    }

    private int qty() {
        try {
            return Math.max(1, Integer.parseInt(qtyField.getText()));
        } catch (NumberFormatException ex) {
            return 1;
        }
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
        String trip = selectedTripName();
        int trips = tripMulti != null && tripMulti.isSelected() ? multiTrips() : 1;
        int quantity = qty();
        double unit = currentUnitPrice();


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
        if (adultBtn != null && adultBtn.isSelected()) return "Adult";
        if (studentBtn != null && studentBtn.isSelected()) return "Student";
        if (seniorBtn != null && seniorBtn.isSelected()) return "Senior";
        return "Adult";
    }

    private String selectedTripName() {
        if (tripSingle != null && tripSingle.isSelected()) return "Single Trip";
        if (tripMulti != null && tripMulti.isSelected()) return "Multiple Trip";
        if (tripDay != null && tripDay.isSelected()) return "Day Pass";
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

    public void shutdown() {
        if (clock != null) clock.stop();
    }

    @FXML
    private void onBrandClick(MouseEvent event) {
        paymentSession.clear();
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

    @FXML
    private void onHelpClick() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Need help?");
        alert.setHeaderText(null);

        // ---- Header row (icon + title)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("🛠");
        icon.getStyleClass().add("help-icon");
        Label title = new Label("If you run into any issues");
        title.getStyleClass().add("help-title");
        header.getChildren().addAll(icon, title);

        // ---- Body (contact lines + copy buttons)
        VBox body = new VBox(8);
        body.getChildren().addAll(
                contactRow("Phone:", "+1 (514) 555-0137"),
                contactRow("Email:", "support@stm.example")
        );

        VBox content = new VBox(14, header, body);
        content.getStyleClass().add("help-content");

        DialogPane pane = alert.getDialogPane();
        pane.setContent(content);

        // Optional: reuse your modal CSS if you have it
        try {
            pane.getStylesheets().add(
                    getClass().getResource("/styles/Modal.css").toExternalForm()
            );
        } catch (Exception ignored) {}
        pane.getStyleClass().add("help-modal");

        alert.getButtonTypes().setAll(new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE));
        Node closeBtn = pane.lookupButton(alert.getButtonTypes().get(0));
        closeBtn.getStyleClass().add("help-close-btn");

        alert.showAndWait();
    }

    // Small helper to render a row with a copy button
    private HBox contactRow(String labelText, String value) {
        Label label = new Label(labelText);
        label.getStyleClass().add("help-label");

        Label val = new Label(value);
        val.getStyleClass().add("help-value");

        Button copy = new Button("Copy");
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
