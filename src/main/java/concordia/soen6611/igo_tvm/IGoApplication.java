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

@SpringBootApplication
public class IGoApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(IGoApplication.class)
                .web(WebApplicationType.NONE) // disable embedded web server
                .run();
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Load FXML with Spring beans
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/Home.fxml"));
        loader.setControllerFactory(context::getBean); // Let Spring manage controllers

        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 700);

        primaryStage.setTitle("iGo Ticket Vending Machine");
        primaryStage.setScene(scene);
        primaryStage.setWidth(1200);
        primaryStage.setHeight(700);
        primaryStage.show();
    }

    @Override
    public void stop() {
        context.close();
    }

    public static void main(String[] args) {
        launch();
    }
}
