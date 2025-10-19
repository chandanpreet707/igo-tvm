package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.exceptions.AbstractCustomException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;

@Controller
@Scope("prototype")
public class ErrorDialogController {
    @FXML private Label timestampLabel;
    @FXML private Label typeLabel;
    @FXML private Label userMessageLabel;
    @FXML private Button closeBtn;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void setException(AbstractCustomException ex) {
        if (ex == null) return;
        timestampLabel.setText(ex.getTimestamp().format(TS_FMT));
        typeLabel.setText(ex.getExceptionType());
        userMessageLabel.setText(ex.getUserMessage());
        StringBuilder sb = new StringBuilder();
        sb.append("Exception: ").append(ex.getClass().getName()).append("\n");
        if (ex.getCause() != null) {
            sb.append("Cause: ").append(ex.getCause().getClass().getName()).append(": ").append(ex.getCause().getMessage()).append("\n\n");
        }
        for (StackTraceElement ste : ex.getStackTrace()) {
            sb.append("\tat ").append(ste.toString()).append("\n");
        }
    }

    @FXML
    private void onClose() {
        Stage st = (Stage) closeBtn.getScene().getWindow();
        st.close();
    }
}
