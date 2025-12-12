package aast.aast_project.model; // IMPORTANT: Update the package name

// This class represents a row in the 'Course' database table
public class Course {
    private int id;
    private String courseIdString;
    private String courseName;
    private String description;

    // Constructor for creating a new Course object
    public Course(int id, String courseIdString, String courseName, String description) {
        this.id = id;
        this.courseIdString = courseIdString;
        this.courseName = courseName;
        this.description = description;
    }

    // Getters (REQUIRED for TableView to read data)
    public int getId() { return id; }
    public String getCourseIdString() { return courseIdString; }
    public String getCourseName() { return courseName; }
    public String getDescription() { return description; }

    // Setters (Optional, but good practice)
    public void setId(int id) { this.id = id; }
    public void setCourseIdString(String courseIdString) { this.courseIdString = courseIdString; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public void setDescription(String description) { this.description = description; }
}