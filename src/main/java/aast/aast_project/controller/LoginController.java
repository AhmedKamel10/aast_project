package aast.aast_project.controller; // Assuming the singular 'controller' package

import aast.aast_project.DatabaseConnection;
import aast.aast_project.app.ViewManager;

import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ChoiceBox;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class LoginController {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Button loginButton;
    @FXML private Button registerButton;
    @FXML private Label messageLabel;
    @FXML private ChoiceBox<String> roleChoiceBox;

    // =========================================================================
    //                            SQL QUERIES
    // =========================================================================

    // FIX 1: UPDATED LOGIN_SQL to retrieve the user's ID (which is the foreign key reference)
    private static final String LOGIN_SQL =
            "SELECT id, role FROM User WHERE username = ? AND password = ?";

    private static final String REGISTER_USER_SQL =
            "INSERT INTO User (username, password, role) VALUES (?, ?, ?)";

    private static final String REGISTER_STUDENT_SQL =
            "INSERT INTO Student (user_id) VALUES (?)";

    private static final String REGISTER_INSTRUCTOR_SQL =
            "INSERT INTO Instructor (user_id) VALUES (?)";

    @FXML
    public void initialize() {
        roleChoiceBox.getItems().addAll("Student", "Instructor");
        roleChoiceBox.setValue("Student");

        loginButton.setOnAction(e -> onLogin());
        registerButton.setOnAction(e -> onRegister());
        messageLabel.setText("Welcome! Please log in or register.");
    }

    // =========================================================================
    //                            LOGIN LOGIC
    // =========================================================================
    private void onLogin() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();

        if (user.isEmpty() || pass.isEmpty()) {
            messageLabel.setText("⚠️ Please enter both username and password.");
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(LOGIN_SQL)) {

            pstmt.setString(1, user);
            pstmt.setString(2, pass);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // FIX 2: Retrieve the ID and the role
                    int userId = rs.getInt("id");
                    String role = rs.getString("role");

                    messageLabel.setText("✅ Login SUCCESSFUL! Role: " + role);

                    // FIX 3: Navigate based on role and PASS THE userId
                    switch (role.toLowerCase()) {
                        case "instructor" -> ViewManager.showTeacherDashboard(userId);
                        case "student" -> ViewManager.showStudentDashboard(userId);
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


    // =========================================================================
    //                            REGISTER LOGIC (NO CHANGES REQUIRED HERE)
    // =========================================================================
    private void onRegister() {
        String user = usernameField.getText().trim();
        String pass = passwordField.getText();
        String role = roleChoiceBox.getValue();

        if (user.isEmpty() || pass.isEmpty() || role == null) {
            messageLabel.setText("⚠️ Please choose a role, username, and password.");
            return;
        }

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            if (conn == null) {
                messageLabel.setText("❌ Cannot connect to database.");
                return;
            }
            conn.setAutoCommit(false);

            // 1. Insert into the base User table and get the new ID
            int newUserId;
            try (PreparedStatement pstmt = conn.prepareStatement(REGISTER_USER_SQL, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, user);
                pstmt.setString(2, pass);
                pstmt.setString(3, role);

                if (pstmt.executeUpdate() == 0) {
                    throw new SQLException("Creating user failed, no rows affected.");
                }

                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        newUserId = rs.getInt(1);
                    } else {
                        throw new SQLException("Creating user failed, no ID obtained.");
                    }
                }
            }

            // 2. Insert into the role-specific table (Student or Instructor)
            String roleSql = role.equals("Student") ? REGISTER_STUDENT_SQL : REGISTER_INSTRUCTOR_SQL;
            try (PreparedStatement rolePstmt = conn.prepareStatement(roleSql)) {
                rolePstmt.setInt(1, newUserId);
                rolePstmt.executeUpdate();
            }

            // 3. Commit the transaction
            conn.commit();
            messageLabel.setText("✅ Registration SUCCESSFUL! Log in now.");

        } catch (SQLException e) {
            // Rollback on failure
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ex) { /* Ignore rollback failure */ }
            }

            // Check for specific error (like duplicate username)
            if (e.getMessage().contains("Duplicate entry")) {
                messageLabel.setText("❌ Username already taken. Try another.");
            } else {
                e.printStackTrace();
                messageLabel.setText("❌ Registration failed. Check console for error.");
            }
        } finally {
            // Restore auto-commit mode and close connection
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    /* Ignore */
                }
            }
        }
    }
}