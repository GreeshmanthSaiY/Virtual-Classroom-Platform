package com.virtualclassroom.service;

import com.virtualclassroom.dto.*;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
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
    private final LiveSessionRepository liveSessionRepository;
    private final AssignmentRepository assignmentRepository;
    private final AssignmentSubmissionRepository assignmentSubmissionRepository;
    private final ExaminationResultRepository examinationResultRepository;
    private final NotificationRepository notificationRepository;

    public StudentService(
            StudentProfileRepository studentProfileRepository,
            SubjectRepository subjectRepository,
            StudyMaterialRepository studyMaterialRepository,
            VideoLectureRepository videoLectureRepository,
            CourseService courseService
    ) {
        this(studentProfileRepository, subjectRepository, studyMaterialRepository, videoLectureRepository,
                courseService, null, null, null, null, null);
    }

    @Autowired
    public StudentService(
            StudentProfileRepository studentProfileRepository,
            SubjectRepository subjectRepository,
            StudyMaterialRepository studyMaterialRepository,
            VideoLectureRepository videoLectureRepository,
            CourseService courseService,
            @Autowired(required = false) LiveSessionRepository liveSessionRepository,
            @Autowired(required = false) AssignmentRepository assignmentRepository,
            @Autowired(required = false) AssignmentSubmissionRepository assignmentSubmissionRepository,
            @Autowired(required = false) ExaminationResultRepository examinationResultRepository,
            @Autowired(required = false) NotificationRepository notificationRepository
    ) {
        this.studentProfileRepository = studentProfileRepository;
        this.subjectRepository = subjectRepository;
        this.studyMaterialRepository = studyMaterialRepository;
        this.videoLectureRepository = videoLectureRepository;
        this.courseService = courseService;
        this.liveSessionRepository = liveSessionRepository;
        this.assignmentRepository = assignmentRepository;
        this.assignmentSubmissionRepository = assignmentSubmissionRepository;
        this.examinationResultRepository = examinationResultRepository;
        this.notificationRepository = notificationRepository;
    }

    @Transactional(readOnly = true)
    public StudentDashboardDto getStudentDashboard(Long studentUserId) {
        StudentProfile profile = studentProfileRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found"));

        CourseDto course = profile.getCourse() != null ? courseService.toCourseDto(profile.getCourse()) : null;

        List<Subject> subjects = profile.getCourse() != null
                ? subjectRepository.findByCourseIdAndYearLevel(profile.getCourse().getId(), profile.getCurrentYear())
                : Collections.emptyList();
        List<SubjectDto> enrolledSubjects = subjects.stream().map(courseService::toSubjectDto).collect(Collectors.toList());
        List<Integer> subjectIds = subjects.stream().map(Subject::getId).collect(Collectors.toList());

        List<LiveSessionDto> upcomingSessions = Collections.emptyList();
        if (liveSessionRepository != null && !subjectIds.isEmpty()) {
            upcomingSessions = liveSessionRepository.findBySubjectIdInAndStatusOrderByScheduledStartAsc(
                    subjectIds, LiveSessionStatus.SCHEDULED).stream()
                    .map(LiveSessionDto::fromEntity)
                    .limit(5)
                    .collect(Collectors.toList());
        }

        List<AssignmentDto> pendingAssignments = Collections.emptyList();
        if (assignmentRepository != null && !subjectIds.isEmpty()) {
            pendingAssignments = assignmentRepository.findBySubjectIdInOrderByDueDateAsc(subjectIds).stream()
                    .filter(a -> assignmentSubmissionRepository == null || !assignmentSubmissionRepository.existsByAssignmentIdAndStudentId(a.getId(), studentUserId))
                    .map(AssignmentDto::fromEntity)
                    .limit(5)
                    .collect(Collectors.toList());
        }

        List<AssignmentSubmissionDto> recentSubmissions = Collections.emptyList();
        if (assignmentSubmissionRepository != null) {
            recentSubmissions = assignmentSubmissionRepository.findByStudentIdOrderBySubmittedAtDesc(studentUserId).stream()
                    .map(AssignmentSubmissionDto::fromEntity)
                    .limit(5)
                    .collect(Collectors.toList());
        }

        List<ExaminationResultDto> recentExamResults = Collections.emptyList();
        if (examinationResultRepository != null) {
            recentExamResults = examinationResultRepository.findByStudentIdOrderByGradedAtDesc(studentUserId).stream()
                    .map(ExaminationResultDto::fromEntity)
                    .limit(5)
                    .collect(Collectors.toList());
        }

        long unreadCount = notificationRepository != null ? notificationRepository.countByUserIdAndIsReadFalse(studentUserId) : 0;

        return StudentDashboardDto.builder()
                .course(course)
                .enrolledSubjects(enrolledSubjects)
                .upcomingLiveSessions(upcomingSessions)
                .pendingAssignments(pendingAssignments)
                .recentSubmissions(recentSubmissions)
                .recentExamResults(recentExamResults)
                .unreadNotificationCount(unreadCount)
                .build();
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

    public void verifyStudentEnrolledInSubject(Long studentUserId, Integer subjectId) {
        StudentProfile profile = studentProfileRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found"));
        if (profile.getCourse() == null) {
            throw new AccessDeniedException("Access denied: You are not enrolled in any course");
        }
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));
        if (!subject.getCourse().getId().equals(profile.getCourse().getId()) || !subject.getYearLevel().equals(profile.getCurrentYear())) {
            throw new AccessDeniedException("Access denied: You are not enrolled in this subject");
        }
    }

    @Transactional(readOnly = true)
    public List<StudyMaterialDto> getAllEnrolledMaterials(Long studentUserId, Integer subjectId) {
        if (subjectId != null) {
            verifyStudentEnrolledInSubject(studentUserId, subjectId);
            return getMaterialsForSubject(subjectId);
        }
        StudentProfile profile = studentProfileRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found"));
        if (profile.getCourse() == null) {
            return Collections.emptyList();
        }
        List<Subject> subjects = subjectRepository.findByCourseIdAndYearLevel(
                profile.getCourse().getId(), profile.getCurrentYear()
        );
        if (subjects.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> subjectIds = subjects.stream().map(Subject::getId).collect(Collectors.toList());
        return studyMaterialRepository.findBySubjectIdIn(subjectIds).stream()
                .map(this::toMaterialDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VideoLectureDto> getAllEnrolledLectures(Long studentUserId, Integer subjectId) {
        if (subjectId != null) {
            verifyStudentEnrolledInSubject(studentUserId, subjectId);
            return getLecturesForSubject(subjectId);
        }
        StudentProfile profile = studentProfileRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Student profile not found"));
        if (profile.getCourse() == null) {
            return Collections.emptyList();
        }
        List<Subject> subjects = subjectRepository.findByCourseIdAndYearLevel(
                profile.getCourse().getId(), profile.getCurrentYear()
        );
        if (subjects.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> subjectIds = subjects.stream().map(Subject::getId).collect(Collectors.toList());
        return videoLectureRepository.findBySubjectIdIn(subjectIds).stream()
                .map(this::toLectureDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<StudyMaterialDto> getMaterialsForSubject(Integer subjectId) {
        return studyMaterialRepository.findBySubjectId(subjectId).stream()
                .map(this::toMaterialDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VideoLectureDto> getLecturesForSubject(Integer subjectId) {
        return videoLectureRepository.findBySubjectId(subjectId).stream()
                .map(this::toLectureDto)
                .collect(Collectors.toList());
    }

    private StudyMaterialDto toMaterialDto(StudyMaterial m) {
        return StudyMaterialDto.builder()
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
                .build();
    }

    private VideoLectureDto toLectureDto(VideoLecture l) {
        return VideoLectureDto.builder()
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
                .build();
    }
}
