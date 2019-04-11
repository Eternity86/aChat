package ru.aChat.home.server;

import java.sql.*;
import java.util.logging.Logger;

class AuthService {
    private static final Logger AUTH_SERVICE_LOGGER = Logger.getLogger(AuthService.class.getName());
    private static final String JDBC_DRIVER = "org.sqlite.JDBC";
    private static Connection connection;
    private static Statement stmt;

    static void connect() {
        try {
            Class.forName(JDBC_DRIVER);
            connection = DriverManager.getConnection(org.sqlite.JDBC.PREFIX + "users.db");
            AUTH_SERVICE_LOGGER.info("Соединение с БД установлено!");
            stmt = connection.createStatement();
            AUTH_SERVICE_LOGGER.info("БД готова для получения SQL-запросов!");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
            AUTH_SERVICE_LOGGER.warning("Ошибка при установке соединения с БД");
        }
    }

    // добавление нового пользователя в БД
    // нужно добавить проверку на существование ника (чтобы избежать двойников)
    public static void addUser(String login, String pass, String nick) {
        String query = "INSERT INTO main (login, password, nickname) VALUES (?, ?, ?)";
        PreparedStatement ps;
        try {
            ps = connection.prepareStatement(query);
            ps.setString(1, login);
            ps.setInt(2, pass.hashCode());
            ps.setString(3, nick);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            AUTH_SERVICE_LOGGER.warning("Ошибка при добавлении нового пользователя");
        }
//        String sql = String.format("INSERT INTO USERS (login, password, nickname)" +
//                "VALUES ('%s','%s','%s')", login, pass.hashCode(), nick);
//        stmt.execute(sql);

    }

    public static void changeNick(String nick, String newNick) {
//        String query = String.format("UPDATE main SET nickname = '%s' WHERE nickname = '%s'", newNick, nick);
//        PreparedStatement ps;
//        try {
//            ps = connection.prepareStatement(query);
//        } catch (SQLException e) {
//            e.printStackTrace();
//        }
    }

    static String getNickByLoginAndPass(String login, String pass) {
        String sql = String.format("SELECT nickname, password FROM main" +
                " WHERE login = '%s'", login);
        try {
            int myHash = pass.hashCode();

            ResultSet rs = stmt.executeQuery(sql);

            if(rs.next()) {
                String nick = rs.getString(1);
                int dbHash = rs.getInt(2);
                if(myHash == dbHash) {
                    return nick;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            AUTH_SERVICE_LOGGER.warning("Ошибка при получении ника пользователя при авторизации");
        }
        return null;
    }

    static void disconnect() {
        try {
            connection.close();
            AUTH_SERVICE_LOGGER.info("Соединение с БД закрыто!");
        } catch (SQLException e) {
            e.printStackTrace();
            AUTH_SERVICE_LOGGER.warning("Ошибка при закрытии соединения с БД");
        }
    }

}