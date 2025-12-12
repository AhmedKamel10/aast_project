package aast.aast_project.controllers;

import aast.aast_project.DatabaseConnection;
import aast.aast_project.model.Question;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class QuizQuestionManagerController {

    // ===============================================
    // FXML FIELDS (QUIZ QUESTION MANAGEMENT)
    // ===============================================
    @FXML private TableView<Question> quizQuestionTable;
    @FXML private TableColumn<Question, Integer> qqIdColumn;
    @FXML private TableColumn<Question, String> qqTextColumn;
    @FXML private TableColumn<Question, String> qqCorrectColumn;
    @FXML private TableColumn<Question, Void> qqActionsColumn;

    @FXML private TextArea questionTextArea;
    @FXML private TextField optionAField;
    @FXML private TextField optionBField;
    @FXML private TextField optionCField;
    @FXML private TextField optionDField;
    @FXML private ChoiceBox<String> correctAnswerChoiceBox;
    @FXML private Button saveQuestionButton;

    // ===============================================
    // CONTEXT & STATE
    // ===============================================
    private int currentSelectedQuizId = -1;
    private Question selectedQuestion = null;

    // --- SQL Queries ---
    private static final String SELECT_QUESTIONS = "SELECT id, string_questionText, char_correctAnswer, string_optionA, string_optionB, string_optionC, string_optionD FROM Question WHERE quiz_id = ?";
    private static final String INSERT_QUESTION = "INSERT INTO Question (string_questionText, string_optionA, string_optionB, string_optionC, string_optionD, char_correctAnswer, quiz_id) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_QUESTION = "UPDATE Question SET string_questionText = ?, string_optionA = ?, string_optionB = ?, string_optionC = ?, string_optionD = ?, char_correctAnswer = ? WHERE id = ?";
    private static final String DELETE_QUESTION = "DELETE FROM Question WHERE id = ?";


    @FXML
    public void initialize() {
        qqIdColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        qqTextColumn.setCellValueFactory(new PropertyValueFactory<>("questionText"));
        qqCorrectColumn.setCellValueFactory(new PropertyValueFactory<>("correctAnswer"));
        setupQuizQuestionActionsColumn();

        correctAnswerChoiceBox.getItems().addAll("A", "B", "C", "D");
        correctAnswerChoiceBox.setValue("A");
    }

    /**
     * Called by CourseManagerController to load questions for the current Quiz.
     * @param quizId The ID of the Quiz record in the Quiz table.
     */
    public void setQuizContext(int quizId) {
        this.currentSelectedQuizId = quizId;
        loadQuestions();
        handleClearQuestionForm();
    }

    /**
     * Loads quiz questions from the database for the current Quiz ID.
     */
    private void loadQuestions() {
        quizQuestionTable.getItems().clear();
        if (currentSelectedQuizId == -1) return;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(SELECT_QUESTIONS)) {

            pstmt.setInt(1, currentSelectedQuizId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    quizQuestionTable.getItems().add(new Question(
                            rs.getInt("id"),
                            rs.getString("string_questionText"),
                            rs.getString("string_optionA"),
                            rs.getString("string_optionB"),
                            rs.getString("string_optionC"),
                            rs.getString("string_optionD"),
                            rs.getString("char_correctAnswer")
                    ));
                }
            }
        } catch (SQLException e) {
            showAlert("Database Error", "Failed to load quiz questions: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void setupQuizQuestionActionsColumn() {
        qqActionsColumn.setCellFactory(tc -> new TableCell<>() {
            final Button editBtn = new Button("Edit");
            final Button deleteBtn = new Button("Delete");
            final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setOnAction(e -> {
                    Question question = getTableView().getItems().get(getIndex());
                    editQuestion(question);
                });

                deleteBtn.setOnAction(e -> {
                    Question question = getTableView().getItems().get(getIndex());
                    deleteQuestionConfirmation(question);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    @FXML
    private void handleSaveQuestion() {
        // ... (Validation logic) ...
        String questionText = questionTextArea.getText().trim();
        String optionA = optionAField.getText().trim();
        String optionB = optionBField.getText().trim();
        String optionC = optionCField.getText().trim();
        String optionD = optionDField.getText().trim();
        String correctAnswer = correctAnswerChoiceBox.getValue();

        if (currentSelectedQuizId == -1) {
            showAlert("Error", "Quiz must be created and saved as a Lesson before adding questions.", AlertType.WARNING);
            return;
        }

        if (questionText.isEmpty() || optionA.isEmpty() || optionB.isEmpty() || correctAnswer == null) {
            showAlert("Input Error", "Question text, options A and B, and the correct answer are required.", AlertType.WARNING);
            return;
        }

        if (selectedQuestion == null) {
            insertQuestion(questionText, optionA, optionB, optionC, optionD, correctAnswer);
        } else {
            updateQuestion(selectedQuestion.getId(), questionText, optionA, optionB, optionC, optionD, correctAnswer);
        }
    }

    private void insertQuestion(String qText, String optA, String optB, String optC, String optD, String cAnswer) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(INSERT_QUESTION)) {

            pstmt.setString(1, qText);
            pstmt.setString(2, optA);
            pstmt.setString(3, optB);
            pstmt.setString(4, optC.isEmpty() ? null : optC);
            pstmt.setString(5, optD.isEmpty() ? null : optD);
            pstmt.setString(6, cAnswer);
            pstmt.setInt(7, currentSelectedQuizId);

            pstmt.executeUpdate();
            showAlert("Success", "Question added successfully.", AlertType.INFORMATION);
            loadQuestions();
            handleClearQuestionForm();

        } catch (SQLException e) {
            showAlert("Database Error", "Failed to insert question: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void editQuestion(Question question) {
        selectedQuestion = question;
        questionTextArea.setText(question.getQuestionText());
        optionAField.setText(question.getOptionA());
        optionBField.setText(question.getOptionB());
        optionCField.setText(question.getOptionC() != null ? question.getOptionC() : "");
        optionDField.setText(question.getOptionD() != null ? question.getOptionD() : "");
        correctAnswerChoiceBox.setValue(question.getCorrectAnswer());
        saveQuestionButton.setText("Update Question");
    }

    private void updateQuestion(int id, String qText, String optA, String optB, String optC, String optD, String cAnswer) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(UPDATE_QUESTION)) {

            pstmt.setString(1, qText);
            pstmt.setString(2, optA);
            pstmt.setString(3, optB);
            pstmt.setString(4, optC.isEmpty() ? null : optC);
            pstmt.setString(5, optD.isEmpty() ? null : optD);
            pstmt.setString(6, cAnswer);
            pstmt.setInt(7, id);

            pstmt.executeUpdate();
            showAlert("Success", "Question updated successfully.", AlertType.INFORMATION);
            loadQuestions();
            handleClearQuestionForm();

        } catch (SQLException e) {
            showAlert("Database Error", "Failed to update question: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    private void deleteQuestionConfirmation(Question question) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Delete Question");
        alert.setHeaderText("Confirm Deletion");
        alert.setContentText("Are you sure you want to delete the question:\n\"" + question.getQuestionText() + "\"?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            deleteQuestion(question.getId());
        }
    }

    private void deleteQuestion(int id) {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(DELETE_QUESTION)) {

            pstmt.setInt(1, id);
            pstmt.executeUpdate();

            showAlert("Success", "Question deleted successfully.", AlertType.INFORMATION);
            loadQuestions();

        } catch (SQLException e) {
            showAlert("Database Error", "Failed to delete question: " + e.getMessage(), AlertType.ERROR);
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearQuestionForm() {
        selectedQuestion = null;
        questionTextArea.clear();
        optionAField.clear();
        optionBField.clear();
        optionCField.clear();
        optionDField.clear();
        correctAnswerChoiceBox.setValue("A");
        saveQuestionButton.setText("Save Question");
    }

    private void showAlert(String title, String content, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}