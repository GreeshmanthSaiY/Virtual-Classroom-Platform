package com.virtualclassroom.service;

import com.virtualclassroom.dto.CourseDto;
import com.virtualclassroom.dto.StudyMaterialDto;
import com.virtualclassroom.dto.SubjectDto;
import com.virtualclassroom.dto.VideoLectureDto;
import com.virtualclassroom.model.Course;
import com.virtualclassroom.model.StudentProfile;
import com.virtualclassroom.model.Subject;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.StudentProfileRepository;
import com.virtualclassroom.repository.StudyMaterialRepository;
import com.virtualclassroom.repository.SubjectRepository;
import com.virtualclassroom.repository.VideoLectureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentService {

    private final StudentProfileRepository studentProfileRepository;
    private final SubjectRepository subjectRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final VideoLectureRepository videoLectureRepository;
    private final CourseService courseService;

    public StudentService(StudentProfileRepository studentProfileRepository, SubjectRepository subjectRepository, StudyMaterialRepository studyMaterialRepository, VideoLectureRepository videoLectureRepository, CourseService courseService) {
        this.studentProfileRepository = studentProfileRepository;
        this.subjectRepository = subjectRepository;
        this.studyMaterialRepository = studyMaterialRepository;
        this.videoLectureRepository = videoLectureRepository;
        this.courseService = courseService;
    }


    @Transactional(readOnly = true)
    public CourseDto getEnrolledCourse(Long studentUserId) {
        StudentProfile profile = studentProfileRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found"));

        Course course = profile.getCourse();
        return course != null ? courseService.toCourseDto(course) : null;
    }

    @Transactional(readOnly = true)
    public List<SubjectDto> getEnrolledSubjects(Long studentUserId) {
        StudentProfile profile = studentProfileRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found"));

        if (profile.getCourse() == null) {
            return Collections.emptyList();
        }

        List<Subject> subjects = subjectRepository.findByCourseIdAndYearLevel(
                profile.getCourse().getId(), profile.getCurrentYear()
        );

        return subjects.stream().map(courseService::toSubjectDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudyMaterialDto> getMaterialsForSubject(Integer subjectId) {
        return studyMaterialRepository.findBySubjectId(subjectId).stream()
                .map(m -> StudyMaterialDto.builder()
                        .id(m.getId())
                        .subjectId(m.getSubject().getId())
                        .subjectTitle(m.getSubject().getTitle())
                        .uploaderId(m.getUploader().getId())
                        .uploaderName(m.getUploader().getFullName())
                        .title(m.getTitle())
                        .description(m.getDescription())
                        .fileUrl(m.getFileUrl())
                        .fileSizeBytes(m.getFileSizeBytes())
                        .mimeType(m.getMimeType())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VideoLectureDto> getLecturesForSubject(Integer subjectId) {
        return videoLectureRepository.findBySubjectId(subjectId).stream()
                .map(l -> VideoLectureDto.builder()
                        .id(l.getId())
                        .subjectId(l.getSubject().getId())
                        .subjectTitle(l.getSubject().getTitle())
                        .facultyId(l.getFaculty().getId())
                        .facultyName(l.getFaculty().getFullName())
                        .title(l.getTitle())
                        .description(l.getDescription())
                        .videoUrl(l.getVideoUrl())
                        .durationSeconds(l.getDurationSeconds())
                        .createdAt(l.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
