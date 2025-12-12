package aast.aast_project.app;

import aast.aast_project.controllers.CourseManagerController;
import aast.aast_project.controllers.StudentDashboardController; // <--- NEW: Import the Student Controller
import aast.aast_project.controllers.TeacherDashboardController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class ViewManager {

    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    // --- NON-DYNAMIC VIEWS (Login) ---
    public static void showLogin() {
        loadView("LoginView.fxml", "E-Learning Login");
    }

    // --- DYNAMIC VIEWS (Accepts User ID) ---
    public static void showStudentDashboard(int userId) {
        // Assuming your FXML file is named dashboard_student.fxml
        loadView("dashboard_student.fxml", "Student Dashboard", userId);
    }

    public static void showTeacherDashboard(int userId) {
        loadView("InstructorDashboardView.fxml", "Instructor Dashboard", userId);
    }

    public static void showCourseManager(int instructorId) {
        loadView("CourseManagerView.fxml", "Manage Courses", instructorId);
    }
    // ----------------------------------------

    /**
     * Overload to load a view without passing a user ID (e.g., for Login).
     */
    private static void loadView(String fxmlFileName, String title) {
        loadView(fxmlFileName, title, 0); // Pass a default ID (0)
    }

    /**
     * Core method to load a view, pass the user ID, and set the stage.
     */
    private static void loadView(String fxmlFileName, String title, int userId) {
        try {
            String resourcePath = "/aast/aast_project/" + fxmlFileName;
            FXMLLoader loader = new FXMLLoader(ViewManager.class.getResource(resourcePath));

            Parent root = loader.load();

            // --- INJECT THE USER ID INTO THE CONTROLLER ---
            Object controller = loader.getController();

            if (controller instanceof StudentDashboardController studentController) { // <--- FIXED: Student check added first
                studentController.setStudentId(userId);
            }
            else if (controller instanceof TeacherDashboardController teacherController) {
                teacherController.setInstructorId(userId);
            }
            else if (controller instanceof CourseManagerController courseManagerController) {
                courseManagerController.setInstructorId(userId);
            }
            // ------------------------------------------------

            Scene scene = new Scene(root);

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Error: FXML file not found at path: /aast/aast_project/" + fxmlFileName);
            e.printStackTrace();
        }
    }
}