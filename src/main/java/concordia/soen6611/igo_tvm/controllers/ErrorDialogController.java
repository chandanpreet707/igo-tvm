package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.*;
import concordia.soen6611.igo_tvm.exceptions.AbstractCustomException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;
import java.time.format.DateTimeFormatter;



@Controller
@Scope("prototype")
public class ErrorDialogController {
    @FXML private Label timestampLabel;
    @FXML private Label typeLabel;
    @FXML private Label userMessageLabel;
    @FXML private Label timeTitleLabel;
    @FXML private Label typeTitleLabel;
    @FXML private Label userMessageTitleLabel;
    @FXML private TextArea stackTraceArea;
    @FXML private Button closeBtn;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired //
    private I18nService i18n;




    @FXML
    private void initialize() {
        updateTexts();
    }

    private void updateTexts() {
        closeBtn.setText(i18n.get("errorDialog.close"));

        timeTitleLabel.setText(i18n.get("errorDialog.timestampLabel"));
        typeTitleLabel.setText(i18n.get("errorDialog.typeLabel"));
        userMessageTitleLabel.setText(i18n.get("errorDialog.userMessageLabel"));
    }

    public void setException(AbstractCustomException ex) {
        if (ex == null) return;

        timestampLabel.setText(ex.getTimestamp().format(TS_FMT));
        String translatedType = i18n.get("exceptionType." + ex.getExceptionType());
        typeLabel.setText(translatedType);
        userMessageLabel.setText(ex.getUserMessage());

        StringBuilder sb = new StringBuilder();

        sb.append(i18n.get("errorDialog.exceptionPrefix")).append(" ").append(ex.getClass().getName()).append("\n");

        if (ex.getCause() != null) {
            sb.append(i18n.get("errorDialog.causePrefix")).append(" ")
                    .append(ex.getCause().getClass().getName()).append(": ").append(ex.getCause().getMessage()).append("\n\n");
        }

        for (StackTraceElement ste : ex.getStackTrace()) {
            sb.append("\tat ").append(ste.toString()).append("\n");
        }

        if (stackTraceArea != null) {
            stackTraceArea.setText(sb.toString());
        }
    }




    // java
    public void setStage(Stage stage) {
        if (stage == null) return;
        stage.setTitle(i18n.get("errorDialog.title"));
    }



    @FXML
    private void onClose() {
        Stage st = (Stage) closeBtn.getScene().getWindow();
        st.close();
    }
}
