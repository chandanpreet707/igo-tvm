package concordia.soen6611.igo_tvm.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class PaymentSuccessController {

    @FXML
    private Button printBtn;
    @FXML
    private Button doneBtn;
    @FXML
    private Label clockLabel;
    private Timeline clock;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a");

    private final ApplicationContext appContext;

    public PaymentSuccessController(ApplicationContext appContext) {
        this.appContext = appContext;
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
        if (doneBtn != null) doneBtn.setDisable(b);
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
    }

    public void onBack(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/Payment.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent home = loader.load();
            ((Node) event.getSource()).getScene().setRoot(home);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
