package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.ContrastManager;
import concordia.soen6611.igo_tvm.Services.TextZoomService;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@org.springframework.context.annotation.Scope("prototype")
public class CardReloadController {
    public BorderPane root;
    public Label brandLink;
    public Label tapYouCardLabel;
    public Label reloadCardLabel;
    @FXML private Button startReadBtn;
    @FXML private ProgressIndicator readProgress;
    @FXML private Label readStatus;
    private Timeline clock;
    @FXML private Label clockLabel;

    private final ApplicationContext appContext;
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("MMM dd, yyyy\nhh : mm a");

    public CardReloadController(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    @FXML
    private void initialize() {

        // Live clock
        clock = new Timeline(
                new KeyFrame(Duration.ZERO, e -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))),
                new KeyFrame(Duration.seconds(1))
        );
        clock.setCycleCount(Timeline.INDEFINITE);
        clock.play();


        Platform.runLater(() -> {
            var zoom = TextZoomService.get();
            zoom.register(brandLink, reloadCardLabel, clockLabel, tapYouCardLabel, readStatus, reloadCardLabel);
        });

        javafx.application.Platform.runLater(() -> {
            ContrastManager.getInstance().attach(root.getScene(), root);
        });
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
    private void onStartReading(javafx.event.ActionEvent event) {
        // UI: show spinner & status
        startReadBtn.setDisable(true);
        readProgress.setVisible(true);
        readProgress.setManaged(true);
        readStatus.setText("Reading card… / Lecture de la carte…");

        // Simulate 10-second read
        PauseTransition wait = new PauseTransition(Duration.seconds(5));
        wait.setOnFinished(e -> {
            readStatus.setText("Card reading done. / Lecture terminée.");
            readProgress.setVisible(false);
            readProgress.setManaged(false);

            // Inform user
            Alert ok = new Alert(Alert.AlertType.INFORMATION);
            ok.setTitle("Card Read");
            ok.setHeaderText(null);
            ok.setContentText("Card reading done. Proceeding…");
            ok.show();

            // Short pause so they see the dialog, then go to next page
            PauseTransition after = new PauseTransition(Duration.seconds(2));
            after.setOnFinished(x -> {
                ok.close();
                goNext((Node) event.getSource());
            });
            after.play();
        });
        wait.play();
    }

    /** Load your next page after reading completes. */
    private void goNext(Node source) {
        try {
            // TODO: change to your real "next" screen
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/CardReloadAmount.fxml"));
            loader.setControllerFactory(appContext::getBean);
            Parent next = loader.load();
            source.getScene().setRoot(next);
        } catch (Exception ex) {
            ex.printStackTrace();
            // Fallback: just re-enable the button if navigation fails
            startReadBtn.setDisable(false);
        }
    }
}
