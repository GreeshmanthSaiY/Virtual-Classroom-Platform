package com.virtualclassroom.service;

import com.virtualclassroom.dto.UpdateProfileRequest;
import com.virtualclassroom.dto.UserDto;
import com.virtualclassroom.model.FacultyProfile;
import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.StudentProfile;
import com.virtualclassroom.model.User;
import com.virtualclassroom.repository.FacultyProfileRepository;
import com.virtualclassroom.repository.StudentProfileRepository;
import com.virtualclassroom.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserProfileService {

    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final AuditLogService auditLogService;

    public UserProfileService(
            UserRepository userRepository,
            StudentProfileRepository studentProfileRepository,
            FacultyProfileRepository facultyProfileRepository,
            AuditLogService auditLogService
    ) {
        this.userRepository = userRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.facultyProfileRepository = facultyProfileRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public UserDto getProfile(Long userId, User requester) {
        if (requester.getRole() != Role.ROLE_ADMIN && !requester.getId().equals(userId)) {
            throw new AccessDeniedException("Access denied: You can only view your own user profile");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        return buildUserDto(user);
    }

    public UserDto updateProfile(Long userId, UpdateProfileRequest request, User requester, String ipAddress) {
        if (requester.getRole() != Role.ROLE_ADMIN && !requester.getId().equals(userId)) {
            throw new AccessDeniedException("Access denied: You can only update your own user profile");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl().trim());
        }
        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber().trim());
        }

        if (user.getRole() == Role.ROLE_STUDENT) {
            studentProfileRepository.findById(userId).ifPresent(profile -> {
                if (request.getBio() != null) {
                    profile.setBio(request.getBio().trim());
                    studentProfileRepository.save(profile);
                }
            });
        } else if (user.getRole() == Role.ROLE_FACULTY) {
            facultyProfileRepository.findById(userId).ifPresent(profile -> {
                if (request.getBio() != null) {
                    profile.setBio(request.getBio().trim());
                }
                if (request.getDesignation() != null && !request.getDesignation().isBlank()) {
                    profile.setDesignation(request.getDesignation().trim());
                }
                if (request.getOfficeHours() != null) {
                    profile.setOfficeHours(request.getOfficeHours().trim());
                }
                facultyProfileRepository.save(profile);
            });
        }

        User saved = userRepository.save(user);
        auditLogService.logAction(requester, "UPDATE_PROFILE", "User", saved.getId().toString(),
                "Updated profile metadata", ipAddress);

        return buildUserDto(saved);
    }

    private UserDto buildUserDto(User user) {
        UserDto.UserDtoBuilder builder = UserDto.builder()
                .id(user.getId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .role(user.getRole().name())
                .status(user.getStatus().name())
                .rejectionReason(user.getRejectionReason())
                .avatarUrl(user.getAvatarUrl())
                .phoneNumber(user.getPhoneNumber())
                .createdAt(user.getCreatedAt());

        if (user.getRole() == Role.ROLE_STUDENT) {
            studentProfileRepository.findById(user.getId()).ifPresent(profile -> {
                builder.rollNumber(profile.getRollNumber())
                        .courseName(profile.getCourse() != null ? profile.getCourse().getName() : null)
                        .currentYear(profile.getCurrentYear());
            });
        } else if (user.getRole() == Role.ROLE_FACULTY) {
            facultyProfileRepository.findById(user.getId()).ifPresent(profile -> {
                builder.employeeId(profile.getEmployeeId())
                        .department(profile.getDepartment());
            });
        }

        return builder.build();
    }
}
