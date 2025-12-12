package aast.aast_project.controllers;

import aast.aast_project.DatabaseConnection;
import aast.aast_project.app.ViewManager;
import aast.aast_project.model.Course; // Assuming Course model exists
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.control.Separator;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TeacherDashboardController {

    // ===============================================
    // FXML FIELDS
    // ===============================================
    @FXML private Text welcomeText;
    @FXML private VBox centerVBox;  // CRITICAL: Linked to fx:id="centerVBox" in FXML

    // ===============================================
    // STATE
    // ===============================================
    private int instructorId;

    // ===============================================
    // DTO for Grading View
    // ===============================================
    public static class SubmissionForGrading {
        int submissionId;
        int studentId; // NEW FIELD: Store the actual ID
        String studentDisplay; // Field for displaying "Student ID: X"
        String assignmentTitle;
        LocalDateTime submissionDate;
        String filePath;

        public SubmissionForGrading(int submissionId, int studentId, String assignmentTitle, LocalDateTime submissionDate, String filePath) {
            this.submissionId = submissionId;
            this.studentId = studentId;
            this.assignmentTitle = assignmentTitle;
            this.submissionDate = submissionDate;
            this.filePath = filePath;
            // Set the display name to the ID since the actual name columns are missing
            this.studentDisplay = "Student ID: " + studentId;
        }

        public int getSubmissionId() { return submissionId; }
        public String getStudentName() { return studentDisplay; } // Used by the UI
        public String getAssignmentTitle() { return assignmentTitle; }
        public LocalDateTime getSubmissionDate() { return submissionDate; }
        public String getFilePath() { return filePath; }
    }

    // ===============================================
    // SQL QUERIES
    // ===============================================
    private static final String SELECT_INSTRUCTOR_COURSES =
            "SELECT id, string_courseName FROM Course WHERE instructor_id = ?";

    private static final String SELECT_UNGRADED_SUBMISSIONS =
            // FIX: Removed U.string_firstName and U.string_lastName, replaced with S.student_id
            "SELECT S.id AS submissionId, S.string_filePath, S.datetime_submissionDate, " +
                    "A.string_title AS assignmentTitle, S.student_id " +
                    "FROM Submission S " +
                    "JOIN Assignment A ON S.assignment_id = A.id " +
                    "JOIN Lesson L ON A.lesson_id = L.id " +
                    "JOIN User U ON S.student_id = U.id " + // U table is still needed for the JOIN constraint
                    "WHERE L.course_id = ? AND S.float_score IS NULL";

    private static final String UPDATE_SUBMISSION_GRADE =
            "UPDATE Submission SET float_score = ?, string_feedback = ? WHERE id = ?";


    // =========================================================================
    // INITIALIZATION AND CONTEXT
    // =========================================================================

    public void setInstructorId(int id) {
        this.instructorId = id;
        System.out.println("LOG: Instructor ID set to: " + this.instructorId);
        if (welcomeText != null) {
            welcomeText.setText("Welcome, Instructor ID: " + id);
        }
    }

    @FXML
    public void initialize() {
        // Defer execution until after FXML loading is complete
        Platform.runLater(this::showWelcomeScreenWithShortcuts);
    }

    /** Helper method to dynamically change the content in the center pane. */
    private void setCenterContent(javafx.scene.Node content) {
        if (centerVBox != null) {
            centerVBox.getChildren().clear();
            centerVBox.getChildren().add(content);
            VBox.setVgrow(content, Priority.ALWAYS);
        } else {
            System.err.println("Error: centerVBox not initialized.");
        }
    }

    // =========================================================================
    // INITIAL VIEW & SHORTCUTS LOGIC
    // =========================================================================

    private void showWelcomeScreenWithShortcuts() {
        VBox contentBox = new VBox(25);
        contentBox.setPadding(new Insets(50, 20, 20, 20));
        contentBox.setAlignment(javafx.geometry.Pos.TOP_CENTER);

        Label header = new Label("Instructor Dashboard");
        header.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Label prompt = new Label("Quick Actions:");
        prompt.setStyle("-fx-font-size: 16px; -fx-font-weight: semi-bold; -fx-padding: 20 0 10 0;");

        // Shortcut Button for Grading (Internal Navigation)
        Button gradeButton = new Button("📝 Grade Submissions");
        gradeButton.setPrefSize(250, 50);
        gradeButton.setStyle("-fx-font-size: 16px; -fx-background-color: #2ECC71; -fx-text-fill: white;");
        gradeButton.setOnAction(e -> handleGradeSubmissions());

        // Shortcut Button for Course Management (External Navigation)
        Button manageButton = new Button("📚 Manage Courses");
        manageButton.setPrefSize(250, 50);
        manageButton.setStyle("-fx-font-size: 16px; -fx-background-color: #3498DB; -fx-text-fill: white;");
        manageButton.setOnAction(e -> handleManageCourses());

        contentBox.getChildren().addAll(header, prompt, gradeButton, manageButton);
        setCenterContent(contentBox);
    }


    // =========================================================================
    // FXML ACTION HANDLERS (Menu Items & Shortcuts)
    // =========================================================================

    @FXML
    private void handleManageCourses() {
        System.out.println("LOG: Manage Courses clicked (External View)");
        // Uses your existing application navigation flow:
        ViewManager.showCourseManager(this.instructorId);
    }

    @FXML
    private void handleViewEnrollments() {
        System.out.println("LOG: View Enrollments clicked (Placeholder)");
        showAlert("Feature Not Implemented", "Opening Enrollment Viewer...", AlertType.INFORMATION);
    }

    @FXML
    private void handleGradeSubmissions() {
        System.out.println("LOG: Grade Submissions action triggered (Internal View).");
        showCoursesForGrading();
    }

    @FXML
    private void handleLogout() {
        ViewManager.showLogin();
    }


    // =========================================================================
    // GRADING WORKFLOW STEP 1: SHOW COURSES
    // =========================================================================

    private void showCoursesForGrading() {
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20));

        Label header = new Label("My Courses & Ungraded Submissions");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Button backButton = new Button("← Back to Dashboard");
        backButton.setOnAction(e -> showWelcomeScreenWithShortcuts());

        contentBox.getChildren().addAll(backButton, header);

        List<Course> courses = loadInstructorCourses();

        if (courses.isEmpty()) {
            contentBox.getChildren().add(new Label("You are not currently assigned to teach any courses."));
        } else {
            for (Course course : courses) {
                Button viewSubmissionsButton = new Button("Grade Submissions for " + course.getCourseName());
                viewSubmissionsButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white;");
                viewSubmissionsButton.setOnAction(e -> showSubmissionsForGrading(course));
                contentBox.getChildren().add(viewSubmissionsButton);
            }
        }
        setCenterContent(contentBox);
    }

    private List<Course> loadInstructorCourses() {
        List<Course> courses = new ArrayList<>();
        if (instructorId <= 0) return courses;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_INSTRUCTOR_COURSES)) {

            pstmt.setInt(1, instructorId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(new Course(rs.getInt("id"), null, rs.getString("string_courseName"), null));
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load instructor courses: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
        return courses;
    }


    // =========================================================================
    // GRADING WORKFLOW STEP 2: SHOW UNGRADED SUBMISSIONS
    // =========================================================================

    private void showSubmissionsForGrading(Course course) {
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20));

        Label header = new Label("Ungraded Submissions for: " + course.getCourseName());
        header.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Button backButton = new Button("← Back to Courses");
        backButton.setOnAction(e -> showCoursesForGrading());

        contentBox.getChildren().addAll(backButton, header);

        List<SubmissionForGrading> submissions = loadUngradedSubmissions(course.getId());

        if (submissions.isEmpty()) {
            contentBox.getChildren().add(new Label("No submissions currently require grading for this course."));
        } else {
            for (SubmissionForGrading submission : submissions) {
                HBox submissionRow = createGradingRow(submission);
                contentBox.getChildren().add(submissionRow);
            }
        }

        setCenterContent(contentBox);
    }

    private List<SubmissionForGrading> loadUngradedSubmissions(int courseId) {
        List<SubmissionForGrading> submissions = new ArrayList<>();

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_UNGRADED_SUBMISSIONS)) {

            pstmt.setInt(1, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {

                    int studentId = rs.getInt("student_id");
                    LocalDateTime submissionDate = rs.getTimestamp("datetime_submissionDate").toLocalDateTime();

                    submissions.add(new SubmissionForGrading(
                            rs.getInt("submissionId"),
                            studentId, // Pass the ID
                            rs.getString("assignmentTitle"),
                            submissionDate,
                            rs.getString("string_filePath")
                    ));
                }
            }
        } catch (SQLException e) {
            // This error is now caught and should only appear if the SQL query itself is wrong
            showAlert("Database Error", "Failed to load submissions: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
        return submissions;
    }

    private HBox createGradingRow(SubmissionForGrading submission) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy @ HH:mm");

        Label titleLabel = new Label("Assignment: " + submission.getAssignmentTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox detailsBox = new VBox(5,
                titleLabel,
                new Label("Student: " + submission.getStudentName()), // Displays "Student ID: X"
                new Label("Submitted: " + submission.getSubmissionDate().format(formatter))
        );

        Button gradeButton = new Button("Grade Now");
        gradeButton.setStyle("-fx-background-color: #2ECC71; -fx-text-fill: white; -fx-font-weight: bold;");

        gradeButton.setOnAction(e -> showGradingDialog(
                submission.getSubmissionId(),
                submission.getAssignmentTitle(),
                submission.getStudentName()
        ));

        HBox row = new HBox(20);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-border-color: #CCC; -fx-border-width: 0 0 1 0; -fx-background-color: #F8F8F8;");

        HBox.setHgrow(detailsBox, Priority.ALWAYS);
        row.getChildren().addAll(detailsBox, gradeButton);

        return row;
    }

    // =========================================================================
    // GRADING WORKFLOW STEP 3: DIALOG & DATABASE UPDATE
    // =========================================================================

    public void showGradingDialog(int submissionId, String assignmentTitle, String studentName) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Grade Submission");
        dialog.setHeaderText("Grading: " + assignmentTitle + "\nStudent: " + studentName);

        String filePath = getSubmissionFilePath(submissionId);
        Label filePathLabel = new Label("Submission File Path: " + filePath);
        filePathLabel.setWrapText(true);

        Label scoreLabel = new Label("Score (0.0 to 100.0):");
        TextField scoreField = new TextField();

        Label feedbackLabel = new Label("Feedback:");
        TextArea feedbackArea = new TextArea();

        VBox layout = new VBox(10, filePathLabel, new Separator(), scoreLabel, scoreField, feedbackLabel, feedbackArea);
        layout.setPadding(new Insets(10));
        dialog.getDialogPane().setContent(layout);

        ButtonType gradeButtonType = new ButtonType("Save Grade", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(gradeButtonType, ButtonType.CANCEL);

        Node saveButton = dialog.getDialogPane().lookupButton(gradeButtonType);
        saveButton.setDisable(true);

        scoreField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                float score = Float.parseFloat(newValue);
                saveButton.setDisable(score < 0 || score > 100);
            } catch (NumberFormatException e) {
                saveButton.setDisable(true);
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isPresent() && result.get() == gradeButtonType) {
            try {
                float score = Float.parseFloat(scoreField.getText());
                String feedback = feedbackArea.getText();

                if (updateSubmissionGrade(submissionId, score, feedback)) {
                    showAlert("Success", "Grade saved for " + studentName + ".", AlertType.INFORMATION);
                    showCoursesForGrading(); // Refresh the view
                } else {
                    showAlert("Error", "Failed to update grade in the database.", AlertType.ERROR);
                }

            } catch (NumberFormatException e) {
                showAlert("Error", "Invalid score entered. Grade not saved.", AlertType.ERROR);
            }
        }
    }

    private boolean updateSubmissionGrade(int submissionId, float score, String feedback) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_SUBMISSION_GRADE)) {

            pstmt.setFloat(1, score);
            pstmt.setString(2, feedback);
            pstmt.setInt(3, submissionId);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("Failed to grade submission: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    private String getSubmissionFilePath(int submissionId) {
        String query = "SELECT string_filePath FROM Submission WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setInt(1, submissionId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("string_filePath");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching submission file path: " + e.getMessage());
        }
        return "File path not found.";
    }


    // =========================================================================
    // UTILITIES
    // =========================================================================

    private void showAlert(String title, String content, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}