package ru.aChat.home.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Vector;
import java.util.logging.Logger;

class MainServer {
    private static final Logger MAIN_SERVER_LOGGER = Logger.getLogger(MainServer.class.getName());
    private static final int SERVER_PORT = 8189;
    private static final int DISCONNECT_TIMEOUT = 10000;

    private Vector<ClientHandler> clients;

    Vector<ClientHandler> getClients() {
        return clients;
    }

    MainServer() {
        ServerSocket server = null;
        Socket socket = null;
        clients = new Vector<>();

        try {
            AuthService.connect();
            server = new ServerSocket(SERVER_PORT);
            MAIN_SERVER_LOGGER.info("Сервер " + server.getLocalSocketAddress() + " запущен! Ожидаем подключения...");

            while (true) {
                socket = server.accept();
                socket.setSoTimeout(DISCONNECT_TIMEOUT); // по прошествии 3-х минут бездействия пользователь отключается от сервера
                new ClientHandler(this, socket);
                MAIN_SERVER_LOGGER.info("Клиент инициировал подключение!");
            }
        } catch (IOException e) {
            e.printStackTrace();
            MAIN_SERVER_LOGGER.warning("Время ожидания вышло!");
        } finally {
            try {
                Objects.requireNonNull(socket).close();
            } catch (IOException ee) {
                ee.printStackTrace();
            }
            try {
                Objects.requireNonNull(server).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            AuthService.disconnect();
        }
    }

    void broadCastMsg(ClientHandler from, String msg) {
        for (ClientHandler o: clients) {
            if(!o.checkBlackList(from.getNick())) {
                o.sendMsg(msg);
            }
        }
    }

    void privateMsg(ClientHandler from, String nickTo, String msg) {
        for (ClientHandler o :
                clients) {
            if (o.getNick().equals(nickTo) && !o.checkBlackList(from.getNick())) {
                o.sendMsg("от " + from.getNick() + ": " + msg);
                from.sendMsg("юзеру " + nickTo + ": " + msg);
                return;
            }
        }
        from.sendMsg("Пользователя " + nickTo + " нет в чате!");
    }

    private void broadcastClientsList() {
        StringBuilder sb = new StringBuilder();
        sb.append("/clientlist ");
        for (ClientHandler o: clients) {
            sb.append(o.getNick()).append(" ");
        }
        for (ClientHandler o: clients) {
            o.sendMsg(sb.toString());
        }
    }

    void subscribe(ClientHandler client) {
        clients.add(client);
        MAIN_SERVER_LOGGER.info(client.getNick() + " добавлен в список пользователей");
        broadcastClientsList();
    }

    void unsubscribe(ClientHandler client) {
        clients.remove(client);
        MAIN_SERVER_LOGGER.info(client.getNick() + " удалён из списка пользователей");
        broadcastClientsList();
    }
}
