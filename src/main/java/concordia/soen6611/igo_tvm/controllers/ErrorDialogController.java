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

/**
 * Controller for the localized error dialog.
 * <p>
 * Displays a timestamp, a localized exception type, a user-facing message,
 * and a (developer-oriented) stack trace for an {@link AbstractCustomException}.
 * The dialog text is sourced from {@link I18nService}.
 */
@Controller
@Scope("prototype")
public class ErrorDialogController {

    /** Timestamp value label (formatted with {@link #TS_FMT}). */
    @FXML private Label timestampLabel;

    /** Localized exception type value label. */
    @FXML private Label typeLabel;

    /** Localized, user-friendly error message value label. */
    @FXML private Label userMessageLabel;

    /** Title label for timestamp row. */
    @FXML private Label timeTitleLabel;

    /** Title label for exception type row. */
    @FXML private Label typeTitleLabel;

    /** Title label for user message row. */
    @FXML private Label userMessageTitleLabel;

    /** Multiline stack trace text area (developer-facing). */
    @FXML private TextArea stackTraceArea;

    /** Close/OK button to dismiss the dialog. */
    @FXML private Button closeBtn;

    /** Timestamp format used in {@link #timestampLabel}. */
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Internationalization service for dialog labels, titles, and messages. */
    @Autowired //
    private I18nService i18n;

    /**
     * JavaFX lifecycle hook. Localizes all static labels/buttons.
     */
    @FXML
    private void initialize() {
        updateTexts();
    }

    /**
     * Applies localized strings to dialog UI elements.
     * <p>
     * Uses keys:
     * <ul>
     *   <li>{@code errorDialog.close}</li>
     *   <li>{@code errorDialog.timestampLabel}</li>
     *   <li>{@code errorDialog.typeLabel}</li>
     *   <li>{@code errorDialog.userMessageLabel}</li>
     * </ul>
     */
    private void updateTexts() {
        closeBtn.setText(i18n.get("errorDialog.close"));

        timeTitleLabel.setText(i18n.get("errorDialog.timestampLabel"));
        typeTitleLabel.setText(i18n.get("errorDialog.typeLabel"));
        userMessageTitleLabel.setText(i18n.get("errorDialog.userMessageLabel"));
    }

    /**
     * Populates the dialog with details from an {@link AbstractCustomException}.
     * <p>
     * Sets:
     * <ul>
     *   <li>{@link #timestampLabel} – formatted timestamp</li>
     *   <li>{@link #typeLabel} – localized exception type {@code exceptionType.&lt;type&gt;}</li>
     *   <li>{@link #userMessageLabel} – user-facing message</li>
     *   <li>{@link #stackTraceArea} – exception + (optional) cause + full stack trace</li>
     * </ul>
     *
     * @param ex the custom exception to render; no-ops if {@code null}
     */
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

    /**
     * Sets the stage title using a localized dialog title.
     *
     * @param stage the dialog window; no-ops if {@code null}
     */
    // java
    public void setStage(Stage stage) {
        if (stage == null) return;
        stage.setTitle(i18n.get("errorDialog.title"));
    }

    /**
     * Closes the dialog window when the Close/OK button is pressed.
     */
    @FXML
    private void onClose() {
        Stage st = (Stage) closeBtn.getScene().getWindow();
        st.close();
    }
}
