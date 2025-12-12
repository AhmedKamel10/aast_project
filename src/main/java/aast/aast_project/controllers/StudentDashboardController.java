package aast.aast_project.controllers;

import aast.aast_project.DatabaseConnection;
import aast.aast_project.app.ViewManager;
import aast.aast_project.model.Course;
import aast.aast_project.model.AssignmentDetails;
import aast.aast_project.model.Lesson;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.application.Platform;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Hyperlink;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime; // Added for grade display
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StudentDashboardController {

    // ===============================================
    // FXML FIELDS
    // ===============================================
    @FXML private Button logoutButton;
    @FXML private Button coursesButton;
    @FXML private Button assignmentsButton;
    @FXML private Button gradesButton; // <-- NEW: Add this to your FXML for the menu/shortcut

    // ===============================================
    // STATE
    // ===============================================
    private int studentId;

    // ===============================================
    // DTO for Grade Display (NEW)
    // ===============================================
    public static class StudentGrade {
        String courseName;
        String assignmentTitle;
        Float score; // Can be NULL
        String feedback;
        LocalDateTime submissionDate;

        public StudentGrade(String courseName, String assignmentTitle, Float score, String feedback, LocalDateTime submissionDate) {
            this.courseName = courseName;
            this.assignmentTitle = assignmentTitle;
            this.score = score;
            this.feedback = feedback;
            this.submissionDate = submissionDate;
        }

        public String getCourseName() { return courseName; }
        public String getAssignmentTitle() { return assignmentTitle; }
        public String getScoreDisplay() {
            return (score == null) ? "N/A (Ungraded)" : String.format("%.1f / 100", score);
        }
    }

    // ===============================================
    // SQL QUERIES
    // ===============================================
    private static final String SELECT_ALL_COURSES =
            "SELECT id, string_id, string_courseName, string_description FROM Course";
    private static final String ENROLL_COURSE =
            "INSERT INTO Enrollment (student_id, course_id) VALUES (?, ?)";
    private static final String SELECT_STUDENT_COURSES =
            "SELECT C.id, C.string_id, C.string_courseName FROM Course C JOIN Enrollment SC ON C.id = SC.course_id WHERE SC.student_id = ?";
    private static final String SELECT_ASSIGNMENTS =
            "SELECT A.string_title, A.datetime_dueDate, L.string_title AS lessonTitle, C.string_courseName " +
                    "FROM Assignment A JOIN Lesson L ON A.lesson_id = L.id JOIN Course C ON L.course_id = C.id " +
                    "WHERE C.id = ?";
    private static final String SELECT_LESSONS_BY_COURSE =
            "SELECT id, string_title, string_outline, int_order, course_id FROM Lesson WHERE course_id = ?";
    private static final String CHECK_ASSIGNMENT_EXISTS =
            "SELECT COUNT(id) FROM Assignment WHERE lesson_id = ?";
    private static final String SELECT_LECTURE_DETAILS =
            "SELECT string_url, text_notes FROM Lecture WHERE lesson_id = ?";

    private static final String SELECT_ASSIGNMENT_DETAILS =
            "SELECT id, string_title, text_description, datetime_dueDate, int_maxGrade, string_attachmentPath " +
                    "FROM Assignment WHERE lesson_id = ?";

    private static final String SUBMIT_ASSIGNMENT =
            "INSERT INTO Submission (student_id, assignment_id, string_filePath, datetime_submissionDate) VALUES (?, ?, ?, NOW())";

    // NEW: Query to fetch student's submissions and grades
    private static final String SELECT_STUDENT_GRADES =
            "SELECT C.string_courseName, A.string_title AS assignmentTitle, " +
                    "S.float_score, S.string_feedback, S.datetime_submissionDate " +
                    "FROM Submission S " +
                    "JOIN Assignment A ON S.assignment_id = A.id " +
                    "JOIN Lesson L ON A.lesson_id = L.id " +
                    "JOIN Course C ON L.course_id = C.id " +
                    "WHERE S.student_id = ?";


    // =========================================================================
    // INITIALIZATION AND CONTEXT
    // =========================================================================

    public void setStudentId(int id) {
        this.studentId = id;
        System.out.println("LOG: setStudentId called. studentId set to: " + this.studentId);
    }

    @FXML
    public void initialize() {
        // Link all menu/shortcut buttons to their handlers
        logoutButton.setOnAction(e -> ViewManager.showLogin());
        coursesButton.setOnAction(e -> handleMyCourses());
        assignmentsButton.setOnAction(e -> handleAssignments());
        // Check for the new button before setting the action
        if (gradesButton != null) {
            gradesButton.setOnAction(e -> handleViewGrades());
        }

        Platform.runLater(this::handleMyCourses);
    }

    private void setCenterContent(javafx.scene.Node content) {
        // Use lookup to find the center VBox, as it doesn't have an @FXML field in this controller
        if (coursesButton != null) {
            Node centerNode = coursesButton.getScene().lookup("#centerVBox");
            if (centerNode instanceof VBox parentVBox) {
                parentVBox.getChildren().clear();
                parentVBox.getChildren().add(content);
                VBox.setVgrow(content, Priority.ALWAYS); // Ensure content expands
            } else {
                System.err.println("Error: Could not find center content container. Please ensure the center VBox has fx:id=\"centerVBox\".");
            }
        }
    }


    // =========================================================================
    // MENU HANDLERS
    // =========================================================================

    @FXML
    private void handleMyCourses() {
        // Existing Course logic
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20));

        Label header = new Label("Available Courses (Enroll or View Lectures)");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        contentBox.getChildren().add(header);

        List<Course> allCourses = loadAllCourses();
        List<Integer> enrolledCourseIds = loadEnrolledCourseIds();

        if (allCourses.isEmpty()) {
            contentBox.getChildren().add(new Label("No courses are currently available."));
        } else {
            for (Course course : allCourses) {
                boolean isEnrolled = enrolledCourseIds.contains(course.getId());
                HBox courseRow = createCourseRow(course, isEnrolled);
                contentBox.getChildren().add(courseRow);
            }
        }

        setCenterContent(contentBox);
    }

    @FXML
    private void handleAssignments() {
        // Existing Assignment logic
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20));

        Label header = new Label("Your Assignments by Course");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        contentBox.getChildren().add(header);

        List<Course> enrolledCourses = loadEnrolledCoursesWithNames();

        if (enrolledCourses.isEmpty()) {
            contentBox.getChildren().add(new Label("You are not currently enrolled in any courses."));
        } else {
            for (Course course : enrolledCourses) {
                List<AssignmentDetails> assignments = loadAssignmentsForCourse(course.getId());

                TitledPane coursePane = createAssignmentCoursePane(course, assignments);
                contentBox.getChildren().add(coursePane);
            }
        }
        setCenterContent(contentBox);
    }

    // NEW HANDLER: For viewing grades
    @FXML
    private void handleViewGrades() {
        showStudentGrades();
    }


    // =========================================================================
    // COURSES LOGIC (Existing Methods)
    // =========================================================================

    private HBox createCourseRow(Course course, boolean isEnrolled) {
        Label nameLabel = new Label(course.getCourseIdString() + " - " + course.getCourseName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label descriptionLabel = new Label(course.getDescription());
        descriptionLabel.setWrapText(true);

        Button actionButton = new Button(isEnrolled ? "VIEW LECTURES" : "Enroll Now");
        actionButton.setDisable(false);

        actionButton.setStyle(isEnrolled ? "-fx-background-color: #2ECC71; -fx-text-fill: white;" : "-fx-background-color: #3498DB; -fx-text-fill: white;");

        if (isEnrolled) {
            actionButton.setOnAction(e -> showLessonsForCourse(course));
        } else {
            actionButton.setOnAction(e -> handleEnrollment(course));
        }

        VBox courseDetails = new VBox(5, nameLabel, descriptionLabel);
        VBox.setVgrow(courseDetails, Priority.ALWAYS);

        HBox row = new HBox(20, courseDetails, new Region(), actionButton);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-border-color: #CCC; -fx-border-radius: 5; -fx-background-color: #F8F8F8;");

        return row;
    }

    private List<Course> loadAllCourses() {
        List<Course> courses = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ALL_COURSES);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                courses.add(new Course(
                        rs.getInt("id"),
                        rs.getString("string_id"),
                        rs.getString("string_courseName"),
                        rs.getString("string_description")
                ));
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load all courses: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
        return courses;
    }

    private List<Integer> loadEnrolledCourseIds() {
        List<Integer> enrolledIds = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_STUDENT_COURSES)) {

            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    enrolledIds.add(rs.getInt("id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load enrolled course IDs: " + e.getMessage());
        }
        return enrolledIds;
    }

    private void handleEnrollment(Course course) {
        if (this.studentId <= 0) {
            System.err.println("FATAL ERROR: studentId is uninitialized (0). Enrollment failed.");
            showAlert("Initialization Error", "Cannot enroll. Student ID was not correctly loaded upon login.", AlertType.ERROR);
            return;
        }

        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Enroll Confirmation");
        alert.setHeaderText("Enroll in " + course.getCourseName());
        alert.setContentText("Are you sure you want to enroll in this course?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try (Connection conn = DatabaseConnection.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(ENROLL_COURSE)) {

                pstmt.setInt(1, studentId);
                pstmt.setInt(2, course.getId());

                pstmt.executeUpdate();

                showAlert("Success", "Successfully enrolled in " + course.getCourseName(), AlertType.INFORMATION);
                handleMyCourses();

            } catch (SQLException e) {
                e.printStackTrace();
                showAlert("Database Error", "Enrollment failed. Course might be already enrolled or DB error: " + e.getMessage(), AlertType.ERROR);
            }
        }
    }

    // ... (All existing Lesson and Assignment logic methods remain here) ...
    // ... (You should keep all the original methods like loadLessonsForCourse, showLectureDetails, showAssignmentDetails, etc.) ...

    // =========================================================================
    // SUBMISSION LOGIC (Existing Method)
    // =========================================================================

    private void handleSubmission(int assignmentId, String filePath) {
        // Check if the file path is empty before attempting submission
        if (filePath == null || filePath.trim().isEmpty()) {
            showAlert("Submission Failed", "Please select a valid file path.", AlertType.WARNING);
            return;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SUBMIT_ASSIGNMENT)) {

            pstmt.setInt(1, studentId);
            pstmt.setInt(2, assignmentId);
            pstmt.setString(3, filePath);

            pstmt.executeUpdate();
            showAlert("Submission Successful", "Your assignment has been submitted successfully.", AlertType.INFORMATION);

        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Database Error", "Failed to submit assignment: " + e.getMessage(), AlertType.ERROR);
        }
    }

    // =========================================================================
    // LECTURES (LESSONS) LOGIC (Existing Methods)
    // =========================================================================

    private void showLessonsForCourse(Course course) {
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20));

        Label header = new Label("Lectures for: " + course.getCourseName());
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");

        Button backButton = new Button("← Back to Courses");
        backButton.setOnAction(e -> handleMyCourses());

        contentBox.getChildren().addAll(backButton, header);

        if (this.studentId <= 0) {
            contentBox.getChildren().add(new Label("Error: Student session ID is missing. Cannot load lessons."));
            setCenterContent(contentBox);
            return;
        }

        List<Lesson> lessons = loadLessonsForCourse(course.getId());

        if (lessons.isEmpty()) {
            contentBox.getChildren().add(new Label("No lectures have been posted for this course yet."));
        } else {
            for (Lesson lesson : lessons) {
                HBox lessonRow = createLessonRow(lesson);
                contentBox.getChildren().add(lessonRow);
            }
        }

        setCenterContent(contentBox);
    }

    private List<Lesson> loadLessonsForCourse(int courseId) {
        List<Lesson> lessons = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmtLessons = conn.prepareStatement(SELECT_LESSONS_BY_COURSE);
             PreparedStatement pstmtCheckAssignment = conn.prepareStatement(CHECK_ASSIGNMENT_EXISTS)) {

            pstmtLessons.setInt(1, courseId);
            try (ResultSet rsLessons = pstmtLessons.executeQuery()) {

                while (rsLessons.next()) {
                    Lesson lesson = new Lesson(
                            rsLessons.getInt("id"),
                            rsLessons.getString("string_title"),
                            rsLessons.getString("string_outline"),
                            rsLessons.getInt("int_order"),
                            rsLessons.getInt("course_id")
                    );

                    pstmtCheckAssignment.setInt(1, lesson.getId());
                    try (ResultSet rsCheck = pstmtCheckAssignment.executeQuery()) {
                        if (rsCheck.next() && rsCheck.getInt(1) > 0) {
                            lesson.setContentType("Assignment");
                        } else {
                            lesson.setContentType("Lecture");
                        }
                    }

                    lessons.add(lesson);
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load lessons with type check: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
        return lessons;
    }

    private void showLectureDetails(Lesson lesson) {
        String url = "No URL provided.";
        String notes = "No notes available.";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_LECTURE_DETAILS)) {

            pstmt.setInt(1, lesson.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    url = rs.getString("string_url");
                    notes = rs.getString("text_notes") == null ? "No notes available." : rs.getString("text_notes");
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load lecture details: " + e.getMessage());
            showAlert("Database Error", "Failed to load lecture details from the database.", AlertType.ERROR);
            return;
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        Label urlLabel = new Label("Video/Resource URL:");
        urlLabel.setStyle("-fx-font-weight: bold;");
        Hyperlink urlLink = new Hyperlink(url);

        Label notesHeader = new Label("Instructor Notes:");
        notesHeader.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");
        Label notesContent = new Label(notes);
        notesContent.setWrapText(true);

        content.getChildren().addAll(urlLabel, urlLink, notesHeader, notesContent);

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Lecture Details");
        alert.setHeaderText(lesson.getTitle() + " (" + lesson.getContentType() + ")");

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(content);
        dialogPane.setPrefWidth(550);
        dialogPane.setPrefHeight(450);

        alert.showAndWait();
    }

    private void showAssignmentDetails(Lesson lesson) {
        String title = lesson.getTitle();
        String description = "No description provided.";
        String dueDate = "N/A";
        String maxGrade = "N/A";
        String attachmentPath = null;

        int actualAssignmentId = -1;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy @ HH:mm");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ASSIGNMENT_DETAILS)) {

            pstmt.setInt(1, lesson.getId());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    actualAssignmentId = rs.getInt("id");
                    description = rs.getString("text_description") == null ? "No description provided." : rs.getString("text_description");

                    if (rs.getTimestamp("datetime_dueDate") != null) {
                        dueDate = rs.getTimestamp("datetime_dueDate").toLocalDateTime().format(formatter);
                    } else {
                        dueDate = "No due date set.";
                    }

                    maxGrade = String.valueOf(rs.getInt("int_maxGrade"));
                    attachmentPath = rs.getString("string_attachmentPath");
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load assignment details: " + e.getMessage());
            showAlert("Database Error", "Failed to load assignment details from the database.", AlertType.ERROR);
            return;
        }

        if (actualAssignmentId == -1) {
            showAlert("Error", "Could not find corresponding Assignment record for this lesson.", AlertType.ERROR);
            return;
        }

        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        Label descriptionHeader = new Label("Description:");
        descriptionHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label descriptionContent = new Label(description);
        descriptionContent.setWrapText(true);

        Label infoHeader = new Label("Details:");
        infoHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 0 0 0;");

        Label dueLabel = new Label("Due Date: " + dueDate);
        Label gradeLabel = new Label("Maximum Grade: " + maxGrade);

        content.getChildren().addAll(descriptionHeader, descriptionContent, infoHeader, dueLabel, gradeLabel);

        if (attachmentPath != null && !attachmentPath.trim().isEmpty()) {
            Label attachmentHeader = new Label("File Attachment:");
            attachmentHeader.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");
            Hyperlink attachmentLink = new Hyperlink(attachmentPath);

            final String finalAttachmentPath = attachmentPath;

            attachmentLink.setOnAction(e -> {
                showAlert("File Access", "Attempting to access file at: " + finalAttachmentPath + "\n(Real file/URL opening implementation needed)", AlertType.INFORMATION);
            });
            content.getChildren().addAll(attachmentHeader, attachmentLink);
        }

        Separator separator = new Separator();
        separator.setPadding(new Insets(10, 0, 10, 0));
        content.getChildren().add(separator);

        Button submitButton = new Button("Submit Assignment");
        submitButton.setStyle("-fx-background-color: #2ECC71; -fx-text-fill: white; -fx-font-weight: bold;");

        final int finalAssignmentId = actualAssignmentId;
        submitButton.setOnAction(e -> {
            Platform.runLater(() -> showSubmissionDialog(finalAssignmentId));
        });

        content.getChildren().add(submitButton);

        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle("Assignment Details");
        alert.setHeaderText(title);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setContent(content);
        dialogPane.setPrefWidth(550);
        dialogPane.setPrefHeight(500);

        alert.showAndWait();
    }

    private HBox createLessonRow(Lesson lesson) {
        String icon = "📚";
        String style = "-fx-background-color: #F8F8F8;";
        String buttonText = "View Content";

        boolean isLecture = "Lecture".equals(lesson.getContentType());
        boolean isAssignment = "Assignment".equals(lesson.getContentType());

        if (isAssignment) {
            icon = "📝";
            style = "-fx-background-color: #FEF9E7;";
            buttonText = "View Details";
        }

        Label typeLabel = new Label(" (" + lesson.getContentType() + ")");
        typeLabel.setStyle("-fx-font-size: 11px; -fx-font-style: italic; -fx-text-fill: #7F8C8D;");

        Label titleLabel = new Label(icon + " " + lesson.getTitle());
        titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        HBox titleBox = new HBox(5, titleLabel, typeLabel);

        Button viewButton = new Button(buttonText);
        viewButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white;");

        if (isLecture) {
            viewButton.setOnAction(e -> showLectureDetails(lesson));
        } else if (isAssignment) {
            viewButton.setOnAction(e -> showAssignmentDetails(lesson));
        } else {
            String content = lesson.getContent();
            if (content == null || content.trim().isEmpty()) {
                content = "No content has been uploaded for this lesson yet.";
            }

            final String finalContent = content;

            viewButton.setOnAction(e -> showAlert(lesson.getTitle(), finalContent, AlertType.INFORMATION));
        }

        VBox detailsBox = new VBox(5, titleBox);

        HBox row = new HBox(20, detailsBox, new Region(), viewButton);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-border-color: #CCC; -fx-border-width: 0 0 1 0; " + style);

        return row;
    }

    // =========================================================================
    // GRADING LOGIC (NEW SECTION)
    // =========================================================================

    /**
     * Loads the student's grades and displays them in the center VBox.
     */
    private void showStudentGrades() {
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20));

        Label header = new Label("My Grades and Feedback");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        contentBox.getChildren().add(header);

        List<StudentGrade> grades = loadStudentGrades();

        if (grades.isEmpty()) {
            contentBox.getChildren().add(new Label("You have not submitted any assignments yet."));
        } else {
            for (StudentGrade grade : grades) {
                HBox gradeRow = createGradeRow(grade);
                contentBox.getChildren().add(gradeRow);
            }
        }
        setCenterContent(contentBox);
    }

    private List<StudentGrade> loadStudentGrades() {
        List<StudentGrade> grades = new ArrayList<>();
        if (studentId <= 0) return grades;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_STUDENT_GRADES)) {

            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // GetObject is safe for nullable floats
                    Float score = rs.getObject("float_score", Float.class);
                    LocalDateTime submissionDate = rs.getTimestamp("datetime_submissionDate").toLocalDateTime();

                    grades.add(new StudentGrade(
                            rs.getString("string_courseName"),
                            rs.getString("assignmentTitle"),
                            score,
                            rs.getString("string_feedback"),
                            submissionDate
                    ));
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load student grades: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
        return grades;
    }

    private HBox createGradeRow(StudentGrade grade) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        Label assignmentLabel = new Label(grade.getAssignmentTitle());
        assignmentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label scoreLabel = new Label(grade.getScoreDisplay());
        scoreLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: " + (grade.score == null ? "#FFC300" : "#2ECC71") + "; -fx-font-weight: bold;");

        VBox detailsBox = new VBox(5,
                new Label("Course: " + grade.getCourseName()),
                assignmentLabel,
                new Label("Submitted: " + grade.submissionDate.format(formatter))
        );

        Button viewDetailsButton = new Button("View Feedback");
        viewDetailsButton.setOnAction(e -> showFeedbackDialog(grade));

        HBox row = new HBox(20);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: #FFFFFF;");

        // Layout: Details (left) | Score (center) | Button (right)
        HBox.setHgrow(detailsBox, Priority.ALWAYS);
        row.getChildren().addAll(detailsBox, scoreLabel, viewDetailsButton);

        return row;
    }

    private void showFeedbackDialog(StudentGrade grade) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Assignment Feedback");
        dialog.setHeaderText(grade.getAssignmentTitle() + " in " + grade.getCourseName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));

        Label scoreHeader = new Label("Final Score:");
        scoreHeader.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        Label scoreValue = new Label(grade.getScoreDisplay());
        scoreValue.setStyle("-fx-font-size: 20px; -fx-text-fill: " + (grade.score == null ? "#FFC300" : "#2ECC71") + "; -fx-font-weight: bold;");

        String feedbackText = (grade.feedback == null || grade.feedback.isEmpty())
                ? "No specific feedback was provided yet."
                : grade.feedback;

        TextArea feedbackArea = new TextArea(feedbackText);
        feedbackArea.setEditable(false);
        feedbackArea.setWrapText(true);
        feedbackArea.setPrefRowCount(8);

        layout.getChildren().addAll(scoreHeader, scoreValue, new Label("Instructor Feedback:"), feedbackArea);
        dialog.getDialogPane().setContent(layout);
        dialog.showAndWait();
    }

    // ... (All existing Assignment bulk view logic methods remain here) ...
    private List<Course> loadEnrolledCoursesWithNames() {
        List<Course> courses = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_STUDENT_COURSES)) {

            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    courses.add(new Course(
                            rs.getInt("id"),
                            rs.getString("string_id"),
                            rs.getString("string_courseName"),
                            ""
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load enrolled courses with names: " + e.getMessage());
        }
        return courses;
    }

    private List<AssignmentDetails> loadAssignmentsForCourse(int courseId) {
        List<AssignmentDetails> assignments = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_ASSIGNMENTS)) {

            pstmt.setInt(1, courseId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    assignments.add(new AssignmentDetails(
                            rs.getString("string_title"),
                            rs.getTimestamp("datetime_dueDate").toLocalDateTime(),
                            rs.getString("lessonTitle"),
                            rs.getString("string_courseName")
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load assignments for course " + courseId + ": " + e.getMessage());
        }
        return assignments;
    }

    private TitledPane createAssignmentCoursePane(Course course, List<AssignmentDetails> assignments) {
        VBox assignmentList = new VBox(10);
        assignmentList.setPadding(new Insets(10));

        if (assignments.isEmpty()) {
            assignmentList.getChildren().add(new Label("No assignments currently set for this course."));
        } else {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy @ HH:mm");
            for (AssignmentDetails assignment : assignments) {
                Label titleLabel = new Label("📝 " + assignment.getTitle());
                titleLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #34495E;");

                Label dueLabel = new Label("Due: " + assignment.getDueDate().format(formatter));

                if (assignment.getDueDate().isBefore(java.time.LocalDateTime.now())) {
                    dueLabel.setStyle("-fx-text-fill: #C0392B; -fx-font-weight: bold;");
                } else {
                    dueLabel.setStyle("-fx-text-fill: #27AE60;");
                }

                Label lessonLabel = new Label("From Lesson: " + assignment.getLessonTitle());
                lessonLabel.setStyle("-fx-font-style: italic; -fx-font-size: 11px;");

                VBox assignmentDetails = new VBox(3, titleLabel, dueLabel, lessonLabel);
                assignmentDetails.setPadding(new Insets(5));
                assignmentDetails.setStyle("-fx-border-color: #DDE; -fx-border-width: 0 0 1 0;");
                assignmentList.getChildren().add(assignmentDetails);
            }
        }

        TitledPane pane = new TitledPane(course.getCourseName() + " (" + assignments.size() + " assignments)", assignmentList);
        pane.setCollapsible(true);
        pane.setExpanded(false);
        pane.setStyle("-fx-background-color: #ECF0F1;");
        return pane;
    }


    // =========================================================================
    // UTILITIES & DIALOGS (Existing Methods)
    // =========================================================================

    private void showSubmissionDialog(int assignmentId) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Submit Assignment");
        dialog.setHeaderText("Select a document file to submit (e.g., .pdf, .docx).");

        ButtonType submitButtonType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, ButtonType.CANCEL);

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));

        TextField filePathField = new TextField();
        filePathField.setPromptText("Path to your submission file");
        filePathField.setEditable(false);

        Button browseButton = new Button("Browse...");

        browseButton.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Select Submission File");

            fileChooser.getExtensionFilters().addAll(
                    new javafx.stage.FileChooser.ExtensionFilter("Document Files", "*.pdf", "*.docx", "*.doc", "*.txt"),
                    new javafx.stage.FileChooser.ExtensionFilter("All Files", "*.*")
            );

            java.io.File selectedFile = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (selectedFile != null) {
                filePathField.setText(selectedFile.getAbsolutePath());
            }
        });

        content.getChildren().addAll(new Label("Selected File Path:"), filePathField, browseButton);
        dialog.getDialogPane().setContent(content);

        Node submitButton = dialog.getDialogPane().lookupButton(submitButtonType);
        submitButton.setDisable(true);

        filePathField.textProperty().addListener((obs, oldText, newText) -> {
            submitButton.setDisable(newText == null || newText.trim().isEmpty());
        });

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                return filePathField.getText();
            }
            return null;
        });

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(path -> {
            handleSubmission(assignmentId, path);
        });
    }

    private void showAlert(String title, String message, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}