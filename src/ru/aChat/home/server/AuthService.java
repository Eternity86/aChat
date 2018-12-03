package ru.aChat.home.server;

import java.sql.*;

class AuthService {
    private static Connection connection;
    private static Statement stmt;

    static void connect() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(org.sqlite.JDBC.PREFIX + "mainDB25102018.db");
            System.out.println("БД подключена!");
            stmt = connection.createStatement();
            System.out.println("Соединение с БД установлено!");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void addUser(String login, String pass, String nick) throws SQLException {
        String sql = String.format("INSERT INTO USERS (login, password, nickname)" +
                "VALUES ('%s','%s','%s')", login, pass.hashCode(), nick);
        stmt.execute(sql);
    }

    public static String getNickByLoginAndPass(String login, String pass) {
        String sql = String.format("SELECT nickname, password FROM USERS" +
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
        }
        return null;
    }

    static void disconnect() {
        try {
            connection.close();
            System.out.println("Соединение с БД закрыто!");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}