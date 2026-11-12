package com.virtualclassroom.dto;


import java.util.List;

public class CourseDto {

    private Integer id;
    private Integer degreeId;
    private String degreeCode;
    private String code;
    private String name;
    private boolean archived;
    private List<SubjectDto> subjects;

    public CourseDto() {}

    public CourseDto(Integer id, Integer degreeId, String degreeCode, String code, String name, boolean archived, List<SubjectDto> subjects) {
        this.id = id;
        this.degreeId = degreeId;
        this.degreeCode = degreeCode;
        this.code = code;
        this.name = name;
        this.archived = archived;
        this.subjects = subjects;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getDegreeId() { return degreeId; }
    public void setDegreeId(Integer degreeId) { this.degreeId = degreeId; }
    public String getDegreeCode() { return degreeCode; }
    public void setDegreeCode(String degreeCode) { this.degreeCode = degreeCode; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }
    public List<SubjectDto> getSubjects() { return subjects; }
    public void setSubjects(List<SubjectDto> subjects) { this.subjects = subjects; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer id;
        private Integer degreeId;
        private String degreeCode;
        private String code;
        private String name;
        private boolean archived;
        private List<SubjectDto> subjects;

        public Builder id(Integer id) {
            this.id = id;
            return this;
        }
        public Builder degreeId(Integer degreeId) {
            this.degreeId = degreeId;
            return this;
        }
        public Builder degreeCode(String degreeCode) {
            this.degreeCode = degreeCode;
            return this;
        }
        public Builder code(String code) {
            this.code = code;
            return this;
        }
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        public Builder archived(boolean archived) {
            this.archived = archived;
            return this;
        }
        public Builder subjects(List<SubjectDto> subjects) {
            this.subjects = subjects;
            return this;
        }

        public CourseDto build() {
            return new CourseDto(id, degreeId, degreeCode, code, name, archived, subjects);
        }
    }
}
