package aast.aast_project.controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.event.ActionEvent;
import aast.aast_project.app.ViewManager;

public class StudentDashboardController {

    @FXML
    private Button logoutButton, coursesButton, assignmentsButton, gradesButton, profileButton;

    @FXML
    private TextArea infoArea;

    @FXML
    private void initialize() {
        infoArea.setText("Welcome to your dashboard! Select an option from the left.");
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        ViewManager.showLogin();
    }

    @FXML
    private void handleCourses(ActionEvent event) {
        infoArea.setText("📚 Here are your enrolled courses...");
    }

    @FXML
    private void handleAssignments(ActionEvent event) {
        infoArea.setText("📝 These are your current assignments...");
    }

    @FXML
    private void handleGrades(ActionEvent event) {
        infoArea.setText("🎯 Your grades will appear here...");
    }

    @FXML
    private void handleProfile(ActionEvent event) {
        infoArea.setText("👤 Profile info and settings...");
    }
}
