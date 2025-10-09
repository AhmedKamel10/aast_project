package aast.aast_project;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import java.sql.Connection;

public class HelloController {

    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        Connection conn = DatabaseConnection.getConnection();
        if (conn != null) {
            welcomeText.setText("✅ Connected to Oracle!");
        } else {
            welcomeText.setText("❌ Connection failed!");
        }
    }
}
