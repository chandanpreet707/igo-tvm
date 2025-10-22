package concordia.soen6611.igo_tvm.models;

import concordia.soen6611.igo_tvm.controllers.ErrorDialogController;
import concordia.soen6611.igo_tvm.exceptions.AbstractCustomException;
import concordia.soen6611.igo_tvm.Services.I18nService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.context.ApplicationContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

public final class ExceptionDialog {
    private ExceptionDialog() {}

    // --- Small helper to get i18n safely from Spring (fallbacks to null)
    private static I18nService i18n(ApplicationContext ctx) {
        try { return ctx.getBean(I18nService.class); }
        catch (Throwable ignore) { return null; }
    }

    // Shorthand to resolve a message or fallback to a default
    private static String msg(I18nService i18n, String code, String fallback, Object... args) {
        return i18n == null ? fallback : i18n.get(code, args);
    }

    /**
     * Show modal dialog for an AbstractCustomException. Localized with I18nService if available.
     */
    public static void show(AbstractCustomException ex, Window owner, ApplicationContext appContext) {
        if (ex == null) return;

        final I18nService i18n = i18n(appContext);
        final String titleError = msg(i18n, "errorDialog.title", "Error");

        try {
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(ExceptionDialog.class.getResource("/fxml/ErrorDialog.fxml"))
            );
            loader.setControllerFactory(appContext::getBean);
            Parent root = loader.load();

            // Let controller render localized content; we just supply exception.
            ErrorDialogController ctrl = loader.getController();
            ctrl.setException(ex);

            Stage stage = new Stage();
            if (owner != null) stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle(titleError);
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
                if (owner != null) {
                    try { alert.initOwner(owner); } catch (Exception ignored) {}
                }
                alert.initModality(Modality.WINDOW_MODAL);
                alert.setTitle(titleError);
                alert.setHeaderText(msg(i18n, "errorDialog.fallback.header", "Unable to display error dialog"));
                String msgBody = e.getMessage() != null ? e.getMessage()
                        : msg(i18n, "errorDialog.fallback.body", "Failed to load error dialog UI.");
                alert.setContentText(msgBody);
                alert.showAndWait();
            });
        }
    }

    /**
     * Wrap a generic Throwable into an AbstractCustomException (if needed) and show.
     * Localized strings used for wrapper message and fallback alert.
     */
    public static void showAsUserException(Throwable t, Window owner, ApplicationContext appContext) {
        if (t == null) return;

        final I18nService i18n = i18n(appContext);
        final String titleError = msg(i18n, "errorDialog.title", "Error");

        if (t instanceof AbstractCustomException) {
            show((AbstractCustomException) t, owner, appContext);
            return;
        }

        try {
            // Localized user-facing message inside the wrapper exception:
            String userMsg = msg(i18n,
                    "errorDialog.unexpected.wrapper",
                    "An unexpected error occurred. Please contact support.");

            Class<?> userExClass = Class.forName("concordia.soen6611.igo_tvm.exceptions.UserException");
            AbstractCustomException userEx = (AbstractCustomException) userExClass
                    .getConstructor(String.class, Throwable.class)
                    .newInstance(userMsg, t);

            show(userEx, owner, appContext);
        } catch (Exception e) {
            // If wrapping/UI fails, show a minimal (localized) Alert with a short stack trace
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();

            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR, "", ButtonType.OK);
                if (owner != null) {
                    try { alert.initOwner(owner); } catch (Exception ignored) {}
                }
                alert.initModality(Modality.WINDOW_MODAL);
                alert.setTitle(titleError);
                alert.setHeaderText(msg(i18n, "errorDialog.unexpected.header", "An unexpected error occurred"));
                // Keep content concise, localized prefix + snippet of stack trace
                String prefix = msg(i18n, "errorDialog.unexpected.body", "Details (technical):");
                String body = prefix + "\n" + (trace.length() > 800 ? trace.substring(0, 800) + "..." : trace);
                alert.setContentText(body);
                alert.showAndWait();
            });
        }
    }
}
