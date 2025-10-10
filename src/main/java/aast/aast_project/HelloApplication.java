package aast.aast_project;

import aast.aast_project.app.ViewManager; // Import the new ViewManager
import javafx.application.Application;
import javafx.stage.Stage;

import java.sql.Connection;
// Imports...

public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // 1. Set the primary stage in the ViewManager
        ViewManager.setPrimaryStage(stage);

        // 2. Load the initial view (Login)
        ViewManager.showLogin();

        // stage.setTitle("E-Learning (Prototype)"); // Set inside ViewManager now
        // stage.getIcons().add(...) // Keep icon logic if needed
        stage.show();
    }


    public static void main(String[] args) {

        // --- Connection Test Block ---
        System.out.println("--- Starting Database Connection Test ---");
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (conn != null) {
                System.out.println("✅ CONNECTION SUCCESSFUL!");
                System.out.println("   Database User: " + conn.getMetaData().getUserName());
                System.out.println("---------------------------------------");
            } else {
                System.err.println("❌ CONNECTION FAILED: getConnection returned null.");
                System.err.println("---------------------------------------");
                // Do not exit, continue to launch the GUI for manual checks
            }
        } catch (Exception e) {
            System.err.println("❌ CRITICAL ERROR during test: " + e.getMessage());
            e.printStackTrace();
        }
        // --- End Connection Test Block ---

        launch(args);
    }
}