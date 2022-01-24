import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class Controller implements Initializable{
    @FXML
    private TextArea textArea;
    @FXML
    private TextField textField;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private HBox authPanel;
    @FXML
    private HBox msgPanel;
    @FXML
    private ListView clients;


    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    public Stage regStage;

    private final String ADDRESS = "localhost";
    private final int PORT = 8189;

    private boolean authenticated;
    private String nickname;
    private Stage stage;
    private RegController regController;
    private String[] list;

    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        authPanel.setVisible(!authenticated);
        authPanel.setManaged(!authenticated);
        msgPanel.setVisible(authenticated);
        msgPanel.setManaged(authenticated);
        clients.setVisible(authenticated);

        if (!authenticated) {
            nickname = "";
        }

        setTitle(nickname);
        textArea.clear();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                stage = (Stage) textField.getScene().getWindow();
                stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
                    @Override
                    public void handle(WindowEvent windowEvent) {
                        System.out.println("bye");
                        if (socket != null && !socket.isClosed()) {
                            try {
                                out.writeUTF("/end");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });
        setAuthenticated(false);
    }

    public void connect() {
        try {
            socket = new Socket(ADDRESS, PORT);

            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run()  { try {
                    //цикл аутентификации
                    while (true) {
                        String str = in.readUTF();

                        if (str.startsWith("/")) {
                            if (str.equals("/end")) {
                                break;
                            }
                            if (str.startsWith("/authok")) {
                                nickname = str.split(" ")[1];
                                setAuthenticated(true);
                                break;
                            }
                            if (str.startsWith("/reg")) {
                                regController.regStatus(str);
                            }

                        } else {
                            textArea.appendText(str + "\n");
                        }
                    }


                    //цикл работы
                    while (authenticated) {
                        String str = in.readUTF();

                        if (str.equals("/end")) {
                            setAuthenticated(false);
                            break;
                        }

                        if(str.startsWith("/clients")) {

                            list = str.split(" ");
                            Platform.runLater(new Runnable() {
                                @Override
                                public void run() {
                                    {
                                        clients.getItems().clear();
                                        for (int i = 1; i < list.length; i++) {
                                            clients.getItems().add(list[i]);
                                        }
                                    }
                                }
                            });


                        } else {

                            textArea.appendText(str + "\n");
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void clickBtnSendText(ActionEvent actionEvent) {
        if (textField.getText().length() > 0) {
            try {
                out.writeUTF(textField.getText());
                textField.clear();
                textField.requestFocus();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void clickBtnAuth(ActionEvent actionEvent) {
        if (socket == null || socket.isClosed()) {
            connect();
        }

        try {
            String msg = String.format("/auth %s %s",
                    loginField.getText().trim(), passwordField.getText().trim());
            out.writeUTF(msg);
            passwordField.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setTitle(String nickname) {
        final String title;
        if (nickname.equals("")) {
            title = "Magic chat";
        } else {
            title = String.format("Magic chat - %s", nickname);
        }
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                stage.setTitle(title);
            }
        });
    }

    public void createRegWindow() {
        try {
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/regWindow.fxml"));
            Parent root = (Parent) fxmlLoader.load();
            regStage = new Stage();
            regStage.setTitle("Magic chat registration");
            regStage.setScene(new Scene(root, 500, 425));

            regStage.initModality(Modality.APPLICATION_MODAL);
            regStage.initStyle(StageStyle.UTILITY);

            regController = fxmlLoader.getController();
            regController.setController(this);
            regStage.show();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToReg(String login, String password, String nickname) {
        if (socket == null || socket.isClosed()) {
            connect();
        }
        String msg = String.format("/reg %s %s %s", login, password, nickname);
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clickBtnReg(ActionEvent actionEvent) {
        if(regStage == null) {
            createRegWindow();
        }
        regStage.show();
    }

    public void privateMsg(MouseEvent mouseEvent) {
        String receiver = (String) clients.getSelectionModel().getSelectedItem();
        textField.setText("/w " + receiver + " ");
    }
}