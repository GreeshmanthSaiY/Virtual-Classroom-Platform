package com.virtualclassroom.dto;


public class SubjectDto {

    private Integer id;
    private Integer courseId;
    private String courseName;
    private String code;
    private String title;
    private Integer yearLevel;
    private Integer semester;
    private Integer credits;
    private String description;

    public SubjectDto() {}

    public SubjectDto(Integer id, Integer courseId, String courseName, String code, String title, Integer yearLevel) {
        this.id = id;
        this.courseId = courseId;
        this.courseName = courseName;
        this.code = code;
        this.title = title;
        this.yearLevel = yearLevel;
        this.semester = 1;
        this.credits = 3;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getCourseId() { return courseId; }
    public void setCourseId(Integer courseId) { this.courseId = courseId; }
    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public Integer getYearLevel() { return yearLevel; }
    public void setYearLevel(Integer yearLevel) { this.yearLevel = yearLevel; }
    public Integer getSemester() { return semester; }
    public void setSemester(Integer semester) { this.semester = semester; }
    public Integer getCredits() { return credits; }
    public void setCredits(Integer credits) { this.credits = credits; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer id;
        private Integer courseId;
        private String courseName;
        private String code;
        private String title;
        private Integer yearLevel;
        private Integer semester = 1;
        private Integer credits = 3;
        private String description;

        public Builder id(Integer id) {
            this.id = id;
            return this;
        }
        public Builder courseId(Integer courseId) {
            this.courseId = courseId;
            return this;
        }
        public Builder courseName(String courseName) {
            this.courseName = courseName;
            return this;
        }
        public Builder code(String code) {
            this.code = code;
            return this;
        }
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        public Builder yearLevel(Integer yearLevel) {
            this.yearLevel = yearLevel;
            return this;
        }
        public Builder semester(Integer semester) {
            this.semester = semester;
            return this;
        }
        public Builder credits(Integer credits) {
            this.credits = credits;
            return this;
        }
        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public SubjectDto build() {
            SubjectDto dto = new SubjectDto(id, courseId, courseName, code, title, yearLevel);
            dto.setSemester(semester != null ? semester : 1);
            dto.setCredits(credits != null ? credits : 3);
            dto.setDescription(description);
            return dto;
        }
    }
}
