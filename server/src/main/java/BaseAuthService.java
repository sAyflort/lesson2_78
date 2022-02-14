import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class BaseAuthService implements AuthService{
    private Connection connection;
    private PreparedStatement prInsert, prUpdate, prSelectAuth, prSelectReg, prSelectChange;
    private static final Logger LOGGER = Logger.getLogger(MyServer.class.getName());

    public BaseAuthService() {
        try {
            connect();
            prepareAllStatements();
            LogManager manager = LogManager.getLogManager();
            manager.readConfiguration(new FileInputStream("server/src/main/resources/logging.properties"));
        } catch (SQLException | IOException throwables) {
            LOGGER.warning(throwables.getMessage());;
        }
    }

    private void prepareAllStatements() throws SQLException {
        prInsert = connection.prepareStatement("INSERT INTO Accounts (login, pass, nick) VALUES (?, ?, ?)");
        prSelectAuth = connection.prepareStatement("SELECT nick FROM Accounts WHERE login = ? AND pass = ?");
        prSelectReg = connection.prepareStatement("SELECT  login, nick FROM Accounts WHERE login = ? OR nick = ?");
        prUpdate = connection.prepareStatement("UPDATE Accounts SET nick = ? WHERE login = ?");
        prSelectChange = connection.prepareStatement("SELECT nick FROM Accounts");
    }

    private void connect() {
        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:database.db");
        } catch (SQLException | ClassNotFoundException throwables) {
            throwables.printStackTrace();
        }
    }

    private void disconnect() {
        if(connection != null) {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }


    @Override
    public void start() {
        LOGGER.info("Сервис аутенфикации запущен");
    }
    @Override
    public void stop() {
        disconnect();
        LOGGER.info("Сервис аутенфикации остановлен");
    }

    @Override
    public boolean changeNick(String login, String newNick) {
        try {
            ResultSet rs = prSelectChange.executeQuery();
            while (rs.next()) {
                if(rs.getString(1)==newNick) {
                    return false;
                }
            }
            prUpdate.setString(1, newNick);
            prUpdate.setString(2, login);
            prUpdate.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public String getNickByLoginPass(String login, String pass) {
        try {
            prSelectAuth.setString(1, login);
            prSelectAuth.setString(2, pass);
            ResultSet rs = prSelectAuth.executeQuery();
            return rs.getString(1);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            return null;
        }
    }

    @Override
    public boolean reg(String login, String pass, String nick) {
        try {
            prSelectReg.setString(1, login);
            prSelectReg.setString(2, nick);
            ResultSet rs = prSelectReg.executeQuery();
            try {
                String check1 = rs.getString(1);
                String check2 = rs.getString(2);
            } catch (SQLException e) {
                prInsert.setString(1, login);
                prInsert.setString(2, pass);
                prInsert.setString(3, nick);
                prInsert.executeUpdate();
                return true;
            }
        } catch (SQLException throwables) {
            LOGGER.warning(throwables.getMessage());
        }

        return false;
    }
}

