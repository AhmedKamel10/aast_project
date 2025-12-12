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
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.util.Duration;
import javafx.scene.layout.StackPane;
import javafx.scene.control.Slider;
import javafx.scene.control.ButtonBar;
import javafx.scene.text.Font;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StudentDashboardController {

    // ===============================================
    // FXML FIELDS
    // ===============================================
    @FXML private Button logoutButton;
    @FXML private Button coursesButton;
    @FXML private Button assignmentsButton;
    @FXML private Button gradesButton;

    // ===============================================
    // STATE
    // ===============================================
    private int studentId;

    // ===============================================
    // MEDIA PLAYER STATE
    // ===============================================
    private MediaPlayer mediaPlayer;
    private static final Pattern YT_PATTERN = Pattern.compile(
            "(?:youtube\\.com\\/watch\\?v=|youtu\\.be\\/|youtube\\.com\\/embed\\/)([\\w\\-]{11})"
    );
    private static final Pattern VIMEO_PATTERN = Pattern.compile(
            "vimeo\\.com\\/(\\d+)"
    );

    // ===============================================
    // DTO for Grade Display (UPDATED)
    // ===============================================

    // DTO for detailed assignment/quiz grade
    public static class StudentGrade {
        String courseName;
        String assignmentTitle;
        Float score; // Earned score
        int maxGrade; // Max possible score (NEW)
        String feedback;
        LocalDateTime submissionDate;

        public StudentGrade(String courseName, String assignmentTitle, Float score, int maxGrade, String feedback, LocalDateTime submissionDate) {
            this.courseName = courseName;
            this.assignmentTitle = assignmentTitle;
            this.score = score;
            this.maxGrade = maxGrade;
            this.feedback = feedback;
            this.submissionDate = submissionDate;
        }

        public String getCourseName() { return courseName; }
        public String getAssignmentTitle() { return assignmentTitle; }

        // Updated to display X / Y (Max Grade)
        public String getScoreDisplay() {
            return (score == null) ? "N/A (Ungraded)" : String.format("%.1f / %d", score, maxGrade);
        }
    }

    // NEW DTO for Course Summary View
    public static class CourseSummaryGrade {
        int courseId;
        String courseName;
        Float totalEarnedScore;
        Float totalMaxScore;

        public CourseSummaryGrade(int courseId, String courseName, Float totalEarnedScore, Float totalMaxScore) {
            this.courseId = courseId;
            this.courseName = courseName;
            this.totalEarnedScore = totalEarnedScore;
            this.totalMaxScore = totalMaxScore;
        }

        public int getCourseId() { return courseId; }
        public String getCourseName() { return courseName; }

        public String getScoreDisplay() {
            if (totalMaxScore == null || totalMaxScore == 0.0f) {
                return "N/A (No Graded Items)";
            }
            float percentage = (totalEarnedScore / totalMaxScore) * 100.0f;

            // Display accumulated score and the percentage
            return String.format("%.1f / %.1f (%.1f%%)", totalEarnedScore, totalMaxScore, percentage);
        }

        public float getPercentageScore() {
            if (totalMaxScore == null || totalMaxScore == 0.0f) {
                return 0.0f;
            }
            return (totalEarnedScore / totalMaxScore) * 100.0f;
        }
    }

    // ===============================================
    // SQL QUERIES (UPDATED)
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

    // NEW: Query to calculate the accumulated score (out of accumulated max grade) for a course
    private static final String SELECT_COURSE_ACCUMULATED_SUMMARY =
            "SELECT C.id AS courseId, C.string_courseName, " +
                    "SUM(S.float_score) AS total_earned_score, " +
                    "SUM(A.int_maxGrade) AS total_max_score " +
                    "FROM Course C " +
                    "JOIN Lesson L ON L.course_id = C.id " +
                    "JOIN Assignment A ON A.lesson_id = L.id " +
                    "JOIN Submission S ON S.assignment_id = A.id " +
                    "WHERE S.student_id = ? AND S.float_score IS NOT NULL " +
                    "GROUP BY C.id, C.string_courseName";

    // NEW: Query to load detailed grades for a specific course (includes max grade)
    private static final String SELECT_DETAILED_GRADES_BY_COURSE =
            "SELECT C.string_courseName, A.string_title AS assignmentTitle, " +
                    "A.int_maxGrade, S.float_score, S.string_feedback, S.datetime_submissionDate " +
                    "FROM Submission S " +
                    "JOIN Assignment A ON S.assignment_id = A.id " +
                    "JOIN Lesson L ON A.lesson_id = L.id " +
                    "JOIN Course C ON L.course_id = C.id " +
                    "WHERE S.student_id = ? AND C.id = ?";


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
        if (gradesButton != null) {
            gradesButton.setOnAction(e -> handleViewGrades());
        }

        Platform.runLater(this::handleMyCourses);
    }

    private void setCenterContent(javafx.scene.Node content) {
        if (coursesButton != null) {
            Node centerNode = coursesButton.getScene().lookup("#centerVBox");
            if (centerNode instanceof VBox parentVBox) {
                parentVBox.getChildren().clear();
                parentVBox.getChildren().add(content);
                VBox.setVgrow(content, Priority.ALWAYS);
            } else {
                System.err.println("Error: Could not find center content container. Please ensure the center VBox has fx:id=\"centerVBox\".");
            }
        }
    }

    // =========================================================================
    // MEDIA PLAYER METHODS
    // =========================================================================

    private boolean looksLikeDirectMediaUrl(String url) {
        if (url == null) return false;
        String u = url.toLowerCase();
        return u.endsWith(".mp4") || u.endsWith(".m4v") || u.endsWith(".mov") || u.endsWith(".mkv")
                || u.endsWith(".mp3") || u.endsWith(".aac") || u.endsWith(".wav")
                || u.contains(".m3u8") || u.contains(".mpd");
    }

    private String extractYoutubeId(String url) {
        if (url == null) return null;
        Matcher m = YT_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private String extractVimeoId(String url) {
        if (url == null) return null;
        Matcher m = VIMEO_PATTERN.matcher(url);
        return m.find() ? m.group(1) : null;
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeHtmlAttribute(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }

    private String buildEmbedHtml(String url) {
        String ytId = extractYoutubeId(url);
        if (ytId != null) {
            String embedUrl = "https://www.youtube.com/embed/" + ytId;
            String safeUrl = escapeHtmlAttribute(embedUrl);
            return "<html>" +
                    "<head>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "<style>" +
                    "html, body { margin:0; padding:0; height:100%; background:#111; }" +
                    "iframe { border:0; width:100%; height:100%; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<iframe src='" + safeUrl + "' allow='accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share' allowfullscreen></iframe>" +
                    "</body>" +
                    "</html>";
        }

        String vimeoId = extractVimeoId(url);
        if (vimeoId != null) {
            String embedUrl = "https://player.vimeo.com/video/" + vimeoId;
            String safeUrl = escapeHtmlAttribute(embedUrl);
            return "<html>" +
                    "<head>" +
                    "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                    "<style>" +
                    "html, body { margin:0; padding:0; height:100%; background:#111; }" +
                    "iframe { border:0; width:100%; height:100%; }" +
                    "</style>" +
                    "</head>" +
                    "<body>" +
                    "<iframe src='" + safeUrl + "' allow='autoplay; fullscreen; picture-in-picture' allowfullscreen></iframe>" +
                    "</body>" +
                    "</html>";
        }

        // For non-embedded URLs, show a safe link
        return "<html>" +
                "<head>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "html, body { margin:0; padding:0; height:100%; background:#111; }" +
                ".wrap { height:100%; display:flex; align-items:center; justify-content:center; }" +
                "a { color:#4FC3F7; font-family:Arial, sans-serif; font-size:14px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='wrap'>" +
                "<a target='_blank' href='" + escapeHtml(url) + "'>Open this link</a>" +
                "</div>" +
                "</body>" +
                "</html>";
    }

    private void showVideoPlayerDialog(String url, String title, String notes) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Lecture Video: " + title);
        dialog.setHeaderText(null);

        ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(closeButtonType);

        // Create video player container
        VBox container = new VBox(10);
        container.setPadding(new Insets(10));
        container.setStyle("-fx-background-color: #111;");

        // Video display area
        StackPane videoPane = new StackPane();
        videoPane.setPrefSize(800, 450);
        videoPane.setStyle("-fx-background-color: #000;");

        MediaView mediaView = new MediaView();
        mediaView.setPreserveRatio(true);
        mediaView.setFitWidth(800);
        mediaView.setFitHeight(450);

        WebView webView = new WebView();
        webView.setPrefSize(800, 450);

        // Controls
        HBox controls = new HBox(10);
        controls.setStyle("-fx-background-color: #222; -fx-padding: 10;");

        Button playPauseButton = new Button("▶ Play");
        Button stopButton = new Button("⏹ Stop");
        Slider volumeSlider = new Slider(0, 1, 0.7);
        volumeSlider.setPrefWidth(100);
        Label volumeLabel = new Label("Volume:");

        // Notes area
        TextArea notesArea = new TextArea(notes);
        notesArea.setEditable(false);
        notesArea.setWrapText(true);
        notesArea.setPrefRowCount(4);
        notesArea.setStyle("-fx-font-family: 'Segoe UI'; -fx-font-size: 12px;");

        // Add to video pane
        videoPane.getChildren().addAll(mediaView, webView);

        // Add to controls
        controls.getChildren().addAll(playPauseButton, stopButton, volumeLabel, volumeSlider);

        // Add to container
        container.getChildren().addAll(videoPane, controls);

        if (notes != null && !notes.trim().isEmpty() && !notes.equals("No notes available.")) {
            Label notesLabel = new Label("Lecture Notes:");
            notesLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: white; -fx-font-size: 14px;");
            container.getChildren().addAll(notesLabel, notesArea);
        }

        dialog.getDialogPane().setContent(container);
        dialog.getDialogPane().setPrefSize(850, 600);

        // Set up media player
        if (looksLikeDirectMediaUrl(url)) {
            // Use MediaPlayer for direct media
            webView.setVisible(false);
            mediaView.setVisible(true);

            try {
                Media media = new Media(url);
                mediaPlayer = new MediaPlayer(media);
                mediaView.setMediaPlayer(mediaPlayer);

                // Set initial volume
                mediaPlayer.setVolume(volumeSlider.getValue());

                // Control handlers
                playPauseButton.setOnAction(e -> {
                    if (mediaPlayer.getStatus() == MediaPlayer.Status.PLAYING) {
                        mediaPlayer.pause();
                        playPauseButton.setText("▶ Play");
                    } else {
                        mediaPlayer.play();
                        playPauseButton.setText("⏸ Pause");
                    }
                });

                stopButton.setOnAction(e -> {
                    mediaPlayer.stop();
                    playPauseButton.setText("▶ Play");
                });

                volumeSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                    mediaPlayer.setVolume(newVal.doubleValue());
                });

                // Auto-play
                mediaPlayer.play();
                playPauseButton.setText("⏸ Pause");

            } catch (Exception e) {
                // Fallback to WebView if MediaPlayer fails
                webView.setVisible(true);
                mediaView.setVisible(false);
                webView.getEngine().loadContent(buildEmbedHtml(url));
                playPauseButton.setDisable(true);
                stopButton.setDisable(true);
            }
        } else {
            // Use WebView for embedded content
            webView.setVisible(true);
            mediaView.setVisible(false);
            webView.getEngine().loadContent(buildEmbedHtml(url));
            playPauseButton.setDisable(true);
            stopButton.setDisable(true);
        }

        // Clean up on close
        dialog.setOnHidden(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
        });

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.setOnCloseRequest(e -> {
            if (mediaPlayer != null) {
                mediaPlayer.stop();
                mediaPlayer.dispose();
                mediaPlayer = null;
            }
        });

        dialog.showAndWait();
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
        showCourseSummaryGrades();
    }

    // =========================================================================
    // GRADING LOGIC (UPDATED SECTION FOR SUMMARY/DETAIL VIEW)
    // =========================================================================

    /**
     * Loads the student's overall grades per course and displays the summary.
     */
    private void showCourseSummaryGrades() {
        VBox contentBox = new VBox(15);
        contentBox.setPadding(new Insets(20));

        Label header = new Label("Course Final Grades");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2C3E50;");
        contentBox.getChildren().add(header);

        List<CourseSummaryGrade> summaries = loadCourseSummaryGrades();

        if (summaries.isEmpty()) {
            contentBox.getChildren().add(new Label("No graded assignments found in your enrolled courses."));
        } else {
            for (CourseSummaryGrade summary : summaries) {
                HBox summaryRow = createSummaryGradeRow(summary);
                contentBox.getChildren().add(summaryRow);
            }
        }
        setCenterContent(contentBox);
    }

    /**
     * Executes the SQL query to calculate the accumulated score for each course.
     */
    private List<CourseSummaryGrade> loadCourseSummaryGrades() {
        List<CourseSummaryGrade> summaries = new ArrayList<>();
        if (studentId <= 0) return summaries;

        try (Connection conn = DatabaseConnection.getConnection();
             // Use the new accumulated summary query
             PreparedStatement pstmt = conn.prepareStatement(SELECT_COURSE_ACCUMULATED_SUMMARY)) {

            pstmt.setInt(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Fetch the two new summed columns
                    Float totalEarnedScore = rs.getObject("total_earned_score", Float.class);
                    Float totalMaxScore = rs.getObject("total_max_score", Float.class);

                    summaries.add(new CourseSummaryGrade(
                            rs.getInt("courseId"),
                            rs.getString("string_courseName"),
                            totalEarnedScore,
                            totalMaxScore
                    ));
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load course summary grades: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
        return summaries;
    }

    /**
     * Creates the HBox row for the overall course grade summary, including the progress bar.
     */
    private HBox createSummaryGradeRow(CourseSummaryGrade summary) {
        Label courseLabel = new Label("📚 " + summary.getCourseName());
        courseLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");

        // Calculate progress bar value and color
        float percentage = summary.getPercentageScore();
        String color;
        double progress = percentage / 100.0;

        if (progress == 0.0f && summary.totalMaxScore == null) {
            color = "#7F8C8D"; // Grey for no graded items
        } else if (percentage >= 90) {
            color = "#2ECC71"; // Green (A)
        } else if (percentage >= 80) {
            color = "#3498DB"; // Blue (B)
        } else if (percentage >= 70) {
            color = "#F39C12"; // Orange (C)
        } else {
            color = "#C0392B"; // Red (F)
        }

        // 1. Progress Bar (Visual Indicator)
        ProgressBar progressBar = new ProgressBar(progress);
        progressBar.setPrefWidth(200);
        progressBar.setPrefHeight(15);
        // Custom style for progress color (Note: This might require external CSS for full control in some environments)
        // For simplicity, we use the accent color approach which often works.
        progressBar.setStyle("-fx-accent: " + color + ";");

        // 2. Course Details Box (Name + Progress Bar)
        VBox detailsBox = new VBox(5, courseLabel, progressBar);
        VBox.setVgrow(detailsBox, Priority.ALWAYS);

        // 3. Score Label (Text Indicator: X / Y (Z%))
        Label scoreLabel = new Label(summary.getScoreDisplay());
        scoreLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");

        // 4. Details Button
        Button detailsButton = new Button("View Details");
        detailsButton.setOnAction(e -> showDetailedGradesForCourse(summary));
        detailsButton.setStyle("-fx-background-color: #3498DB; -fx-text-fill: white;");

        HBox row = new HBox(20);
        row.setPadding(new Insets(15));
        row.setStyle("-fx-border-color: #DDD; -fx-border-width: 0 0 1 0; -fx-background-color: #F8F8F8;");

        // Layout: Details Box (Name + Bar) | Spacer | Score | Button
        row.getChildren().addAll(detailsBox, new Region(), scoreLabel, detailsButton);
        HBox.setHgrow(row.getChildren().get(1), Priority.ALWAYS);

        return row;
    }

    /**
     * Shows a dialog with the detailed assignment grades for a specific course.
     */
    private void showDetailedGradesForCourse(CourseSummaryGrade summary) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Detailed Grades");
        dialog.setHeaderText("Assignments and Quizzes for: " + summary.getCourseName());
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);

        VBox detailLayout = new VBox(10);
        detailLayout.setPadding(new Insets(15));

        // Header for the score
        Label summaryHeader = new Label("Overall Course Grade: " + summary.getScoreDisplay());
        summaryHeader.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-padding: 0 0 10 0;");
        detailLayout.getChildren().add(summaryHeader);

        // --- Load Details ---
        List<StudentGrade> detailedGrades = loadDetailedGradesByCourse(summary.getCourseId());

        if (detailedGrades.isEmpty()) {
            detailLayout.getChildren().add(new Label("No submissions found for this course."));
        } else {
            // Create headers for the list
            HBox detailHeader = new HBox(50, new Label("Assignment Title"), new Region(), new Label("Score (Earned/Max)"));
            detailHeader.setStyle("-fx-font-weight: bold; -fx-padding: 5 0; -fx-border-width: 0 0 2 0; -fx-border-color: #AAA;");
            HBox.setHgrow(detailHeader.getChildren().get(1), Priority.ALWAYS);
            detailLayout.getChildren().add(detailHeader);

            for (StudentGrade grade : detailedGrades) {
                HBox detailRow = createDetailedGradeRow(grade);
                detailLayout.getChildren().add(detailRow);
            }
        }

        dialog.getDialogPane().setContent(detailLayout);
        dialog.getDialogPane().setPrefWidth(650);
        dialog.getDialogPane().setPrefHeight(600);
        dialog.showAndWait();
    }

    /**
     * Executes a query to load all submission details (grades, feedback, maxGrade) for a specific course.
     */
    private List<StudentGrade> loadDetailedGradesByCourse(int courseId) {
        List<StudentGrade> grades = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_DETAILED_GRADES_BY_COURSE)) {

            pstmt.setInt(1, studentId);
            pstmt.setInt(2, courseId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Float score = rs.getObject("float_score", Float.class);
                    LocalDateTime submissionDate = rs.getTimestamp("datetime_submissionDate").toLocalDateTime();
                    int maxGrade = rs.getInt("int_maxGrade"); // NEW: Fetch max grade

                    grades.add(new StudentGrade(
                            rs.getString("string_courseName"),
                            rs.getString("assignmentTitle"),
                            score,
                            maxGrade, // Pass max grade
                            rs.getString("string_feedback"),
                            submissionDate
                    ));
                }
            }
        } catch (SQLException e) {
            System.err.println("Failed to load detailed grades: " + e.getMessage());
        }
        return grades;
    }

    /**
     * Creates the HBox row for an individual assignment detail (used in the dialog).
     */
    private HBox createDetailedGradeRow(StudentGrade grade) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        Label assignmentLabel = new Label("📝 " + grade.getAssignmentTitle());
        assignmentLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label scoreLabel = new Label(grade.getScoreDisplay());
        scoreLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: " + (grade.score == null ? "#FFC300" : "#2ECC71") + "; -fx-font-weight: bold;");

        VBox detailsBox = new VBox(5,
                assignmentLabel,
                new Label("Submitted: " + grade.submissionDate.format(formatter))
        );

        Button viewDetailsButton = new Button("View Feedback");
        viewDetailsButton.setOnAction(e -> showFeedbackDialog(grade));

        HBox row = new HBox(20);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-border-color: #EEE; -fx-border-width: 0 0 1 0; -fx-background-color: #FFFFFF;");

        // Layout: Details (left) | Spacer | Score (center) | Button (right)
        HBox.setHgrow(detailsBox, Priority.ALWAYS);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS); // Push elements apart

        row.getChildren().addAll(detailsBox, spacer, scoreLabel, viewDetailsButton);

        return row;
    }

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

    private void handleSubmission(int assignmentId, String filePath) {
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

        // Check if we have a valid URL for video playback
        if (url != null && !url.trim().isEmpty() && !url.equals("No URL provided.")) {
            // Show video player dialog instead of just showing the link
            showVideoPlayerDialog(url, lesson.getTitle(), notes);
        } else {
            // Fallback to the old dialog for non-video content
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
}