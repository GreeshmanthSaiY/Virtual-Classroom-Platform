package com.virtualclassroom.service;

import com.virtualclassroom.dto.PlatformMetricsDto;
import com.virtualclassroom.dto.UserDto;
import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.User;
import com.virtualclassroom.model.UserStatus;
import com.virtualclassroom.repository.*;
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

    public AdminService(UserRepository userRepository, CourseRepository courseRepository, SubjectRepository subjectRepository, StudyMaterialRepository studyMaterialRepository, VideoLectureRepository videoLectureRepository, QuestionRepository questionRepository, NotificationService notificationService, AuditLogService auditLogService) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.studyMaterialRepository = studyMaterialRepository;
        this.videoLectureRepository = videoLectureRepository;
        this.questionRepository = questionRepository;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
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

    @Transactional(readOnly = true)
    public PlatformMetricsDto getPlatformMetrics() {
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
