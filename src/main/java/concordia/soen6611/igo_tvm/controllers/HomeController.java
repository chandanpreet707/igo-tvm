package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.I18nService;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class HomeController {

    @FXML private Label brandLabel;
    @FXML private Label homeLabel;
    @FXML private Label promptLabel;
    @FXML private Label helpLabel;
    @FXML private Label clockLabel;
    @FXML private Button buyBtn;
    @FXML private Button reloadBtn;
    @FXML private Button btnEN;
    @FXML private Button btnFR;
    @FXML private Button infoBtn;
    @FXML private Button volumeBtn;
    @FXML private Label buyBtnTitle;
    @FXML private Label buyBtnSub;
    @FXML private Label reloadBtnTitle;
    @FXML private Label reloadBtnSub;
    @FXML private Label informationLabel;

    private Timeline clock;
    private final I18nService i18n;
    private final ApplicationContext appContext;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");

    public HomeController(I18nService i18n, ApplicationContext appContext) {
        this.i18n = i18n;
        this.appContext = appContext;
    }

    @FXML
    private void initialize() {
        // Live clock
        clock = new Timeline(
                new KeyFrame(Duration.ZERO, e ->
                        clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();

        // Accessibility
        buyBtn.setAccessibleText(i18n.get("home.buyBtn.accessible"));
        reloadBtn.setAccessibleText(i18n.get("home.reloadBtn.accessible"));

        updateTexts();
        i18n.localeProperty().addListener((obs, oldL, newL) -> updateTexts());
    }

    @FXML
    private void onBuyTicket(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/BuyNewTicket.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent view = loader.load();
            ((Node) event.getSource()).getScene().setRoot(view);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @FXML
    private void onReload() { /* handle reload */ }
    @FXML
    private void onInfo()   { /* handle info */ }
    @FXML
    private void onVolume() { /* handle volume */ }

    public void shutdown() {
        if (clock != null) clock.stop();
    }

    @FXML
    public void onLanguageChange(ActionEvent event) {
        Object src = event.getSource();
        if (src == btnEN) {
            i18n.setLocale(Locale.ENGLISH);
        } else if (src == btnFR) {
            i18n.setLocale(Locale.FRENCH);
        }
        updateTexts();
    }

    private void updateTexts() {
        brandLabel.setText(i18n.get("home.brand"));
        homeLabel.setText(i18n.get("home.title"));
        promptLabel.setText(i18n.get("home.prompt"));
        helpLabel.setText(i18n.get("home.help"));
        buyBtnTitle.setText(i18n.get("home.buyBtn.title"));
        buyBtnSub.setText(i18n.get("home.buyBtn.sub"));
        reloadBtnTitle.setText(i18n.get("home.reloadBtn.title"));
        reloadBtnSub.setText(i18n.get("home.reloadBtn.sub"));
        informationLabel.setText(i18n.get("home.information"));

        // Tooltips
        btnEN.setTooltip(new Tooltip(i18n.get("home.lang.en")));
        btnFR.setTooltip(new Tooltip(i18n.get("home.lang.fr")));
        infoBtn.setTooltip(new Tooltip(i18n.get("home.info.tooltip")));
        volumeBtn.setTooltip(new Tooltip(i18n.get("home.volume.tooltip")));
    }
}
