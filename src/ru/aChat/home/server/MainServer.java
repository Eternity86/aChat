package ru.aChat.home.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Objects;
import java.util.Vector;

class MainServer {

    private static final int SERVER_PORT = 8189;

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
            System.out.printf("Сервер %s запущен! Ожидаем подключения...\n", String.valueOf(server.getLocalSocketAddress()));

            while (true) {
                socket = server.accept();
                socket.setSoTimeout(180000); // по прошествии 3-х минут бездействия пользователь отключается от сервера
                new ClientHandler(this, socket);
                System.out.println("Клиент инициировал подключение!");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Время ожидания вышло!");
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
        System.out.println(client.getNick() + " добавлен в список пользователей");
        broadcastClientsList();
    }

    void unsubscribe(ClientHandler client) {
        clients.remove(client);
        System.out.println(client.getNick() + " удалён из списка пользователей");
        broadcastClientsList();
    }

}