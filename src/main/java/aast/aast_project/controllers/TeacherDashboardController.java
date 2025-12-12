package aast.aast_project.controllers;

import aast.aast_project.app.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.text.Text;

public class TeacherDashboardController {

    @FXML private Text welcomeText;

    // Instance variable to hold the instructor's database ID
    private int instructorId;

    // Setter method called by ViewManager after loading
    public void setInstructorId(int id) {
        this.instructorId = id;
        // Future Enhancement: Load instructor name from DB using this ID
        // and set welcomeText.setText("Welcome, Dr. [Name]!");
        welcomeText.setText("Welcome, Instructor ID: " + id); // Temporary display
    }

    @FXML
    public void initialize() {
        // Initialization logic
    }

    // =========================================================================
    // MENU ITEM HANDLERS
    // =========================================================================

    @FXML
    private void handleManageCourses() {
        // Navigates to the Course Manager, passing the logged-in ID
        ViewManager.showCourseManager(this.instructorId);
    }

    /**
     * FIX: Implementation for the 'View Enrollments' menu item.
     * This method is required by TeacherDashboardView.fxml
     */
    @FXML
    private void handleViewEnrollments() {
        // TEMPORARY PLACEHOLDER: Call ViewManager.showEnrollmentView(this.instructorId); once built
        showAlert("Feature Not Implemented", "Opening Enrollment Viewer...", AlertType.INFORMATION);
    }

    /**
     * FIX: Implementation for the 'Grade Submissions' menu item.
     * This method is required by TeacherDashboardView.fxml
     */
    @FXML
    private void handleGradeSubmissions() {
        // TEMPORARY PLACEHOLDER: Call ViewManager.showGradingView(this.instructorId); once built
        showAlert("Feature Not Implemented", "Opening Submission Grading Interface...", AlertType.INFORMATION);
    }

    @FXML
    private void handleLogout() {
        ViewManager.showLogin();
    }

    private void showAlert(String title, String content, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}