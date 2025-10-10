module aast.aast_project {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires java.sql;
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    // --- CRITICAL FIXES ---

    // 1. Open the controller package to javafx.fxml for @FXML field injection (REFLECTION)
    opens aast.aast_project.controller to javafx.fxml;

    // 2. You may need to open the main app package too, if your FXML files are here
    opens aast.aast_project to javafx.fxml;

    // 3. And open the app package if ViewManager or other classes are referenced by FXML
    opens aast.aast_project.app to javafx.fxml;
    opens aast.aast_project.controllers to javafx.fxml;
    // --- EXPORTS ---
    exports aast.aast_project;
    exports aast.aast_project.controller;
    exports aast.aast_project.app;
}