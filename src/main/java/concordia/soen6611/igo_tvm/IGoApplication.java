package concordia.soen6611.igo_tvm;

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
        context = new SpringApplicationBuilder(IGoApplication.class).run();
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("iGo Ticket Vending Machine");
        primaryStage.show();
    }

    @Override
    public void stop() {
        context.close();
    }

    public static void main(String[] args) {
        Application.launch(args);
    }
}
