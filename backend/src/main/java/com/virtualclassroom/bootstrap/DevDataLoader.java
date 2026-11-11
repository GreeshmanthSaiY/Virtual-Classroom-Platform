package com.virtualclassroom.bootstrap;

import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Development-only Data Loader.
 * Active ONLY under 'local' or 'dev' profiles.
 * Never executed in production environments.
 */
@Component
@Profile({"local", "dev"})
public class DevDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataLoader.class);

    private final UserRepository userRepository;
    private final FacultyProfileRepository facultyProfileRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final CourseRepository courseRepository;
    private final SubjectRepository subjectRepository;
    private final FacultySubjectAssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    public DevDataLoader(
            UserRepository userRepository,
            FacultyProfileRepository facultyProfileRepository,
            StudentProfileRepository studentProfileRepository,
            CourseRepository courseRepository,
            SubjectRepository subjectRepository,
            FacultySubjectAssignmentRepository assignmentRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.facultyProfileRepository = facultyProfileRepository;
        this.studentProfileRepository = studentProfileRepository;
        this.courseRepository = courseRepository;
        this.subjectRepository = subjectRepository;
        this.assignmentRepository = assignmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Local development users already present. Skipping dev seed.");
            return;
        }

        log.info("Seeding local development test accounts (Profile: local/dev)...");

        // 1. Local Admin
        User admin = User.builder()
                .email("admin@virtualclassroom.edu")
                .fullName("System Administrator")
                .passwordHash(passwordEncoder.encode("Admin@2026!"))
                .role(Role.ROLE_ADMIN)
                .status(UserStatus.APPROVED)
                .build();
        userRepository.save(admin);

        // 2. Local Faculty
        User faculty = User.builder()
                .email("dr.jeevan@virtualclassroom.edu")
                .fullName("Dr. Jeevan Limaye")
                .passwordHash(passwordEncoder.encode("Teacher@2026!"))
                .role(Role.ROLE_FACULTY)
                .status(UserStatus.APPROVED)
                .build();
        User savedFaculty = userRepository.save(faculty);

        FacultyProfile facultyProfile = FacultyProfile.builder()
                .user(savedFaculty)
                .employeeId("EMP-DEV-001")
                .department("Department of Computer Science")
                .build();
        facultyProfileRepository.save(facultyProfile);

        // 3. Local Student
        User student = User.builder()
                .email("gaurav.shukla@virtualclassroom.edu")
                .fullName("Gaurav Shukla")
                .passwordHash(passwordEncoder.encode("Student@2026!"))
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.APPROVED)
                .build();
        User savedStudent = userRepository.save(student);

        Course csCourse = courseRepository.findByCode("BSC-CS").orElse(null);
        if (csCourse != null) {
            StudentProfile studentProfile = StudentProfile.builder()
                    .user(savedStudent)
                    .rollNumber("CB.EN.U4DEV21170")
                    .course(csCourse)
                    .currentYear(3)
                    .build();
            studentProfileRepository.save(studentProfile);
        }

        // 4. Assign faculty to subjects if subjects exist
        subjectRepository.findByCode("CS201").ifPresent(subject -> {
            FacultySubjectAssignment assignment = FacultySubjectAssignment.builder()
                    .facultyId(savedFaculty.getId())
                    .subjectId(subject.getId())
                    .academicTerm("2026-SPRING")
                    .build();
            assignmentRepository.save(assignment);
        });

        subjectRepository.findByCode("CS301").ifPresent(subject -> {
            FacultySubjectAssignment assignment = FacultySubjectAssignment.builder()
                    .facultyId(savedFaculty.getId())
                    .subjectId(subject.getId())
                    .academicTerm("2026-SPRING")
                    .build();
            assignmentRepository.save(assignment);
        });

        log.info("Local development seed completed successfully.");
    }
}
