package aast.aast_project.model;

/**
 * Represents a Lesson within a Course.
 * This model is updated to handle the new modular content structure
 * by including a temporary 'contentType' field for display purposes.
 */
public class Lesson {
    private int id;
    private String title;
    private String content; // Retained for compatibility but mostly unused in new structure
    private int order;
    private int courseId;

    // NEW FIELD: Used by CourseManagerController to display "Lecture", "Assignment", etc.
    private String contentType;

    // --- Constructor ---

    public Lesson(int id, String title, String content, int order, int courseId) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.order = order;
        this.courseId = courseId;
    }

    // --- Getters and Setters for all required fields ---

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    // NOTE: getContent() is retained for TableView compatibility
    // but the controller now passes 'null' when loading lessons.
    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public int getCourseId() {
        return courseId;
    }

    public void setCourseId(int courseId) {
        this.courseId = courseId;
    }

    // -----------------------------------------------------------------
    // FIX FOR "cannot find symbol: method getContentType()"
    // -----------------------------------------------------------------

    /**
     * Getter for the dynamically determined content type (Lecture, Assignment, Quiz, Empty).
     * This is used by the Lesson TableView.
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Setter for the content type, determined in the CourseManagerController's
     * loadLessons() method using database queries.
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}