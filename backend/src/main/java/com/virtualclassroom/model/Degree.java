package com.virtualclassroom.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "degrees")
public class Degree {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "duration_years", nullable = false)
    private Integer durationYears;

    @OneToMany(mappedBy = "degree", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Course> courses = new ArrayList<>();

    public Degree() {}

    public Degree(Integer id, String code, String name, Integer durationYears) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.durationYears = durationYears;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer id;
        private String code;
        private String name;
        private Integer durationYears;

        public Builder id(Integer id) { this.id = id; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Builder durationYears(Integer durationYears) { this.durationYears = durationYears; return this; }
        public Degree build() { return new Degree(id, code, name, durationYears); }
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getDurationYears() { return durationYears; }
    public void setDurationYears(Integer durationYears) { this.durationYears = durationYears; }
    public List<Course> getCourses() { return courses; }
    public void setCourses(List<Course> courses) { this.courses = courses; }
}
