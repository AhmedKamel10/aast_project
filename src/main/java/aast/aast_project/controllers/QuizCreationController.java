package aast.aast_project.controllers;

import aast.aast_project.model.Question;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.ArrayList; // Needed for the Question model
import java.util.Optional;


public class QuizCreationController {

    // FXML fields for Quiz Core Details
    @FXML private TextField quizTitleField;
    @FXML private TextField quizPassingScoreField;

    // FXML fields for Question Management
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

    // State
    // NOTE: The ID here is temporary for display; the DB handles final IDs.
    private int nextQuestionId = 1;
    private Question selectedQuestion = null;
    private QuizData resultQuizData = null; // Starts as null, set on finalization


    @FXML
    public void initialize() {
        // Table columns need to map to Question model getters (getTitle, getCorrectAnswer, etc.)
        qqIdColumn.setCellValueFactory(data -> new javafx.beans.property.SimpleIntegerProperty(data.getValue().getId()).asObject());
        qqTextColumn.setCellValueFactory(new PropertyValueFactory<>("questionText"));
        qqCorrectColumn.setCellValueFactory(new PropertyValueFactory<>("correctAnswer"));
        setupQuizQuestionActionsColumn();

        correctAnswerChoiceBox.getItems().addAll("A", "B", "C", "D");
        correctAnswerChoiceBox.setValue("A");

        // Initialize Table View items
        quizQuestionTable.setItems(javafx.collections.FXCollections.observableArrayList(new ArrayList<>()));
    }

    // ===============================================
    // QUESTION MANAGEMENT LOGIC
    // ===============================================

    @FXML
    private void handleSaveQuestion() {
        // 1. Validation
        String title = questionTextArea.getText().trim();
        String optA = optionAField.getText().trim();
        String optB = optionBField.getText().trim();
        String optC = optionCField.getText().trim();
        String optD = optionDField.getText().trim();
        String correct = correctAnswerChoiceBox.getValue();

        if (title.isEmpty() || optA.isEmpty() || optB.isEmpty() || correct == null) {
            showAlert("Input Error", "Question text, options A and B, and the correct answer are required.", AlertType.WARNING);
            return;
        }

        // Use -1 for the ID if it's new, as the database will generate the real ID
        // The display logic needs to be updated if you want to show temporary incrementing IDs
        int questionIdToUse = selectedQuestion != null ? selectedQuestion.getId() : -1;

        // 2. Create/Update Question Object
        Question newOrUpdatedQuestion = new Question(
                questionIdToUse, // Use the real DB ID or -1 (or nextQuestionId for temporary UI list management)
                title, optA, optB, optC, optD, correct
        );

        if (selectedQuestion == null) {
            // INSERT: For UI management, we can still use nextQuestionId for temporary display
            // NOTE: When passing to DB, the DB ignores this ID.
            Question questionForTable = new Question(nextQuestionId, title, optA, optB, optC, optD, correct);
            quizQuestionTable.getItems().add(questionForTable);
            nextQuestionId++;
        } else {
            // UPDATE: Replace the old object in the list
            int index = quizQuestionTable.getItems().indexOf(selectedQuestion);
            if (index != -1) {
                // Ensure we use the correct temporary ID so the table display doesn't break
                Question questionForTable = new Question(selectedQuestion.getId(), title, optA, optB, optC, optD, correct);
                quizQuestionTable.getItems().set(index, questionForTable);
            }
        }

        quizQuestionTable.refresh();
        handleClearQuestionForm();
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

    private void deleteQuestionConfirmation(Question question) {
        Alert alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Delete Question");
        alert.setHeaderText("Confirm Deletion");
        alert.setContentText("Are you sure you want to delete the question:\n\"" + question.getQuestionText() + "\"?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            quizQuestionTable.getItems().remove(question);
            handleClearQuestionForm();
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

    private void setupQuizQuestionActionsColumn() {
        qqActionsColumn.setCellFactory(tc -> new TableCell<>() {
            final Button editBtn = new Button("Edit");
            final Button deleteBtn = new Button("Delete");
            final HBox pane = new HBox(5, editBtn, deleteBtn);

            {
                editBtn.setOnAction(e -> editQuestion(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> deleteQuestionConfirmation(getTableView().getItems().get(getIndex())));
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
    }

    // ===============================================
    // WINDOW ACTIONS
    // ===============================================

    @FXML
    private void handleFinalizeQuiz() {
        // 1. Validate Core Quiz Details
        String title = quizTitleField.getText().trim();
        int passingScore;
        try {
            passingScore = Integer.parseInt(quizPassingScoreField.getText().trim());
        } catch (NumberFormatException e) {
            showAlert("Input Error", "Please enter a valid number for Passing Score.", AlertType.WARNING);
            return;
        }

        if (title.isEmpty()) {
            showAlert("Input Error", "Quiz Title is required.", AlertType.WARNING);
            return;
        }

        // 2. Validate Questions
        if (quizQuestionTable.getItems().isEmpty()) {
            showAlert("Input Error", "Quiz must contain at least one question.", AlertType.WARNING);
            return;
        }

        // 3. Package Data and Close

        // *** CRITICAL FIX APPLIED HERE ***
        // Use the correct constructor: QuizData(title, score, List<Question>)
        resultQuizData = new QuizData(
                title,
                passingScore,
                new ArrayList<>(quizQuestionTable.getItems()) // Pass the list of questions
        );

        closeWindow();
    }

    @FXML
    private void handleCancel() {
        // resultQuizData remains null, indicating cancellation
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) quizTitleField.getScene().getWindow();
        stage.close();
    }

    // Getter for the result data (used by main controller)
    public QuizData getResultQuizData() {
        return resultQuizData;
    }

    private void showAlert(String title, String content, AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}