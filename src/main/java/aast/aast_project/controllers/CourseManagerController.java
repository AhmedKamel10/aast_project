package aast.aast_project.controllers;

import aast.aast_project.DatabaseConnection;
import aast.aast_project.app.ViewManager;
import aast.aast_project.model.Course;
import aast.aast_project.model.Lesson;
import aast.aast_project.model.Question;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView; // ADDED: For embedded video playback
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

public class CourseManagerController {

    // ===============================================
    // FXML FIELDS (COURSE MANAGEMENT - LEFT PANEL)
    // ===============================================
    @FXML private TableView<Course> courseTable;
    @FXML private TableColumn<Course, Integer> idColumn;
    @FXML private TableColumn<Course, String> courseIdStringColumn;
    @FXML private TableColumn<Course, String> nameColumn;
    @FXML private TableColumn<Course, String> descriptionColumn;
    @FXML private TableColumn<Course, Void> actionsColumn;

    @FXML private TextField courseIdStringField;
    @FXML private TextField courseNameField;
    @FXML private TextArea descriptionArea;
    @FXML private Button saveButton;

    // ===============================================
    // FXML FIELDS (LESSON CORE - RIGHT PANEL)
    // ===============================================
    @FXML private VBox lessonManagerPane;
    @FXML private Label lessonHeaderLabel;
    @FXML private TableView<Lesson> lessonTable;
    @FXML private TableColumn<Lesson, Integer> lessonIdColumn;
    @FXML private TableColumn<Lesson, Integer> lessonOrderColumn;
    @FXML private TableColumn<Lesson, String> lessonTitleColumn;
    @FXML private TableColumn<Lesson, String> lessonContentColumn;
    @FXML private TableColumn<Lesson, Void> lessonActionsColumn;

    @FXML private TextField lessonTitleField;
    @FXML private TextField lessonOrderField;

    @FXML private Button lessonSaveButton;

    // ===============================================
    // FXML FIELDS (LECTURE TAB)
    // ===============================================
    @FXML private TextField lectureTitleField;
    @FXML private TextField lectureUrlField;
    @FXML private TextArea lectureNotesArea;
    @FXML private WebView lectureWebView; // <-- ADDED FIX for "cannot find symbol"

    // ===============================================
    // FXML FIELDS (ASSIGNMENT TAB)
    // ===============================================
    @FXML private TextField assignmentTitleField;
    @FXML private DatePicker assignmentDueDate;
    @FXML private TextField assignmentMaxGradeField;
    @FXML private TextArea assignmentDescriptionArea;

    // ===============================================
    // FXML FIELDS (QUIZ TAB)
    // ===============================================
    @FXML private TextField quizTitleField;
    @FXML private TextField quizPassingScoreField;
    @FXML private Button editQuizQuestionsButton;

    // ===============================================
    // FXML FIELDS (LESSON PREVIEW) - REMOVED DUE TO NPE
    // ===============================================
    /*
    @FXML private VBox lessonPreviewVBox;
    @FXML private Label previewInstructionLabel;
    @FXML private Label previewContentTypeLabel;
    @FXML private Label previewTitleLabel;
    @FXML private Label previewUrlLabel;
    @FXML private Label previewDueDateLabel;
    @FXML private Label previewGradeLabel;
    @FXML private TextArea previewNotesArea;
    */


    // ===============================================
    // CONTEXT & STATE
    // ===============================================
    private int instructorId;
    private Course selectedCourse = null;
    private int currentSelectedCourseId = -1;
    private Lesson selectedLesson = null;

    // NEW STATE: Holds the data structure returned by QuizCreationController
    private QuizData currentQuizData = null;

    // --- SQL Queries ---
    private static final String SELECT_COURSES = "SELECT id, string_id, string_courseName, string_description FROM Course WHERE instructor_id = ?";
    private static final String INSERT_COURSE = "INSERT INTO Course (string_id, string_courseName, string_description, instructor_id) VALUES (?, ?, ?, ?)";
    private static final String UPDATE_COURSE = "UPDATE Course SET string_id = ?, string_courseName = ?, string_description = ? WHERE id = ?";
    private static final String DELETE_COURSE = "DELETE FROM Course WHERE id = ?";

    private static final String SELECT_LESSONS = "SELECT id, string_title, int_order FROM Lesson WHERE course_id = ? ORDER BY int_order";
    private static final String INSERT_LESSON_RETURN_ID = "INSERT INTO Lesson (string_title, int_order, course_id) VALUES (?, ?, ?)";
    private static final String DELETE_LESSON = "DELETE FROM Lesson WHERE id = ?";

