package com.virtualclassroom.service;

import com.virtualclassroom.dto.PlatformMetricsDto;
import com.virtualclassroom.dto.UserDto;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final VideoLectureRepository videoLectureRepository;
    private final QuestionRepository questionRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final FacultySubjectAssignmentRepository facultySubjectAssignmentRepository;
    private final AssignmentRepository assignmentRepository;
    private final ExaminationRepository examinationRepository;
    private final LiveSessionRepository liveSessionRepository;

    public AdminService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            SubjectRepository subjectRepository,
            StudyMaterialRepository studyMaterialRepository,
            VideoLectureRepository videoLectureRepository,
            QuestionRepository questionRepository,
            NotificationService notificationService,
            AuditLogService auditLogService
    ) {
        this(userRepository, courseRepository, subjectRepository, studyMaterialRepository,
                videoLectureRepository, questionRepository, notificationService, auditLogService,
                null, null, null, null, null, null);
    }

    @Autowired
    public AdminService(
            UserRepository userRepository,
            CourseRepository courseRepository,
            SubjectRepository subjectRepository,
            StudyMaterialRepository studyMaterialRepository,
            VideoLectureRepository videoLectureRepository,
            QuestionRepository questionRepository,
            NotificationService notificationService,
            AuditLogService auditLogService,
            @Autowired(required = false) RefreshTokenRepository refreshTokenRepository,
            @Autowired(required = false) FacultyProfileRepository facultyProfileRepository,
            @Autowired(required = false) FacultySubjectAssignmentRepository facultySubjectAssignmentRepository,
            @Autowired(required = false) AssignmentRepository assignmentRepository,
            @Autowired(required = false) ExaminationRepository examinationRepository,
            @Autowired(required = false) LiveSessionRepository liveSessionRepository
    ) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.studyMaterialRepository = studyMaterialRepository;
        this.videoLectureRepository = videoLectureRepository;
        this.questionRepository = questionRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.facultyProfileRepository = facultyProfileRepository;
        this.facultySubjectAssignmentRepository = facultySubjectAssignmentRepository;
        this.assignmentRepository = assignmentRepository;
        this.examinationRepository = examinationRepository;
        this.liveSessionRepository = liveSessionRepository;
    }


    @Transactional(readOnly = true)
    public List<UserDto> getPendingStudents() {
        return userRepository.findByRoleAndStatus(Role.ROLE_STUDENT, UserStatus.PENDING)
                .stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<UserDto> getPendingFaculty() {
        return userRepository.findByRoleAndStatus(Role.ROLE_FACULTY, UserStatus.PENDING)
                .stream()
                .map(this::toUserDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<UserDto> getAllUsersByRole(Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable).map(this::toUserDto);
    }

    @Transactional
    public void approveUsers(List<Long> userIds, User adminActor, String ipAddress) {
        List<User> users = userRepository.findAllById(userIds);
        for (User user : users) {
            user.setStatus(UserStatus.APPROVED);
            user.setRejectionReason(null);
            userRepository.save(user);

            notificationService.createNotification(
                    user,
                    "Registration Approved!",
                    "Your Virtual Classroom account has been approved by the Administrator. You now have full access to your classroom.",
                    "APPROVAL"
            );

            auditLogService.logAction(adminActor, "APPROVE_USER", "users", user.getId().toString(),
                    "Approved user account for: " + user.getEmail(), ipAddress);
        }
    }

    @Transactional
    public void rejectUsers(List<Long> userIds, String reason, User adminActor, String ipAddress) {
        List<User> users = userRepository.findAllById(userIds);
        for (User user : users) {
            user.setStatus(UserStatus.REJECTED);
            user.setRejectionReason(reason);
            userRepository.save(user);

            notificationService.createNotification(
                    user,
                    "Registration Update",
                    "Your registration request was not approved. Reason: " + reason,
                    "REJECTION"
            );

            auditLogService.logAction(adminActor, "REJECT_USER", "users", user.getId().toString(),
                    "Rejected user account for: " + user.getEmail() + ". Reason: " + reason, ipAddress);
        }
    }

    @Transactional
    public void suspendUser(Long userId, String reason, User adminActor, String ipAddress) {
        if (adminActor != null && adminActor.getId().equals(userId)) {
            throw new IllegalArgumentException("Administrators cannot suspend their own account.");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        user.setStatus(UserStatus.SUSPENDED);
        user.setRejectionReason(reason);
        userRepository.save(user);

        if (refreshTokenRepository != null) {
            refreshTokenRepository.revokeAllActiveByUserId(userId);
        }

        notificationService.createNotification(
                user,
                "Account Suspended",
                "Your account access has been suspended. Reason: " + reason,
                "ACCOUNT_SUSPENDED"
        );

        auditLogService.logAction(adminActor, "SUSPEND_USER", "users", user.getId().toString(),
                "Suspended user: " + user.getEmail() + ". Reason: " + reason, ipAddress);
    }

    @Transactional
    public void reactivateUser(Long userId, User adminActor, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (user.getStatus() != UserStatus.SUSPENDED) {
            throw new IllegalStateException("User account is not currently suspended (status: " + user.getStatus() + ")");
        }

        user.setStatus(UserStatus.APPROVED);
        user.setRejectionReason(null);
        userRepository.save(user);

        notificationService.createNotification(
                user,
                "Account Reactivated",
                "Your account has been reactivated. You now have full access to your classroom.",
                "ACCOUNT_REACTIVATED"
        );

        auditLogService.logAction(adminActor, "REACTIVATE_USER", "users", user.getId().toString(),
                "Reactivated user account for: " + user.getEmail(), ipAddress);
    }

    @Transactional(readOnly = true)
    public Page<UserDto> searchUsers(String query, Role role, UserStatus status, Pageable pageable) {
        return userRepository.searchUsers(query, role, status, pageable).map(this::toUserDto);
    }

    @Transactional
    public void assignFacultyToSubject(Integer subjectId, Long facultyId, String academicTerm, User adminActor, String ipAddress) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        FacultyProfile facultyProfile = null;
        if (facultyProfileRepository != null) {
            facultyProfile = facultyProfileRepository.findById(facultyId)
                    .orElseThrow(() -> new IllegalArgumentException("Faculty profile not found for user ID: " + facultyId));
        }

        if (facultySubjectAssignmentRepository != null && facultyProfile != null) {
            String term = (academicTerm != null && !academicTerm.isBlank()) ? academicTerm : "2026-ACTIVE";
            FacultySubjectAssignment assignment = FacultySubjectAssignment.builder()
                    .facultyId(facultyId)
                    .subjectId(subjectId)
                    .academicTerm(term)
                    .facultyProfile(facultyProfile)
                    .subject(subject)
                    .build();
            facultySubjectAssignmentRepository.save(assignment);
        }

        User facultyUser = userRepository.findById(facultyId).orElse(null);
        if (facultyUser != null) {
            notificationService.createNotification(
                    facultyUser,
                    "New Subject Assigned",
                    "You have been assigned as the instructor for subject: " + subject.getTitle() + " (" + subject.getCode() + ")",
                    "FACULTY_ASSIGNMENT"
            );
        }

        auditLogService.logAction(adminActor, "ASSIGN_FACULTY_SUBJECT", "faculty_subject_assignments",
                facultyId + "-" + subjectId, "Assigned faculty " + facultyId + " to subject " + subjectId, ipAddress);
    }

    @Transactional(readOnly = true)
    public PlatformMetricsDto getPlatformMetrics() {
        long totalAssignments = assignmentRepository != null ? assignmentRepository.count() : 0;
        long totalExaminations = examinationRepository != null ? examinationRepository.count() : 0;
        long upcomingLiveSessions = liveSessionRepository != null ? liveSessionRepository.countByStatus(LiveSessionStatus.SCHEDULED) : 0;
        long suspendedUsers = userRepository.countByStatus(UserStatus.SUSPENDED);
        long approvedUsers = userRepository.countByStatus(UserStatus.APPROVED);

        return PlatformMetricsDto.builder()
                .totalStudents(userRepository.countByRole(Role.ROLE_STUDENT))
                .totalFaculty(userRepository.countByRole(Role.ROLE_FACULTY))
                .pendingStudentApprovals(userRepository.countByRoleAndStatus(Role.ROLE_STUDENT, UserStatus.PENDING))
                .pendingFacultyApprovals(userRepository.countByRoleAndStatus(Role.ROLE_FACULTY, UserStatus.PENDING))
                .totalCourses(courseRepository.count())
                .totalSubjects(subjectRepository.count())
                .totalMaterials(studyMaterialRepository.count())
                .totalLectures(videoLectureRepository.count())
                .totalQuestions(questionRepository.count())
                .resolvedQuestions(questionRepository.countByIsResolvedTrue())
                .totalAssignments(totalAssignments)
                .totalExaminations(totalExaminations)
                .upcomingLiveSessions(upcomingLiveSessions)
                .suspendedUsers(suspendedUsers)
                .approvedUsers(approvedUsers)
                .build();
    }

    private UserDto toUserDto(User user) {
        UserDto.UserDtoBuilder builder = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .rejectionReason(user.getRejectionReason())
                .createdAt(user.getCreatedAt());

        if (user.getStudentProfile() != null) {
            builder.rollNumber(user.getStudentProfile().getRollNumber())
                    .courseName(user.getStudentProfile().getCourse() != null ? user.getStudentProfile().getCourse().getName() : null)
                    .currentYear(user.getStudentProfile().getCurrentYear());
        }

        if (user.getFacultyProfile() != null) {
            builder.employeeId(user.getFacultyProfile().getEmployeeId())
                    .department(user.getFacultyProfile().getDepartment());
        }

        return builder.build();
    }
}
