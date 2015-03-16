package sqlboa.view;

import com.sun.deploy.uitoolkit.impl.fx.HostServicesFactory;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import sqlboa.Configuration;

import java.io.IOException;

/**
 * Created by trevor on 3/6/2015.
 */
public class PopupDialog {

    private Stage parentStage;
    private Stage dialog;
    private Scene dialogScene;
    private String title;

    public PopupDialog(String title, Stage parentStage, String bodyLayout) {
        this.parentStage = parentStage;
        this.title = title;

        try {
            if (!bodyLayout.endsWith(".fxml")) {
                bodyLayout = bodyLayout + ".fxml";
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource(bodyLayout));
            Parent root = loader.load();

            dialogScene = new Scene(root);

            configure(dialogScene);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void configure(Scene scene) {
        // No-op
    }

    public void show() {
        dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(parentStage);
        if (title != null) {
            dialog.setTitle(title);
        }

        dialog.setScene(dialogScene);
        dialog.sizeToScene();

        dialog.show();
    }

    public void close() {
        dialog.close();
    }
}