    private static final String INSERT_LECTURE = "INSERT INTO Lecture (string_title, string_url, text_notes, lesson_id) VALUES (?, ?, ?, ?)";
    private static final String INSERT_ASSIGNMENT = "INSERT INTO Assignment (string_title, text_description, datetime_dueDate, int_maxGrade, lesson_id) VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_QUIZ = "INSERT INTO Quiz (string_title, integer_passingScore, lesson_id) VALUES (?, ?, ?)";
    private static final String INSERT_QUESTION = "INSERT INTO Question (string_questionText, string_optionA, string_optionB, string_optionC, string_optionD, char_correctAnswer, quiz_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

    private static final String CHECK_LECTURE = "SELECT string_title FROM Lecture WHERE lesson_id = ?";
    private static final String CHECK_ASSIGNMENT = "SELECT string_title FROM Assignment WHERE lesson_id = ?";
    private static final String CHECK_QUIZ = "SELECT string_title FROM Quiz WHERE lesson_id = ?";

    // Content Retrieval Queries
    private static final String SELECT_LECTURE_CONTENT = "SELECT string_title, string_url, text_notes FROM Lecture WHERE lesson_id = ?";
    private static final String SELECT_ASSIGNMENT_CONTENT = "SELECT string_title, text_description, datetime_dueDate, int_maxGrade FROM Assignment WHERE lesson_id = ?";
    private static final String SELECT_QUIZ_CONTENT = "SELECT string_title, integer_passingScore FROM Quiz WHERE lesson_id = ?";


    // =========================================================================
    //                            INITIALIZATION & CONTEXT
    // =========================================================================

    public void setInstructorId(int id) {
        this.instructorId = id;
        loadCourses();
        lessonManagerPane.setVisible(false);
    }

    /**
     * Loads a video URL into the WebView component. Supports YouTube embedding.
     */
    private void loadVideo(String url) {
        if (lectureWebView == null) return;

        if (url == null || url.trim().isEmpty()) {
            lectureWebView.getEngine().loadContent("<html><body style='text-align:center; font-family:sans-serif;'>Enter a YouTube/Video URL above to preview here.</body></html>");
            return;
        }

        // Basic logic to embed a YouTube video
        String embedUrl = url;
        if (url.contains("youtube.com/watch?v=")) {
            // Extract the video ID
            String videoId = url.split("v=")[1].split("&")[0];
            // Use HTML iframe for YouTube embedding
        }

    }

