package aast.aast_project.model;

import java.time.LocalDateTime;

/**
 * A simple data transfer object (DTO) to hold assignment details retrieved 
 * specifically for display in the student dashboard's assignment list.
 */
public class AssignmentDetails {

    private final String title;
    private final LocalDateTime dueDate;
    private final String lessonTitle;
    private final String courseName;

    public AssignmentDetails(String title, LocalDateTime dueDate, String lessonTitle, String courseName) {
        this.title = title;
        this.dueDate = dueDate;
        this.lessonTitle = lessonTitle;
        this.courseName = courseName;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public String getLessonTitle() {
        return lessonTitle;
    }

    public String getCourseName() {
        return courseName;
    }
}