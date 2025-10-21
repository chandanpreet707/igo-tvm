package concordia.soen6611.igo_tvm.controllers;

import concordia.soen6611.igo_tvm.Services.I18nService;
import concordia.soen6611.igo_tvm.exceptions.AbstractCustomException;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Controller
@Scope("prototype")
public class ErrorDialogController {

    // Static UI text targets
    @FXML private Label titleLabel;
    @FXML private Button closeBtn;
    @FXML private Label timeKeyLabel;
    @FXML private Label typeKeyLabel;
    @FXML private Label userMsgKeyLabel;

    // Dynamic values
    @FXML private Label timestampLabel;
    @FXML private Label typeLabel;
    @FXML private Label userMessageLabel;

    @Autowired
    private I18nService i18n;

    private DateTimeFormatter tsFmt;

    @FXML
    private void initialize() {
        applyLocale();                                   // set all texts for current locale
        i18n.localeProperty().addListener((o, oldL, newL) -> applyLocale());
    }

    private void applyLocale() {
        // Keys come from messages_{en,fr}.properties (see section 3)
        titleLabel.setText(i18n.get("errorDialog.dTitle"));
        closeBtn.setText(i18n.get("common.close"));

        timeKeyLabel.setText(i18n.get("errorDialog.field.time"));
        typeKeyLabel.setText(i18n.get("errorDialog.field.type"));
        userMsgKeyLabel.setText(i18n.get("errorDialog.field.userMessage"));

        // Localized timestamp format
        Locale loc = i18n.getLocale();
        // Example pattern; adjust if you prefer 24h/12h differently by locale
        String pattern = loc.getLanguage().equals("fr") ? "yyyy-MM-dd HH:mm:ss" : "yyyy-MM-dd HH:mm:ss";
        tsFmt = DateTimeFormatter.ofPattern(pattern, loc);

        // If content already loaded via setException, re-render the time label with new locale
        if (timestampLabel.getUserData() instanceof AbstractCustomException ex) {
            timestampLabel.setText(ex.getTimestamp().format(tsFmt));
        }

        // Also try to update the window title if available (ExceptionDialog sets it too)
        try {
            Stage st = (Stage) closeBtn.getScene().getWindow();
            if (st != null) st.setTitle(i18n.get("errorDialog.dTitle"));
        } catch (Exception ignored) {}
    }

    public void setException(AbstractCustomException ex) {
        if (ex == null) return;
        // Store ex on the node so we can re-render time on locale switch
        timestampLabel.setUserData(ex);

        if (tsFmt == null) applyLocale(); // ensure tsFmt is ready
        timestampLabel.setText(ex.getTimestamp().format(tsFmt));
        typeLabel.setText(ex.getExceptionType());
        userMessageLabel.setText(ex.getUserMessage());
        // (Stack trace is deliberately not shown in this compact dialog)
    }

    @FXML
    private void onClose() {
        Stage st = (Stage) closeBtn.getScene().getWindow();
        st.close();
    }
}
