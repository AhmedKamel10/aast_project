package aast.aast_project.model;

import java.time.LocalDateTime;

public class SubmissionDetails {
    private int id;
    private int studentId;
    private int assignmentId;
    private String filePath;
    private LocalDateTime submissionDate;
    private Double score;
    private String feedback;

    // Constructor for creating a new submission
    public SubmissionDetails(int studentId, int assignmentId, String filePath) {
        this.studentId = studentId;
        this.assignmentId = assignmentId;
        this.filePath = filePath;
    }

    // Getters and Setters (omitted for brevity, but needed)
    // ...

    public String getFilePath() {
        return filePath;
    }

    public int getId() { return id; }
    public int getStudentId() { return studentId; }
    public int getAssignmentId() { return assignmentId; }
    public LocalDateTime getSubmissionDate() { return submissionDate; }
    public Double getScore() { return score; }
    public String getFeedback() { return feedback; }

    public void setId(int id) { this.id = id; }
    public void setStudentId(int studentId) { this.studentId = studentId; }
    public void setAssignmentId(int assignmentId) { this.assignmentId = assignmentId; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public void setSubmissionDate(LocalDateTime submissionDate) { this.submissionDate = submissionDate; }
    public void setScore(Double score) { this.score = score; }
    public void setFeedback(String feedback) { this.feedback = feedback; }
}