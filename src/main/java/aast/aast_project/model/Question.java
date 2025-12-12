package aast.aast_project.model;

import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;

public class Question {
    private final SimpleIntegerProperty id;
    private final SimpleStringProperty questionText;
    private SimpleStringProperty optionA;
    private SimpleStringProperty optionB;
    private SimpleStringProperty optionC;
    private SimpleStringProperty optionD;
    private final SimpleStringProperty correctAnswer;

    // Constructor used for TableView loading (minimal data)
    public Question(int id, String questionText, String correctAnswer) {
        this.id = new SimpleIntegerProperty(id);
        this.questionText = new SimpleStringProperty(questionText);
        this.correctAnswer = new SimpleStringProperty(correctAnswer);
    }

    // Constructor used for full data retrieval (e.g., for editing)
    public Question(int id, String questionText, String optionA, String optionB, String optionC, String optionD, String correctAnswer) {
        this(id, questionText, correctAnswer);
        this.optionA = new SimpleStringProperty(optionA);
        this.optionB = new SimpleStringProperty(optionB);
        this.optionC = new SimpleStringProperty(optionC);
        this.optionD = new SimpleStringProperty(optionD);
    }

    // Getters for TableView PropertyValueFactory
    public int getId() { return id.get(); }
    public SimpleIntegerProperty idProperty() { return id; }

    public String getQuestionText() { return questionText.get(); }
    public SimpleStringProperty questionTextProperty() { return questionText; }

    public String getCorrectAnswer() { return correctAnswer.get(); }
    public SimpleStringProperty correctAnswerProperty() { return correctAnswer; }

    // Getters for Options (used for populating edit form)
    public String getOptionA() { return optionA != null ? optionA.get() : null; }
    public String getOptionB() { return optionB != null ? optionB.get() : null; }
    public String getOptionC() { return optionC != null ? optionC.get() : null; }
    public String getOptionD() { return optionD != null ? optionD.get() : null; }
}