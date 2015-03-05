package sqlboa;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;

import java.io.IOException;

/**
 * Created by trevor on 3/4/2015.
 */
public class ResultPanelController {
    @FXML
    private Label titleLabel;

    @FXML
    private Label detailLabel;

    @FXML
    private BorderPane bodyPane;

    private Object data;
    private OnCloseListener closeListener;

    public void setOnCloseListener(OnCloseListener listener) {
        closeListener = listener;
    }

    public void update(String name, String detail, Node content, Object data) {
        this.data = data;

        if (name == null) {
            name = "";
        }

        if (detail == null) {
            detail = "";
        }

        titleLabel.setText(name);
        detailLabel.setText(detail);
        bodyPane.setCenter(content);
    }

    @FXML
    public void handleCloseResults() {
        if (closeListener != null) {
            closeListener.onClose();
        }
    }

    public interface OnCloseListener {
        public void onClose();
    }
}
