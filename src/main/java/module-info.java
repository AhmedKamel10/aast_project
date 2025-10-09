module aast.aast_project {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;

    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;

    // ✅ Add this for JDBC
    requires java.sql;

    opens aast.aast_project to javafx.fxml;
    exports aast.aast_project;
}

