package concordia.soen6611.igo_tvm;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javafx.application.Application;
import javafx.stage.Stage;

/**
 * JavaFX + Spring Boot entry point for the iGo Ticket Vending Machine application.
 * <p>
 * This class bootstraps a headless Spring context (no embedded web server) and
 * wires JavaFX controllers via Spring's {@code ApplicationContext}. The initial
 * view is loaded from {@code /welcome-screen.fxml}.
 * </p>
 *
 * <h3>Lifecycle</h3>
 * <ol>
 *   <li>{@link #init()} – starts the Spring context.</li>
 *   <li>{@link #start(Stage)} – loads the initial FXML, sets up the primary stage, and shows the UI.</li>
 *   <li>{@link #stop()} – gracefully closes the Spring context.</li>
 * </ol>
 */
@SpringBootApplication
public class IGoApplication extends Application {

    /** Spring application context backing controller creation and services. */
    private ConfigurableApplicationContext context;

    /**
     * Initializes the Spring application context before the JavaFX UI starts.
     * <p>
     * Uses {@link SpringApplicationBuilder} with {@link WebApplicationType#NONE}
     * to disable the embedded web server, as this is a desktop JavaFX app.
     * </p>
     */
    @Override
    public void init() {
        context = new SpringApplicationBuilder(IGoApplication.class)
                .web(WebApplicationType.NONE) // disable embedded web server
                .run();
    }

    /**
     * Starts the JavaFX application, loads the welcome screen, and shows the primary stage.
     * <p>
     * Controllers referenced by {@code welcome-screen.fxml} are created via the Spring
     * controller factory to enable dependency injection.
     * </p>
     *
     * @param primaryStage the primary JavaFX stage provided by the runtime
     * @throws Exception if the FXML cannot be loaded
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load FXML with Spring beans
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/welcome-screen.fxml"));
        loader.setControllerFactory(context::getBean); // Let Spring manage controllers

        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 700);

        primaryStage.setTitle("iGo Ticket Vending Machine");
        primaryStage.setScene(scene);
        primaryStage.setWidth(1200);
        primaryStage.setHeight(700);
        primaryStage.show();
    }

    /**
     * Stops the JavaFX application and closes the Spring context to release resources.
     */
    @Override
    public void stop() {
        context.close();
    }

    /**
     * Standard Java entry point that delegates to {@link Application#launch(String...)}.
     *
     * @param args CLI arguments (ignored)
     */
    public static void main(String[] args) {
        launch();
    }
}
