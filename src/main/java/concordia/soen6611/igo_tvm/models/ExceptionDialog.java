package concordia.soen6611.igo_tvm.models;

import concordia.soen6611.igo_tvm.controllers.ErrorDialogController;
import concordia.soen6611.igo_tvm.exceptions.AbstractCustomException;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.springframework.context.ApplicationContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;

public final class ExceptionDialog {
    private ExceptionDialog() {}

    /**
     * Show modal dialog for an AbstractCustomException. Pass appContext for controller factory.
     * This method prints the stacktrace and shows a simple Alert if the FXML UI fails to load.
     */
    public static void show(AbstractCustomException ex, Window owner, ApplicationContext appContext) {
        if (ex == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(Objects.requireNonNull(ExceptionDialog.class.getResource("/fxml/ErrorDialog.fxml")));
            loader.setControllerFactory(appContext::getBean);
            Parent root = loader.load();
            ErrorDialogController ctrl = loader.getController();
            ctrl.setException(ex);

            Stage stage = new Stage();
            stage.initOwner(owner);
            stage.initModality(Modality.WINDOW_MODAL);
            stage.setTitle("Error");
            stage.setScene(new Scene(root));
            stage.showAndWait();
        } catch (Exception e) {
            // Catch all exceptions (including IllegalArgumentException from FXML coercion)
            e.printStackTrace();

            // show a concise user-facing alert on the JavaFX thread
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                if (owner != null) {
                    try { alert.initOwner(owner); } catch (Exception ignored) {}
                }
                alert.initModality(Modality.WINDOW_MODAL);
                alert.setTitle("Error");
                alert.setHeaderText("Unable to display error dialog");
                String msg = e.getMessage() != null ? e.getMessage() : "Failed to load error dialog UI.";
                // include short message; stacktrace is already on console
                alert.setContentText(msg);
                alert.showAndWait();
            });
        }
    }

    /**
     * Wrap a generic Throwable into an AbstractCustomException (if needed) and show.
     */
    public static void showAsUserException(Throwable t, Window owner, ApplicationContext appContext) {
        if (t == null) return;

        if (t instanceof AbstractCustomException) {
            show((AbstractCustomException) t, owner, appContext);
            return;
        }

        // Fallback: create a simple wrapper message and show using existing UI path if available.
        try {
            Class<?> userExClass = Class.forName("concordia.soen6611.igo_tvm.exceptions.UserException");
            AbstractCustomException userEx = (AbstractCustomException) userExClass
                    .getConstructor(String.class, Throwable.class)
                    .newInstance("An unexpected error occurred. Please contact support.", t);
            show(userEx, owner, appContext);
        } catch (Exception e) {
            // If wrapping or UI show fails, print stack traces and show minimal alert to user
            e.printStackTrace();
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            String trace = sw.toString();
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                if (owner != null) {
                    try { alert.initOwner(owner); } catch (Exception ignored) {}
                }
                alert.initModality(Modality.WINDOW_MODAL);
                alert.setTitle("Error");
                alert.setHeaderText("An unexpected error occurred");
                alert.setContentText(trace.length() > 800 ? trace.substring(0, 800) + "..." : trace);
                alert.showAndWait();
            });
        }
    }
}
