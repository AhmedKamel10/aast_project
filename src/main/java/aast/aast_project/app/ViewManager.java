package aast.aast_project.app;

import aast.aast_project.HelloApplication;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;

public class ViewManager {
    private static Stage primaryStage;

    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    private static void loadAndShowScene(String fxmlPath, String title) {
        try {
            // Path relative to the resources folder (e.g., /aast/aast_project/views/DashboardView.fxml)
            URL fxmlUrl = ViewManager.class.getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("Error: FXML file not found at path: " + fxmlPath);
                return;
            }

            FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
            Scene scene = new Scene(fxmlLoader.load(), 800, 600); // Standard dashboard size

            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load FXML: " + fxmlPath);
        }
    }

    // --- Public Navigation Methods ---

    public static void showLogin() {
        // Assuming LoginView.fxml is in /aast/aast_project/LoginView.fxml (as per your structure)
        loadAndShowScene("/aast/aast_project/LoginView.fxml", "E-Learning Login");
    }

    public static void showTeacherDashboard() {
        // NOTE: You must create this FXML file later (e.g., TeacherDashboardView.fxml)
        loadAndShowScene("/aast/aast_project/TeacherDashboardView.fxml", "Teacher Dashboard");
    }

    public static void showStudentDashboard() {
        // NOTE: You must create this FXML file later (e.g., StudentDashboardView.fxml)
        loadAndShowScene("/aast/aast_project/dashboard_student.fxml", "Student Dashboard");
    }

    // You can add more navigation methods here (e.g., showAdminView)
}