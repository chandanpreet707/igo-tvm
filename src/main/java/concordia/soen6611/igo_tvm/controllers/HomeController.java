package concordia.soen6611.igo_tvm.controllers;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
public class HomeController {

    @FXML private Label clockLabel;
    @FXML private Button buyBtn;
    @FXML private Button reloadBtn;

    private Timeline clock;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh:mm a");

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
        buyBtn.setAccessibleText("Buy new ticket / Acheter un nouveau titre");
        reloadBtn.setAccessibleText("Reload card / Recharger une carte");
    }

    // --- Actions (wire to your navigation) ---
    @FXML private void onBuy()    { System.out.println("Buy New Ticket clicked"); }
    @FXML private void onReload() { System.out.println("Reload Card clicked"); }
    @FXML private void onInfo()   { System.out.println("Info clicked"); }
    @FXML private void onVolume() { System.out.println("Volume clicked"); }

    // Call if you navigate away from this view
    public void shutdown() {
        if (clock != null) clock.stop();
    }
}
