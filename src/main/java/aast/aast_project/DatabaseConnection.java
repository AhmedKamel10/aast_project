package aast.aast_project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    // Must match the database name: aast_db
    private static final String URL = "jdbc:mysql://localhost:3306/aast_db";

    // The user created in Step 3
    private static final String USER = "aast_user";

    // The password set in Step 3
    private static final String PASSWORD = "1234";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection(URL, USER, PASSWORD);
            // The old Oracle-specific ALTER SESSION is now gone!
            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
            if (conn != null) {
                try { conn.close(); } catch (SQLException ex) { /* Ignore */ }
            }
            return null;
        }
    }
}