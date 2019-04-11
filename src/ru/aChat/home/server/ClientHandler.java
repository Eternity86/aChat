package ru.aChat.home.server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

class ClientHandler {
    private static final Logger CLIENT_LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private MainServer server;
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private String nick;

    private List<String> blackList;

    String getNick() {
        return nick;
    }

    boolean checkBlackList(String nick) {
        return blackList.contains(nick);
    }

    private boolean isNickBusy(Vector<ClientHandler> vch, String nick) {
        for (ClientHandler ch : vch) {
            if (ch.getNick().equals(nick)) {
                return true;
            }
        }
        return false;
    }

    ClientHandler(MainServer server, Socket socket) {
        try {
            this.server = server;
            this.socket = socket;
            this.in = new DataInputStream(socket.getInputStream());
            this.out = new DataOutputStream(socket.getOutputStream());
            this.blackList = new Vector<>();

            ExecutorService executorService = Executors.newFixedThreadPool(6);
            executorService.execute(() -> {
                try {
                    // цикл для авторизации
                    while (true) {
                        String str = null;
                        if (!socket.isClosed()) {
                            str = in.readUTF();
                        }
                        if (Objects.requireNonNull(str).startsWith("/auth")) {
                            String[] tokens = str.split(" ");
                            String newNick = AuthService.getNickByLoginAndPass(tokens[1], tokens[2]);
                            if (newNick != null) {
                                if (isNickBusy(server.getClients(), newNick)) {
                                    sendMsg("Учётная запись уже используется!");
                                } else {
                                    nick = newNick;
                                    sendMsg("/authok " + nick);
                                    server.subscribe(ClientHandler.this);
                                    server.broadCastMsg(ClientHandler.this, nick + " зашёл в чат");
                                    break;
                                }
                            } else {
                                sendMsg("Неверный логин/пароль!");
                            }
                        }
                    }

                    // цикл для работы
                    while (true) {
                        String str = null;
                        if (!socket.isClosed()) {
                            str = in.readUTF();
                        }
                        // блок служебных команд
                        if (Objects.requireNonNull(str).startsWith("/")) {
                            if (str.equals("/end")) {
                                out.writeUTF("/serverClosed");
                                CLIENT_LOGGER.info("Клиент закрыл соединение с сервером");
                                break;
                            }
                            if (str.startsWith("/w ")) {
                                String[] tokens = str.split("\\s", 3);
                                server.privateMsg(this, tokens[1], tokens[2]);
                            }
                            if (str.startsWith("/blacklist ")) {
                                String[] tokens = str.split("\\s");
                                if (checkBlackList(tokens[1])) {
                                    sendMsg("Пользователь " + tokens[1] + " уже в чёрном списке!");
                                } else {
                                    blackList.add(tokens[1]);
                                    sendMsg("Вы добавили пользователя " + tokens[1] + " в черный список!");
                                }
                            }
                        } else {
                            server.broadCastMsg(ClientHandler.this, nick + ": " + str);
                            CLIENT_LOGGER.info(nick + ": " + str);
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    server.unsubscribe(ClientHandler.this);
                    server.broadCastMsg(ClientHandler.this, nick + " покинул чат");
                    CLIENT_LOGGER.info(nick + " покинул чат");
                }
            });
            executorService.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void sendMsg(String msg) {
        try {
            out.writeUTF(msg);
        } catch (IOException e) {
            e.printStackTrace();
            CLIENT_LOGGER.warning("Ошибка при отправке сообщения пользователю(ям)");
        }
    }

}
