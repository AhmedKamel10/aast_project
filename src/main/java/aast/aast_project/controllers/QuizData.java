package aast.aast_project.controllers;

import aast.aast_project.model.Question;
import java.util.List;

/**
 * Data Transfer Object (DTO) to hold the complete quiz structure
 * (header info + all questions) received from the QuizCreationController.
 */
public class QuizData {
    public String title;
    public int passingScore;
    public List<Question> questions;

    public QuizData(String title, int passingScore, List<Question> questions) {
        this.title = title;
        this.passingScore = passingScore;
        this.questions = questions;
    }
}