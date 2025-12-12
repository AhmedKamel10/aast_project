package aast.aast_project.model;

import java.time.LocalDateTime;

/**
 * Represents an assignment task associated with a Lesson.
 */
public class Assignment {
    private int id;
    private String title;
    private String description;
    private LocalDateTime dueDate;
    private int maxGrade;
    private int lessonId;

    // --- Constructors ---
    public Assignment(int id, String title, String description, LocalDateTime dueDate, int maxGrade, int lessonId) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.dueDate = dueDate;
        this.maxGrade = maxGrade;
        this.lessonId = lessonId;
    }

    public Assignment(String title, String description, LocalDateTime dueDate, int maxGrade, int lessonId) {
        this(-1, title, description, dueDate, maxGrade, lessonId);
    }

    // --- Getters and Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime dueDate) { this.dueDate = dueDate; }

    public int getMaxGrade() { return maxGrade; }
    public void setMaxGrade(int maxGrade) { this.maxGrade = maxGrade; }

    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }
}