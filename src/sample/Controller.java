package sample;

import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;

public class Controller {

    final String USERNAME = "Jane";

    @FXML
    TextArea textArea;
    @FXML
    TextField textField;

    @FXML
    void sendMsg() {
        textArea.appendText(USERNAME + ": " + textField.getText() + '\n');
        textField.clear();
        textField.requestFocus();
    }
}
