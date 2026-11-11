package com.virtualclassroom.dto;


import java.util.List;

public class DegreeDto {

    private Integer id;
    private String code;
    private String name;
    private Integer durationYears;
    private List<CourseDto> courses;

    public DegreeDto() {}

    public DegreeDto(Integer id, String code, String name, Integer durationYears, List<CourseDto> courses) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.durationYears = durationYears;
        this.courses = courses;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDurationYears() { return durationYears; }
    public void setDurationYears(Integer durationYears) { this.durationYears = durationYears; }
    public List<CourseDto> getCourses() { return courses; }
    public void setCourses(List<CourseDto> courses) { this.courses = courses; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Integer id;
        private String code;
        private String name;
        private Integer durationYears;
        private List<CourseDto> courses;

        public Builder id(Integer id) {
            this.id = id;
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
        public Builder durationYears(Integer durationYears) {
            this.durationYears = durationYears;
            return this;
        }
        public Builder courses(List<CourseDto> courses) {
            this.courses = courses;
            return this;
        }

        public DegreeDto build() {
            return new DegreeDto(id, code, name, durationYears, courses);
        }
    }
}
