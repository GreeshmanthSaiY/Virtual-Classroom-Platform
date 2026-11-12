package com.virtualclassroom.service;

import com.virtualclassroom.dto.CourseDto;
import com.virtualclassroom.dto.DegreeDto;
import com.virtualclassroom.dto.SubjectDto;
import com.virtualclassroom.model.Course;
import com.virtualclassroom.model.Degree;
import com.virtualclassroom.model.Subject;
import com.virtualclassroom.repository.CourseRepository;
import com.virtualclassroom.repository.DegreeRepository;
import com.virtualclassroom.repository.SubjectRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CourseService {

    private final DegreeRepository degreeRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Autowired
    public CourseService(DegreeRepository degreeRepository, CourseRepository courseRepository, SubjectRepository subjectRepository, AuditLogService auditLogService) {
        this.degreeRepository = degreeRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.auditLogService = auditLogService;
    }

    public CourseService(DegreeRepository degreeRepository, CourseRepository courseRepository, SubjectRepository subjectRepository) {
        this(degreeRepository, courseRepository, subjectRepository, null);
    }


    @Transactional(readOnly = true)
    public List<DegreeDto> getAllDegrees() {
        return degreeRepository.findAll().stream()
                .map(this::toDegreeDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CourseDto> getCoursesByDegree(Integer degreeId) {
        return courseRepository.findByDegreeId(degreeId).stream()
                .map(this::toCourseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubjectDto> getSubjectsByCourseAndYear(Integer courseId, Integer year) {
        List<Subject> subjects = (year != null)
                ? subjectRepository.findByCourseIdAndYearLevel(courseId, year)
                : subjectRepository.findByCourseId(courseId);

        return subjects.stream().map(this::toSubjectDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubjectDto> getAllSubjects() {
        return subjectRepository.findAll().stream()
                .map(this::toSubjectDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public CourseDto archiveCourse(Integer courseId, com.virtualclassroom.model.User adminActor, String ipAddress) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        course.setArchived(true);
        Course updated = courseRepository.save(course);
        if (auditLogService != null && adminActor != null) {
            auditLogService.logAction(adminActor, "ARCHIVE_COURSE", "courses", courseId.toString(),
                    "Archived course: " + course.getName() + " (" + course.getCode() + ")", ipAddress);
        }
        return toCourseDto(updated);
    }

    @Transactional
    public CourseDto unarchiveCourse(Integer courseId, com.virtualclassroom.model.User adminActor, String ipAddress) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("Course not found"));
        course.setArchived(false);
        Course updated = courseRepository.save(course);
        if (auditLogService != null && adminActor != null) {
            auditLogService.logAction(adminActor, "UNARCHIVE_COURSE", "courses", courseId.toString(),
                    "Unarchived course: " + course.getName() + " (" + course.getCode() + ")", ipAddress);
        }
        return toCourseDto(updated);
    }

    public DegreeDto toDegreeDto(Degree degree) {
        return DegreeDto.builder()
                .id(degree.getId())
                .code(degree.getCode())
                .name(degree.getName())
                .durationYears(degree.getDurationYears())
                .courses(degree.getCourses() != null ? degree.getCourses().stream().map(this::toCourseDto).collect(Collectors.toList()) : List.of())
                .build();
    }

    public CourseDto toCourseDto(Course course) {
        return CourseDto.builder()
                .id(course.getId())
                .degreeId(course.getDegree() != null ? course.getDegree().getId() : null)
                .degreeCode(course.getDegree() != null ? course.getDegree().getCode() : null)
                .code(course.getCode())
                .name(course.getName())
                .archived(course.isArchived())
                .subjects(course.getSubjects() != null ? course.getSubjects().stream().map(this::toSubjectDto).collect(Collectors.toList()) : List.of())
                .build();
    }

    public SubjectDto toSubjectDto(Subject subject) {
        return SubjectDto.builder()
                .id(subject.getId())
                .courseId(subject.getCourse() != null ? subject.getCourse().getId() : null)
                .courseName(subject.getCourse() != null ? subject.getCourse().getName() : null)
                .code(subject.getCode())
                .title(subject.getTitle())
                .yearLevel(subject.getYearLevel())
                .build();
    }
}
