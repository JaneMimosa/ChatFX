package Server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.util.Vector;

public class AuthService {
    private static Connection connection;
    private static Statement statement;
    private static Vector<ClientHandler> users;
    private static final Logger LOG = LogManager.getLogger(AuthService.class.getName());

    public static void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:main.db");
        } catch (ClassNotFoundException | SQLException e) {
            LOG.fatal("Exception: '{}' couldn't connect to DataBase", e.toString());
            e.printStackTrace();
        }
    }

    public static String getNicknameByLoginAndPassword(String login, String password) {
        String query = String.format("select nickname from users where login='%s' and password='%s'", login, password);
        try {
            statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()) {
                return rs.getString("nickname");
            }
        } catch (SQLException e) {
            LOG.error("Exception: '{}' in method 'getNicknameByLoginAndPassword'", e.toString());
            e.printStackTrace();
        }
        return null;
    }

    public static boolean doesUserExist(String nick) {
        String query = String.format("select login from users where nickname='%s'", nick);
        try {
            statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            LOG.error("Exception: '{}' in method doesUserExist", e.toString());
            e.printStackTrace();
        }
        return false;
    }

    public static boolean checkBlackList(String blocker, String blocked) {
        String query = String.format(
                "SELECT 'Record Exist'\n" +
                        "WHERE EXISTS(SELECT 1 FROM blacklist\n" +
                        "       WHERE blocker = '%s' AND blocked = '%s')", blocker, blocked);
        try {
            ResultSet rs = statement.executeQuery(query);
            if (rs.next()) {
                return true;
            }
        } catch (SQLException e) {
            LOG.error("Exception: '{}' in method 'checkBlackList'", e.toString());
            e.printStackTrace();
        }
        return false;
    }

    public static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int blackListAdd(String blocker, String blocked) {
        if (!checkBlackList(blocker, blocked) && !blocker.equals(blocked)) {
            try {
                String query = "INSERT INTO blacklist (blocker, blocked) VALUES (?, ?);";
                PreparedStatement ps = connection.prepareStatement(query);
                ps.setString(1, blocker);
                ps.setString(2, blocked);
                return ps.executeUpdate();
            } catch (SQLException e) {
                LOG.error("Exception: '{}' in method 'blackListAdd'", e.toString());
                e.printStackTrace();
            }
        }
        return 0;
    }

    public static void blackListRemove(String blocker, String blocked) {
        if (checkBlackList(blocker, blocked)) {
            try {
                String query = String.format("DELETE FROM blacklist WHERE blocker = '%s' AND blocked = '%s'", blocker, blocked);
                PreparedStatement ps = connection.prepareStatement(query);
                ps.executeUpdate();
            } catch (SQLException e) {
                LOG.error("Exception: '{}' in method 'blackListRemove'", e.toString());
                e.printStackTrace();
            }
        }
    }

    public static void changeNick(String nick, String newNick) {
        String query = String.format("UPDATE users SET nickname = '%s' WHERE nickname='%s'", newNick, nick);
        try {
            PreparedStatement ps = connection.prepareStatement(query);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOG.error("Exception: '{}' in method 'changeNick'", e.toString());
            e.printStackTrace();
        }
    }
}
