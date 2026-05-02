package utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;



public class DatabaseConnection {
    private static final String URL = "jdbc:postgresql://localhost:32768/postgres";

    public static Connection getConnection(String user, String password) throws SQLException {
        return DriverManager.getConnection(URL, user, password);
    }

    public static boolean isConnectionAlive(Connection conn) {
        try {
            return conn != null && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }
}