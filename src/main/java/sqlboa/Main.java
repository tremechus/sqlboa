package sqlboa;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import sqlboa.state.AppState;
import sqlboa.view.SqlTextArea;

public class Main extends Application {

    private MainController controller;
    private SQLBoa boa;
    private AppState appState;

    @Override
    public void start(Stage primaryStage) throws Exception{

        FXMLLoader loader = new FXMLLoader(getClass().getResource("body.fxml"));
        Parent root = loader.load();

        Scene scene = new Scene(root, 1024, 768);

        primaryStage.setTitle("SQLBoa");
        primaryStage.getIcons().add(new Image("/sqlboa/icon/database.png"));
        primaryStage.setScene(scene);

        appState = AppState.restore();
        boa = new SQLBoa(appState, primaryStage);

        SqlTextArea.start();

        controller = loader.getController();
        controller.init(this, boa, appState, primaryStage);

        primaryStage.show();
    }

    @Override
    public void stop() throws Exception {
        super.stop();

        SqlTextArea.stop();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
