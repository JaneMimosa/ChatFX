package FXMLApp;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable {

    @FXML
    TextArea chatArea;
    @FXML
    TextField textField;
    @FXML
    HBox upperPanel;
    @FXML
    HBox bottomPanel;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passwordField;
    @FXML
    ListView<String> clientList;

    Socket socket;
    DataInputStream in;
    DataOutputStream out;
    public static final String ADDRESS = "localhost";
    public static final int PORT = 6000;

    private boolean isAuthorized;

    private List<TextArea> textAreas;
    private static final Logger LOG = LogManager.getLogger(Controller.class.getName());

    public void setAuthorized(boolean authorized) {
        this.isAuthorized = authorized;

        if (!isAuthorized) {
            upperPanel.setVisible(true);
            upperPanel.setManaged(true);

            bottomPanel.setVisible(false);
            bottomPanel.setManaged(false);

            clientList.setVisible(false);
            clientList.setManaged(false);

        } else {
            upperPanel.setVisible(false);
            upperPanel.setManaged(false);

            bottomPanel.setVisible(true);
            bottomPanel.setManaged(true);

            clientList.setVisible(true);
            clientList.setManaged(true);
        }
    }

    @FXML
    void sendMsg() {
        try {
            out.writeUTF(textField.getText());
            textField.clear();
            textField.requestFocus();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void connect() {
        try {
            socket = new Socket(ADDRESS, PORT);

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            setAuthorized(false);

            new Thread(() -> {
                try {
                    while (true) {
                            String str = in.readUTF();
                            if ("/auth-OK".equals(str)) {
                                setAuthorized(true);
                                chatArea.clear();
                                break;
                            } else {
                                for (TextArea ta : textAreas) {
                                    ta.appendText(str + "\n");
                                }
                            }
                    }
                    while (true) {
                        String str = in.readUTF();
                        if ("/serverClosed".equals(str)) {
                            break;
                        }
                        if ("/clientlist".equals(str)) {
                            String[] tokens = str.split(" ");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    clientList.getItems().clear();
                                    for (int i = 1; i < tokens.length; i++) {
                                        clientList.getItems().add(tokens[i]);
                                    }
                                }
                            });
                        } else {
                            chatArea.appendText(str + "\n");
                        }
                    }
                } catch (IOException e) {
                    LOG.error("Exception: '{}' in method connect while running thread", e.toString());
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    setAuthorized(false);
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
            chatArea.appendText("Connection denied");
            LOG.error("Exception: '{}' in method connect", e.toString());
        }
    }

    public void tryToAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passwordField.getText());
            loginField.clear();
            passwordField.clear();
        } catch (IOException e) {
            LOG.error("Exception: '{}' in method tryToAuth",e.toString());
            e.printStackTrace();
        }
    }

    public void disconnect() {
        if (socket != null) {
            if (!socket.isClosed()) {
                try {
                    out.writeUTF("/end");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
    @Override
    public void initialize(URL location, ResourceBundle bundle) {
        setAuthorized(false);
        textAreas = new ArrayList<>();
        textAreas.add(chatArea);
    }
}
