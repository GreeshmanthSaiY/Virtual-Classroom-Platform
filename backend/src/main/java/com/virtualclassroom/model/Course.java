package com.virtualclassroom.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "degree_id", nullable = false)
    private Degree degree;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Subject> subjects = new ArrayList<>();

    public Course() {}

    public Course(Integer id, Degree degree, String code, String name) {
        this.id = id;
        this.degree = degree;
        this.code = code;
        this.name = name;
    }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private Integer id;
        private Degree degree;
        private String code;
        private String name;

        public Builder id(Integer id) { this.id = id; return this; }
        public Builder degree(Degree degree) { this.degree = degree; return this; }
        public Builder code(String code) { this.code = code; return this; }
        public Builder name(String name) { this.name = name; return this; }
        public Course build() { return new Course(id, degree, code, name); }
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Degree getDegree() { return degree; }
    public void setDegree(Degree degree) { this.degree = degree; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public List<Subject> getSubjects() { return subjects; }
    public void setSubjects(List<Subject> subjects) { this.subjects = subjects; }
}