    @FXML
    public void initialize() {
        // --- COURSE Table Setup ---
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        courseIdStringColumn.setCellValueFactory(new PropertyValueFactory<>("courseIdString"));
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("courseName"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        setupCourseActionsColumn();

        // --- LESSON Table Setup ---
        lessonIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        lessonOrderColumn.setCellValueFactory(new PropertyValueFactory<>("order"));
        lessonTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        lessonContentColumn.setCellValueFactory(data -> {
            String contentType = data.getValue().getContentType();
            return new SimpleStringProperty(contentType != null ? contentType : "N/A");
        });
        setupLessonActionsColumn();

        // --- LESSON TABLE SELECTION LISTENER ---
        lessonTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showLessonContentDetails(newSelection);
            }
            // Removed: else { handleClearPreview(); }
        });

        // Removed: Setup initial preview state: handleClearPreview();

        // Setup Video URL Listener
        if (lectureUrlField != null && lectureWebView != null) {
            lectureUrlField.textProperty().addListener((obs, oldUrl, newUrl) -> {
                loadVideo(newUrl);
            });
        }
    }


    // =========================================================================
    //                            COURSE ACTIONS
    // =========================================================================

    private void setupCourseActionsColumn() {
        actionsColumn.setCellFactory(tc -> new TableCell<>() {
            final Button editBtn = new Button("Edit");
            final Button deleteBtn = new Button("Delete");
            final Button manageLessonBtn = new Button("Lessons");

            final HBox pane = new HBox(5, editBtn, deleteBtn, manageLessonBtn);

            {
                editBtn.setOnAction(e -> {
                    Course course = getTableView().getItems().get(getIndex());
                    editCourse(course);
                });

                deleteBtn.setOnAction(e -> {
                    Course course = getTableView().getItems().get(getIndex());
                    deleteCourseConfirmation(course);
                });

                manageLessonBtn.setOnAction(e -> {
                    Course course = getTableView().getItems().get(getIndex());
                    showLessonManagerForCourse(course);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    private void loadCourses() {
        if (this.instructorId == 0) {
            showAlert("Error", "Instructor ID not set. Cannot load courses.", AlertType.ERROR);
            return;
        }

        courseTable.getItems().clear();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_COURSES)) {

            pstmt.setInt(1, this.instructorId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    courseTable.getItems().add(new Course(
                            rs.getInt("id"),
                            rs.getString("string_id"),
                            rs.getString("string_courseName"),
                            rs.getString("string_description")
                    ));
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load courses: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleSaveCourse() {
        String courseIdString = courseIdStringField.getText().trim();
        String courseName = courseNameField.getText().trim();
        String description = descriptionArea.getText().trim();

        if (courseIdString.isEmpty() || courseName.isEmpty()) {
            showAlert("Input Error", "Course Code and Course Name are required.", AlertType.WARNING);
            return;
        }

        if (selectedCourse == null) {
            insertCourse(courseIdString, courseName, description);
        } else {
            updateCourse(selectedCourse.getId(), courseIdString, courseName, description);
        }
    }

    private void insertCourse(String courseIdString, String courseName, String description) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_COURSE)) {

            pstmt.setString(1, courseIdString);
            pstmt.setString(2, courseName);
            pstmt.setString(3, description);
            pstmt.setInt(4, this.instructorId);

            pstmt.executeUpdate();
            showAlert("Success", "Course added successfully.", AlertType.INFORMATION);
            loadCourses();
            handleClearForm();

        } catch (SQLException e) {
            if (e.getMessage().contains("Duplicate entry")) {
                showAlert("Input Error", "Course Code already exists.", AlertType.WARNING);
            } else if (e.getMessage().contains("foreign key constraint fails")) {
                showAlert("Database Error", "Failed to add course: Instructor ID is invalid or not registered.", AlertType.ERROR);
            } else {
                showAlert("Database Error", "Failed to add course: " + e.getMessage(), AlertType.ERROR);
            }
            e.printStackTrace();
        }
    }

    private void editCourse(Course course) {
        selectedCourse = course;
        courseIdStringField.setText(course.getCourseIdString());
        courseNameField.setText(course.getCourseName());
        descriptionArea.setText(course.getDescription());
        saveButton.setText("Update Course");
    }

    private void updateCourse(int id, String courseIdString, String courseName, String description) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_COURSE)) {

            pstmt.setString(1, courseIdString);
            pstmt.setString(2, courseName);
            pstmt.setString(3, description);
            pstmt.setInt(4, id);

            pstmt.executeUpdate();
            showAlert("Success", "Course updated successfully.", AlertType.INFORMATION);
            loadCourses();
            handleClearForm();

        } catch (SQLException e) {
            showAlert("Database Error", "Failed to update course: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void deleteCourseConfirmation(Course course) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Delete Course");
        alert.setHeaderText("Confirm Deletion");
        alert.setContentText("Are you sure you want to delete the course: " + course.getCourseName() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteCourse(course.getId());
        }
    }

    private void deleteCourse(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_COURSE)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();

            showAlert("Success", "Course deleted successfully.", AlertType.INFORMATION);
            loadCourses();

            if (currentSelectedCourseId == id) {
                lessonManagerPane.setVisible(false);
                currentSelectedCourseId = -1;
            }

        } catch (SQLException e) {
            showAlert("Database Error", "Failed to delete course: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearForm() {
        selectedCourse = null;
        courseIdStringField.clear();
        courseNameField.clear();
        descriptionArea.clear();
        saveButton.setText("Save Course");
    }

    @FXML
    private void handleBackToDashboard() {
        ViewManager.showTeacherDashboard(this.instructorId);
    }

    // =========================================================================
    //                            LESSON ACTIONS
    // =========================================================================

    private void showLessonManagerForCourse(Course course) {
        currentSelectedCourseId = course.getId();
        lessonHeaderLabel.setText("Lessons for: " + course.getCourseName() + " (" + course.getCourseIdString() + ")");
        loadLessons();
        lessonManagerPane.setVisible(true);
        handleClearLessonForm();
    }

    private void setupLessonActionsColumn() {
        lessonActionsColumn.setCellFactory(tc -> new TableCell<>() {
            final Button editBtn = new Button("Edit");
            final Button deleteBtn = new Button("Delete");
            final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setOnAction(e -> {
                    Lesson lesson = getTableView().getItems().get(getIndex());
                    editLesson(lesson);
                });

                deleteBtn.setOnAction(e -> {
                    Lesson lesson = getTableView().getItems().get(getIndex());
                    deleteLessonConfirmation(lesson);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    /**
     * Loads base Lesson records and checks content type for display.
     */
    private void loadLessons() {
        lessonTable.getItems().clear();
        if (currentSelectedCourseId == -1) return;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_LESSONS)) {

            pstmt.setInt(1, currentSelectedCourseId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Lesson lesson = new Lesson(
                            rs.getInt("id"),
                            rs.getString("string_title"),
                            null,
                            rs.getInt("int_order"),
                            currentSelectedCourseId
                    );

                    lesson.setContentType(determineContentType(lesson.getId()));

                    lessonTable.getItems().add(lesson);
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load lessons: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    /**
     * Helper to check which content table (Lecture, Assignment, Quiz) has a link
     * to the given lessonId.
     */
    private String determineContentType(int lessonId) {
        try (Connection conn = DatabaseConnection.getConnection()) {

            if (checkContentExistence(conn, CHECK_LECTURE, lessonId)) return "Lecture";
            if (checkContentExistence(conn, CHECK_ASSIGNMENT, lessonId)) return "Assignment";
            if (checkContentExistence(conn, CHECK_QUIZ, lessonId)) return "Quiz";

        } catch (SQLException e) {
            // Log warning, but don't stop execution
            System.err.println("Warning: Could not determine content type due to SQL error (missing content table?): " + e.getMessage());
        }
        return "Empty";
    }

    private boolean checkContentExistence(Connection conn, String sql, int lessonId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, lessonId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Loads the content of the selected lesson into the respective tabs for viewing/editing.
     */
    private void showLessonContentDetails(Lesson lesson) {
        // 1. Clear current form state
        handleClearLessonForm();

        // 2. Set core Lesson data
        selectedLesson = lesson;
        lessonTitleField.setText(lesson.getTitle());
        lessonOrderField.setText(String.valueOf(lesson.getOrder()));
        lessonSaveButton.setText("Update Lesson (Content Update Not Supported)");

        // 3. Load associated content (Lecture, Assignment, or Quiz)
        String contentType = lesson.getContentType();

        try (Connection conn = DatabaseConnection.getConnection()) {
            switch (contentType) {
                case "Lecture":
                    loadLectureContent(conn, lesson.getId());
                    break;
                case "Assignment":
                    loadAssignmentContent(conn, lesson.getId());
                    break;
                case "Quiz":
                    loadQuizContent(conn, lesson.getId());
                    break;
                default:
                    // No content to load
                    break;
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load content for lesson: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void loadLectureContent(Connection conn, int lessonId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_LECTURE_CONTENT)) {
            pstmt.setInt(1, lessonId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    lectureTitleField.setText(rs.getString("string_title"));
                    String url = rs.getString("string_url");
                    lectureUrlField.setText(url);
                    lectureNotesArea.setText(rs.getString("text_notes"));
                    // This triggers the WebView listener (in initialize()) and loads the video
                    loadVideo(url);
                }
            }
        }
    }

    private void loadAssignmentContent(Connection conn, int lessonId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_ASSIGNMENT_CONTENT)) {
            pstmt.setInt(1, lessonId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    assignmentTitleField.setText(rs.getString("string_title"));
                    assignmentDescriptionArea.setText(rs.getString("text_description"));

                    Timestamp dueDateTimestamp = rs.getTimestamp("datetime_dueDate");
                    if (dueDateTimestamp != null) {
                        assignmentDueDate.setValue(dueDateTimestamp.toLocalDateTime().toLocalDate());
                    } else {
                        assignmentDueDate.setValue(null);
                    }

                    assignmentMaxGradeField.setText(String.valueOf(rs.getInt("int_maxGrade")));
                }
            }
        }
    }

    private void loadQuizContent(Connection conn, int lessonId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SELECT_QUIZ_CONTENT)) {
            pstmt.setInt(1, lessonId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    quizTitleField.setText(rs.getString("string_title"));
                    quizPassingScoreField.setText(String.valueOf(rs.getInt("integer_passingScore")));
                }
            }
        }
    }


    // =========================================================================
    //                            QUIZ BUILDER INTEGRATION
    // =========================================================================

    @FXML
    private void handleEditQuizQuestions() {
        if (currentSelectedCourseId == -1) {
            showAlert("Selection Required", "Please select a Course first.", AlertType.WARNING);
            return;
        }

        try {
            // Load the new FXML file for the pop-up window
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/aast/aast_project/views/QuizCreationView.fxml"));
            Parent root = loader.load();
            QuizCreationController controller = loader.getController();

            // Set up the stage
            Stage stage = new Stage();
            stage.setTitle("Create/Edit Quiz Questions");
            stage.setScene(new Scene(root, 800, 750));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait(); // Wait for the pop-up to close

            // Retrieve result after pop-up closes
            QuizData newQuizData = controller.getResultQuizData();

            if (newQuizData != null) {
                // If the user clicked "Finalize & Save Quiz"
                currentQuizData = newQuizData;

                // Update main form fields for visual confirmation
                quizTitleField.setText(currentQuizData.title);
                quizPassingScoreField.setText(String.valueOf(currentQuizData.passingScore));

                showAlert("Quiz Data Loaded",
                        "Quiz data for '" + currentQuizData.title +
                                "' containing " + currentQuizData.questions.size() + " questions has been finalized. Click 'Save Lesson and Content' below to complete the process.",
                        AlertType.INFORMATION);
            }

        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Error", "Could not open Quiz Creation window. Check FXML path.", AlertType.ERROR);
        }
    }

    @FXML
    private void handleSaveLesson() {
        String lessonTitle = lessonTitleField.getText().trim();
        String orderText = lessonOrderField.getText().trim();

        if (selectedLesson != null) {
            showAlert("Unsupported Action", "Editing structured content is not yet supported. Please delete and re-create.", AlertType.WARNING);
            return;
        }

        // 1. Basic Validation
        if (lessonTitle.isEmpty() || orderText.isEmpty()) {
            showAlert("Input Error", "Lesson Title and Order are required.", AlertType.WARNING);
            return;
        }

        int order;
        try {
            order = Integer.parseInt(orderText);
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Order must be a number.", AlertType.WARNING);
            return;
        }

        // 2. Determine which content type is being created
        boolean isCreatingLecture = !lectureTitleField.getText().trim().isEmpty() || !lectureUrlField.getText().trim().isEmpty() || !lectureNotesArea.getText().isEmpty();
        // Check for Assignment content (Title or MaxGrade is sufficient)
        boolean isCreatingAssignment = !assignmentTitleField.getText().trim().isEmpty() || !assignmentMaxGradeField.getText().trim().isEmpty();

        // QUIZ LOGIC: Check if the QuizData object is set
        boolean isCreatingQuiz = currentQuizData != null;

        int contentCount = (isCreatingLecture ? 1 : 0) + (isCreatingAssignment ? 1 : 0) + (isCreatingQuiz ? 1 : 0);

        if (contentCount == 0) {
            showAlert("Input Error", "Please fill out at least one content tab (Lecture, Assignment, or Quiz).", AlertType.WARNING);
            return;
        }
        if (contentCount > 1) {
            showAlert("Input Error", "Only one content type (Lecture, Assignment, OR Quiz) is allowed per lesson.", AlertType.WARNING);
            return;
        }

        // 3. Transactional Insert
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            // A. Insert base Lesson record and get ID
            int newLessonId = insertLessonRecord(conn, lessonTitle, order, this.currentSelectedCourseId);

            if (newLessonId == -1) {
                throw new SQLException("Failed to retrieve new lesson ID after insertion.");
            }

            // B. Insert Content based on type
            if (isCreatingLecture) {
                insertLecture(conn, newLessonId);
            } else if (isCreatingAssignment) {
                insertAssignment(conn, newLessonId);
            } else if (isCreatingQuiz) {
                // MODIFIED: Pass currentQuizData object to the insert method
                int newQuizId = insertQuiz(conn, newLessonId, currentQuizData);

                // NEW: Insert all questions associated with the new quiz ID
                for (Question q : currentQuizData.questions) {
                    insertQuestion(conn, newQuizId, q);
                }
            }

            conn.commit();
            showAlert("Success", "Lesson and associated content saved successfully.", AlertType.INFORMATION);
            loadLessons();
            handleClearLessonForm();

        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            showAlert("Database Error", "Failed to save lesson/content: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        } finally {
            try {
                if (conn != null) conn.setAutoCommit(true);
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    // --- Lesson Content Insertion Helpers ---

    private int insertLessonRecord(Connection conn, String title, int order, int courseId) throws SQLException {
        int newLessonId = -1;
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_LESSON_RETURN_ID, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, title);
            pstmt.setInt(2, order);
            pstmt.setInt(3, courseId);

            if (pstmt.executeUpdate() > 0) {
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        newLessonId = rs.getInt(1);
                    }
                }
            }
            return newLessonId;
        }
    }

    private void insertLecture(Connection conn, int lessonId) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_LECTURE)) {
            pstmt.setString(1, lectureTitleField.getText().trim());
            pstmt.setString(2, lectureUrlField.getText().trim());
            pstmt.setString(3, lectureNotesArea.getText());
            pstmt.setInt(4, lessonId);

            pstmt.executeUpdate();
        }
    }

    private void insertAssignment(Connection conn, int lessonId) throws SQLException {
        String title = assignmentTitleField.getText().trim();
        String description = assignmentDescriptionArea.getText();
        LocalDate localDate = assignmentDueDate.getValue();
        int maxGrade;

        try {
            maxGrade = Integer.parseInt(assignmentMaxGradeField.getText().trim());
        } catch (NumberFormatException e) {
            throw new SQLException("Max Grade must be a number.", e);
        }

        LocalDateTime dueDateTime = (localDate != null) ? localDate.atTime(LocalTime.MAX) : null;

        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_ASSIGNMENT)) {
            pstmt.setString(1, title);
            pstmt.setString(2, description);
            // Convert LocalDateTime to SQL TIMESTAMP (Handle null case if dueDate is optional)
            pstmt.setObject(3, dueDateTime);
            pstmt.setInt(4, maxGrade);
            pstmt.setInt(5, lessonId);

            pstmt.executeUpdate();
        }
    }

    // MODIFIED: Now takes QuizData object
    private int insertQuiz(Connection conn, int lessonId, QuizData quizData) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_QUIZ, PreparedStatement.RETURN_GENERATED_KEYS)) {
            // Use data from the QuizData object
            pstmt.setString(1, quizData.title);
            pstmt.setInt(2, quizData.passingScore);
            pstmt.setInt(3, lessonId);

            pstmt.executeUpdate();

            try (ResultSet rs = pstmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Failed to retrieve new Quiz ID after insertion.");
                }
            }
        }
    }

    // NEW: Helper method to insert a single Question
    private void insertQuestion(Connection conn, int quizId, Question question) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(INSERT_QUESTION)) {
            pstmt.setString(1, question.getQuestionText());
            pstmt.setString(2, question.getOptionA());
            pstmt.setString(3, question.getOptionB());
            pstmt.setString(4, question.getOptionC());
            pstmt.setString(5, question.getOptionD());
            pstmt.setString(6, question.getCorrectAnswer());
            pstmt.setInt(7, quizId);

            pstmt.executeUpdate();
        }
    }

    private void editLesson(Lesson lesson) {
        selectedLesson = lesson;
        lessonTitleField.setText(lesson.getTitle());
        lessonOrderField.setText(String.valueOf(lesson.getOrder()));
        lessonSaveButton.setText("Update Lesson");

        showAlert("Warning", "Only Lesson core data can be selected for edit. Content data must be managed manually or deleted/re-created.", AlertType.WARNING);
    }

    private void deleteLessonConfirmation(Lesson lesson) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Delete Lesson");
        alert.setHeaderText("Confirm Deletion");
        alert.setContentText("Are you sure you want to delete the lesson: " + lesson.getTitle() + "? Deleting the lesson will also delete all associated Lecture/Assignment/Quiz content due to CASCADE rules.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteLesson(lesson.getId());
        }
    }

    private void deleteLesson(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_LESSON)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();

            showAlert("Success", "Lesson and all associated content deleted successfully.", AlertType.INFORMATION);
            loadLessons();

        } catch (SQLException e) {
            showAlert("Database Error", "Failed to delete lesson: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearLessonForm() {
        selectedLesson = null;
        currentQuizData = null; // Reset quiz state

        // Core Lesson Details
        lessonTitleField.clear();
        lessonOrderField.clear();
        lessonSaveButton.setText("Save Lesson and Content");

        // Lecture Tab
        lectureTitleField.clear();
        lectureUrlField.clear();
        lectureNotesArea.clear();
        loadVideo(null); // Clear WebView

        // Assignment Tab
        assignmentTitleField.clear();
        assignmentDueDate.setValue(null);
        assignmentMaxGradeField.clear();
        assignmentDescriptionArea.clear();

        // Quiz Tab (Update display fields)
        quizTitleField.setText("Set via 'Manage Quiz Questions' button");
        quizPassingScoreField.setText("Set via 'Manage Quiz Questions' button");
    }


    // --- Generic Utility Method ---

    private void showAlert(String title, String message, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}