package aast.aast_project.model;

/**
 * Represents a Quiz container associated with a Lesson.
 */
public class Quiz {
    private int id;
    private String title;
    private int passingScore;
    private int lessonId;

    // --- Constructors ---
    public Quiz(int id, String title, int passingScore, int lessonId) {
        this.id = id;
        this.title = title;
        this.passingScore = passingScore;
        this.lessonId = lessonId;
    }

    public Quiz(String title, int passingScore, int lessonId) {
        this(-1, title, passingScore, lessonId);
    }

    // --- Getters and Setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public int getPassingScore() { return passingScore; }
    public void setPassingScore(int passingScore) { this.passingScore = passingScore; }

    public int getLessonId() { return lessonId; }
    public void setLessonId(int lessonId) { this.lessonId = lessonId; }
}