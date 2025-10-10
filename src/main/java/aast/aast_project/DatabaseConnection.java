package aast.aast_project;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement; // <-- This is essential

public class DatabaseConnection {

    private static final String URL = "jdbc:oracle:thin:@localhost:1521/XE";
    private static final String USER = "system";
    private static final String PASSWORD = "1234";

    public static Connection getConnection() {
        Connection conn = null;
        try {
            // 1. Establish the connection
            conn = DriverManager.getConnection(URL, USER, PASSWORD);

            // 2. --- CRITICAL FIX: Set the Default Schema ---
            // This is required to resolve ORA-00942 when SYSTEM is the owner
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER SESSION SET CURRENT_SCHEMA = SYSTEM");
            }
            // ---------------------------------------------

            return conn;
        } catch (SQLException e) {
            e.printStackTrace();
            // Close connection if setup fails
            if (conn != null) {
                try { conn.close(); } catch (SQLException ex) { /* Ignore */ }
            }
            return null;
        }
    }
}