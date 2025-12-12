package aast.aast_project.model;

/**
 * Represents a specific lecture/media content associated with a Lesson.
 */
public class Lecture {
    private int id;
    private String title;
    private String url; // Video link, PDF path, etc.
    private String notes; // Detailed text or notes
    private int lessonId;

    // --- Constructors ---
    public Lecture(int id, String title, String url, String notes, int lessonId) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.notes = notes;
        this.lessonId = lessonId;
    }

    public Lecture(String title, String url, String notes, int lessonId) {
        this(-1, title, url, notes, lessonId); // Use -1 for a new unsaved object
    }

    // --- Getters and Setters (Required for property access and TableView) ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }
}