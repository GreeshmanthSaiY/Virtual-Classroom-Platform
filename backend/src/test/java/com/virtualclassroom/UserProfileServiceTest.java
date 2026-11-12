package com.virtualclassroom;

import com.virtualclassroom.dto.UpdateProfileRequest;
import com.virtualclassroom.dto.UserDto;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.FacultyProfileRepository;
import com.virtualclassroom.repository.StudentProfileRepository;
import com.virtualclassroom.repository.UserRepository;
import com.virtualclassroom.service.AuditLogService;
import com.virtualclassroom.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private StudentProfileRepository studentProfileRepository;
    @Mock
    private FacultyProfileRepository facultyProfileRepository;
    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private UserProfileService userProfileService;

    private User studentUser;
    private StudentProfile studentProfile;
    private User facultyUser;
    private FacultyProfile facultyProfile;

    @BeforeEach
    void setUp() {
        studentUser = User.builder()
                .id(200L)
                .email("student@test.com")
                .fullName("Alice Brown")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.APPROVED)
                .build();

        studentProfile = StudentProfile.builder()
                .user(studentUser)
                .rollNumber("CS-101")
                .build();

        facultyUser = User.builder()
                .id(100L)
                .email("faculty@test.com")
                .fullName("Dr. Smith")
                .role(Role.ROLE_FACULTY)
                .status(UserStatus.APPROVED)
                .build();

        facultyProfile = FacultyProfile.builder()
                .user(facultyUser)
                .employeeId("FAC-101")
                .department("Computer Science")
                .build();
    }

    @Test
    @DisplayName("Get profile returns student details")
    void getProfile_Student_ReturnsDto() {
        when(userRepository.findById(200L)).thenReturn(Optional.of(studentUser));
        when(studentProfileRepository.findById(200L)).thenReturn(Optional.of(studentProfile));

        UserDto dto = userProfileService.getProfile(200L, studentUser);

        assertNotNull(dto);
        assertEquals("Alice Brown", dto.getFullName());
        assertEquals("CS-101", dto.getRollNumber());
    }

    @Test
    @DisplayName("Update student profile updates avatar, phone, and bio")
    void updateProfile_Student_UpdatesSuccessfully() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Alice B. Brown");
        req.setPhoneNumber("+1234567890");
        req.setAvatarUrl("https://example.com/alice.png");
        req.setBio("CS Sophomore interested in algorithms");

        when(userRepository.findById(200L)).thenReturn(Optional.of(studentUser));
        when(studentProfileRepository.findById(200L)).thenReturn(Optional.of(studentProfile));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(studentProfileRepository.save(any(StudentProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserDto result = userProfileService.updateProfile(200L, req, studentUser, "127.0.0.1");

        assertNotNull(result);
        assertEquals("Alice B. Brown", result.getFullName());
        assertEquals("+1234567890", result.getPhoneNumber());
        assertEquals("https://example.com/alice.png", result.getAvatarUrl());
        assertEquals("CS Sophomore interested in algorithms", studentProfile.getBio());
        verify(auditLogService).logAction(eq(studentUser), eq("UPDATE_PROFILE"), eq("User"), eq("200"), anyString(), eq("127.0.0.1"));
    }

    @Test
    @DisplayName("Update faculty profile updates designation, office hours, and bio")
    void updateProfile_Faculty_UpdatesSuccessfully() {
        UpdateProfileRequest req = new UpdateProfileRequest();
        req.setFullName("Dr. Robert Smith");
        req.setDesignation("Associate Professor");
        req.setOfficeHours("Mon, Wed 2-4 PM");
        req.setBio("Researcher in distributed systems");

        when(userRepository.findById(100L)).thenReturn(Optional.of(facultyUser));
        when(facultyProfileRepository.findById(100L)).thenReturn(Optional.of(facultyProfile));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));
        when(facultyProfileRepository.save(any(FacultyProfile.class))).thenAnswer(i -> i.getArgument(0));

        UserDto result = userProfileService.updateProfile(100L, req, facultyUser, "127.0.0.1");

        assertNotNull(result);
        assertEquals("Dr. Robert Smith", result.getFullName());
        assertEquals("Associate Professor", facultyProfile.getDesignation());
        assertEquals("Mon, Wed 2-4 PM", facultyProfile.getOfficeHours());
        assertEquals("Researcher in distributed systems", facultyProfile.getBio());
    }
}
