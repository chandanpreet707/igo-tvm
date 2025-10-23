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

/**
 * Utility class for showing localized error dialogs.
 * <p>
 * Primary responsibilities:
 * <ul>
 *   <li>Render a modal error dialog (via {@link ErrorDialogController}) for {@link AbstractCustomException}.</li>
 *   <li>Safely fall back to a basic {@link Alert} if the FXML UI cannot be loaded.</li>
 *   <li>Wrap arbitrary {@link Throwable}s into a user-facing {@link AbstractCustomException} when needed.</li>
 *   <li>Leverage {@link I18nService} (if available) for all user-visible strings.</li>
 * </ul>
 * This class is final and non-instantiable.
 */
public final class ExceptionDialog {
    private ExceptionDialog() {}

    /**
     * Attempts to resolve the {@link I18nService} from the Spring {@link ApplicationContext}.
     * Returns {@code null} if the bean is not available.
     *
     * @param ctx Spring application context
     * @return an {@link I18nService} instance or {@code null}
     */
    // --- Small helper to get i18n safely from Spring (fallbacks to null)
    private static I18nService i18n(ApplicationContext ctx) {
        try { return ctx.getBean(I18nService.class); }
        catch (Throwable ignore) { return null; }
    }

    /**
     * Safely retrieves a localized message using {@link I18nService}, with a fallback
     * default string if the service is unavailable.
     *
     * @param i18n      i18n service (nullable)
     * @param code      message key
     * @param fallback  default string if i18n is {@code null}
     * @param args      optional message format arguments
     * @return resolved message string
     */
    // Shorthand to resolve a message or fallback to a default
    private static String msg(I18nService i18n, String code, String fallback, Object... args) {
        return i18n == null ? fallback : i18n.get(code, args);
    }

    /**
     * Shows a modal dialog for an {@link AbstractCustomException}.
     * <p>
     * Behavior:
     * <ol>
     *   <li>Loads {@code /fxml/ErrorDialog.fxml} and delegates UI rendering to {@link ErrorDialogController}.</li>
     *   <li>Localizes titles/labels using {@link I18nService}, if present in the context.</li>
     *   <li>If loading fails, shows a minimal fallback {@link Alert} with localized header/body.</li>
     * </ol>
     *
     * @param ex          the custom exception to display; no-op if {@code null}
     * @param owner       the owner window for modality (nullable)
     * @param appContext  Spring application context used as controller factory and for i18n
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
     * Wraps a generic {@link Throwable} into an {@link AbstractCustomException} (if needed) and displays it.
     * <p>
     * Behavior:
     * <ol>
     *   <li>If {@code t} already extends {@link AbstractCustomException}, delegates to {@link #show(AbstractCustomException, Window, ApplicationContext)}.</li>
     *   <li>Otherwise, constructs a localized user-facing message and reflectively instantiates
     *       {@code concordia.soen6611.igo_tvm.exceptions.UserException} with the cause.</li>
     *   <li>If wrapping or UI loading fails, falls back to a concise, localized {@link Alert}
     *       that includes a shortened stack trace.</li>
     * </ol>
     *
     * @param t           the throwable to display; no-op if {@code null}
     * @param owner       the owner window for modality (nullable)
     * @param appContext  Spring application context used for i18n and controller factory
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
