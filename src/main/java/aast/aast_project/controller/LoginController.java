package aast.aast_project.controller;

import aast.aast_project.DatabaseConnection;
import aast.aast_project.app.ViewManager; // Assuming you're using the ViewManager

import javafx.fxml.FXML;
import javafx.scene.control.TextField;    // <--- YOU NEED THIS ONE
import javafx.scene.control.PasswordField; // <--- YOU LIKELY NEED THIS ONE TOO
import javafx.scene.control.Button;       // <--- YOU LIKELY NEED THIS ONE TOO
import javafx.scene.control.Label;        // <--- YOU LIKELY NEED THIS ONE TOO
import java.sql.PreparedStatement; // <-- Add this line
import java.sql.ResultSet;         // <-- You likely need this one too
import java.sql.Connection;
import java.sql.SQLException;
// ... rest of your imports

// Imports... (omitted for brevity)

public class LoginController {
    // FXML fields... (omitted for brevity)
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label messageLabel;

    // NOTE: Ensure 'YOUR_SCHEMA_OWNER' is replaced with the correct value (e.g., SYSTEM)
    private static final String TABLE_NAME = "SYSTEM.USER_ACCOUNTS";

    @FXML
    public void initialize() {
        loginButton.setOnAction(e -> onLogin());
        registerButton.setOnAction(e -> messageLabel.setText("Registration not yet implemented."));
        messageLabel.setText("Welcome! Please log in.");
    }
    private void onLogin() {
        // ... (User input gathering and validation omitted for simplicity)
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            messageLabel.setText("⚠️ Please enter both username and password.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) {
                messageLabel.setText("❌ Cannot connect to database.");
                return;
            }

            // --- QUICK TEST SQL ---
            // This query uses the hardcoded values that worked in SQL Developer.
            // We use user input to construct the string, but it's not secure.
            String testSql =
                    "SELECT role FROM SYSTEM.USER_ACCOUNTS " +
                            "WHERE username = '" + user + "' AND password = '" + pass + "'";

            // ----------------------

            // 1. Use a simple Statement object for direct execution
            try (java.sql.Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(testSql)) {

                if (rs.next()) {
                    String role = rs.getString("role");


                    // If successful, navigate!
                    messageLabel.setText("✅ Login SUCCESSFUL! Role: " + role);

                    // You can add your ViewManager calls here if the test succeeds:

                switch (role.toLowerCase()) {
                    case "teacher" -> ViewManager.showTeacherDashboard();
                    case "student" -> ViewManager.showStudentDashboard();
                    default -> messageLabel.setText("✅ Unknown role.");
                }

                } else {
                    messageLabel.setText("❌ Invalid username or password.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            messageLabel.setText("❌ Database test failed. Check console for error.");
        }
    }
}