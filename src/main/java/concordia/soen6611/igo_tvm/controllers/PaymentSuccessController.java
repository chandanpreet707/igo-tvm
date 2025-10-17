package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.I18nService;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class PaymentSuccessController {

    private final I18nService i18n;
    @FXML private Button printBtn;
    @FXML private Button doneBtn;
    @FXML private Label successTitle;
    @FXML private Label printingLine;
    @FXML private Label printBtnLabel;
    @FXML private Label doneBtnLabel;
    @FXML private Label helpLabel;
    @FXML private Label confirmationLabel;


    private final ApplicationContext appContext;

    public PaymentSuccessController(ApplicationContext appContext, I18nService i18n) {
        this.appContext = appContext;
        this.i18n = i18n;
    }

    @FXML
    private void initialize() {
        updateTexts();
    }

    private void updateTexts() {
        successTitle.setText(i18n.get("paymentSuccess.success"));
        printingLine.setText(i18n.get("paymentSuccess.printing"));
        printBtnLabel.setText(i18n.get("paymentSuccess.printReceipt"));
        doneBtnLabel.setText(i18n.get("paymentSuccess.done"));
        helpLabel.setText(i18n.get("help"));
        confirmationLabel.setText(i18n.get("paymentSuccess.confirmation"));
    }

    /* ===== Actions ===== */

    @FXML
    private void onPrintReceipt(ActionEvent event) {
        // Modal popup
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Receipt");
        alert.setHeaderText(null);
        alert.setContentText("Receipt printed successfully.\n"
                + "Redirection in 5 seconds...");
        // show non-blocking
        alert.show();

        // disable actions while waiting
        setButtonsDisabled(true);

        // after 5s, close modal & go Home
        PauseTransition wait = new PauseTransition(Duration.seconds(5));
        wait.setOnFinished(e -> {
            alert.close();
            goHome(event);
        });
        wait.play();
    }

    @FXML
    private void onDone(ActionEvent event) {
        goHome(event);
    }

    private void setButtonsDisabled(boolean b) {
        if (printBtn != null) printBtn.setDisable(b);
        if (doneBtn  != null) doneBtn.setDisable(b);
    }

    /* ===== Navigation helper ===== */
    private void goHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/Home.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent home = loader.load();
            ((Node) event.getSource()).getScene().setRoot(home);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /* Optional: footer volume/help handlers if you want */
    public void onVolume(ActionEvent e) { /* no-op */ }
}
