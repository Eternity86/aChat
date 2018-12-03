package ru.aChat.home.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class Controller {

    @FXML
    VBox vboxMain;
    @FXML
    Label labelUsers;
    @FXML
    Label labelUserCount;
    @FXML
    HBox bottomPanel;
    @FXML
    HBox upperPanel;
    @FXML
    TextField loginField;
    @FXML
    PasswordField passField;
    @FXML
    Button btnLogin;
    @FXML
    ListView<String> userList;
    @FXML
    Button btnSend;
    @FXML
    TextArea textArea;
    @FXML
    TextField textField;

    private Socket socket;

    private DataInputStream in;
    private DataOutputStream out;

    private boolean isAuthorized;

    private final String IP_ADDRESS = "localhost";
    private final int PORT = 8189;

    private void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;

        if(!isAuthorized) {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    upperPanel.setVisible(true);
                    upperPanel.setManaged(true);
                    bottomPanel.setVisible(false);
                    bottomPanel.setManaged(false);
                    labelUserCount.setText("0");
                    userList.getItems().clear();
                }
            });
        } else {
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    upperPanel.setVisible(false);
                    upperPanel.setManaged(false);
                    bottomPanel.setVisible(true);
                    bottomPanel.setManaged(true);
                }
            });
        }
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // цикл для авторизации
                        while (true) {
                            String str = in.readUTF();
                            if(str.startsWith("/authok")) {
                                setAuthorized(true);
                                break;
                            } else {
                                textArea.appendText(String.format("%s%s", str, System.lineSeparator()));
                            }
                        }
                        // цикл для работы
                        while (true) {
                            if(!socket.isClosed()) {
                                String str = in.readUTF();
                                if (str.startsWith("/")) {
                                    if (str.equals("/serverClosed")) {
                                        setAuthorized(false);
                                        break;
                                    }
                                    if(str.startsWith("/clientlist")) {
                                        String[] tokens = str.split(" ");
                                        Platform.runLater(new Runnable() {
                                            @Override
                                            public void run() {
                                                userList.getItems().clear();
                                                labelUserCount.setText(String.valueOf(tokens.length - 1));
                                                userList.getItems().clear();
                                                for (int i = 1; i < tokens.length; i++) {
                                                    userList.getItems().add(tokens[i]);
                                                }
                                            }
                                        });
                                    }
                                } else {
                                    textArea.appendText(String.format("%s%s", str, System.lineSeparator()));
                                }
                            }
                        }
                    } catch (IOException e) {
                        System.out.println(e.getMessage());;
                        //e.printStackTrace();
                    } finally {
                        try {
                            out.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            in.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        setAuthorized(false);
                    }
                }
            }).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void tryToAuth() {
        if(socket == null || socket.isClosed()) {
            connect();
        }
        try {
            out.writeUTF("/auth " + loginField.getText() + " " + passField.getText());
            Platform.runLater(new Runnable() {
                @Override
                public void run() {
                    loginField.clear();
                    passField.clear();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg() {
        if (!textField.getText().equals("")) {
            try {
                out.writeUTF(textField.getText());
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        textField.clear();
                        textField.requestFocus();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnectServer() {
        if(socket != null) {
            try {
                out.writeUTF("/end");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void closeApp() {
        try {
            Platform.exit();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                in.close();
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

    public void showAbout() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("О программе");
        alert.setHeaderText("aChat v0.1");
        alert.setContentText("Чат на JavaFX");
        alert.showAndWait();
    }

}