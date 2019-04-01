package ru.aChat.home.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

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

    private final String IP_ADDRESS = "localhost";
    private final int PORT = 8189;

    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private String myNick;

    private boolean isAuthorized;

    private void setAuthorized(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        if(!isAuthorized) {
            Platform.runLater(() -> {
                upperPanel.setVisible(true);
                upperPanel.setManaged(true);
                bottomPanel.setVisible(false);
                bottomPanel.setManaged(false);
                labelUserCount.setText("0");
                userList.getItems().clear();
            });
        } else {
            Platform.runLater(() -> {
                upperPanel.setVisible(false);
                upperPanel.setManaged(false);
                bottomPanel.setVisible(true);
                bottomPanel.setManaged(true);
            });
        }
    }

    private void connect() {
        try {
            socket = new Socket(IP_ADDRESS, PORT);
            in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            new Thread(() -> {
                try {
                    // цикл для авторизации
                    while (true) {
                        String str = in.readUTF();
                        if(str.startsWith("/authok")) {
                            setAuthorized(true);
                            String[] tokens = str.split(" ");
                            myNick = tokens[1];
                            try {
                                BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream("history_" + myNick + ".txt")));
                                String temp;
                                int counter = 0;
                                while ((temp = in.readLine()) != null && counter < 100) {
                                    textArea.appendText(String.format("%s%s", temp, System.lineSeparator()));
                                    counter++;
                                }
                                in.close();
                            } catch (IOException e) {
                                System.out.println("Ошибка при обработке файла");
                            }
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
                                    Platform.runLater(() -> {
                                        userList.getItems().clear();
                                        labelUserCount.setText(String.valueOf(tokens.length - 1));
                                        userList.getItems().clear();
                                        for (int i = 1; i < tokens.length; i++) {
                                            userList.getItems().add(tokens[i]);
                                        }
                                    });
                                }
                            } else {
                                Date date = new Date();
                                SimpleDateFormat formatForDate = new SimpleDateFormat("dd.MM.yyyy hh:mm:ss zz");
                                textArea.appendText(String.format("%s %s%s", formatForDate.format(date), str, System.lineSeparator()));
                                try {
                                    String fileName = "history_" + myNick + ".txt";
                                    BufferedWriter out = new BufferedWriter(new FileWriter(fileName, true));
                                    out.write(formatForDate.format(date) + " " + str + System.lineSeparator());
                                    out.close();
                                } catch (IOException e) {
                                    System.out.println("Ошибка в процессе записи файла");
                                }

                            }
                        }
                    }
                } catch (IOException e) {
                    System.out.println(e.getMessage());;
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
                    setAuthorized(false);
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
            Platform.runLater(() -> {
                loginField.clear();
                passField.clear();
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMsg() {
        if (!textField.getText().equals("")) {
            try {
                out.writeUTF(textField.getText());
                Platform.runLater(() -> {
                    textField.clear();
                    textField.requestFocus();
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