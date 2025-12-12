module aast.aast_project {
    // Standard requirements
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires javafx.web;

    // Ensure javafx.base is required, even if it's transitive
    requires javafx.base;

    // =========================================================================
    //                            FIXES & OPENINGS
    // =========================================================================

    // 1. Open packages to javafx.fxml for controller, app, and view reflection
    opens aast.aast_project to javafx.fxml;
    opens aast.aast_project.app to javafx.fxml;

    // Assuming your LoginController is in the singular 'controller' package:
    opens aast.aast_project.controller to javafx.fxml;

    // Assuming your TeacherDashboard/CourseManager controllers are here:
    opens aast.aast_project.controllers to javafx.fxml;

    // 2. THE CRITICAL FIX for PropertyValueFactory (IllegalAccessException)
    // This grants javafx.base access to reflection on your model classes (Course, Student, etc.)
    opens aast.aast_project.model to javafx.base; // <--- THIS SOLVES THE ERROR
    // =========================================================================
    //                             EXPORTS
    // =========================================================================
    // Export all packages that need to be visible outside the module
    exports aast.aast_project;
    exports aast.aast_project.app;
    exports aast.aast_project.controller;
    exports aast.aast_project.controllers;
    // You should also export the model package if other non-JavaFX modules need it
    exports aast.aast_project.model;
}