package com.virtualclassroom;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class FlywayMigrationSafetyTest {

    @Test
    @DisplayName("Scenario A: Fresh disposable PostgreSQL with V1, V2, V3 and V4 executes cleanly without demo users")
    void scenarioA_FreshPostgresDatabase_AppliesAllMigrationsCleanly() throws Exception {
        try (EmbeddedPostgres pg = EmbeddedPostgres.start()) {
            DataSource dataSource = pg.getDatabase("postgres", "postgres");

            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load();

            int applied = flyway.migrate().migrationsExecuted;
            assertEquals(4, applied, "All 4 migrations (V1, V2, V3, V4) must execute on a fresh PostgreSQL database");

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // 1. Verify degrees and courses exist (from V2)
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM degrees")) {
                    assertTrue(rs.next());
                    assertTrue(rs.getInt(1) > 0, "Academic degrees must be present");
                }

                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM courses")) {
                    assertTrue(rs.next());
                    assertTrue(rs.getInt(1) > 0, "Academic courses must be present");
                }

                // 2. Verify all demo users are completely purged (by V3)
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM users WHERE email IN (" +
                        "'admin@virtualclassroom.edu', 'dr.jeevan@virtualclassroom.edu', " +
                        "'gaurav.shukla@virtualclassroom.edu', 'alice.smith@virtualclassroom.edu', " +
                        "'prof.tushar@virtualclassroom.edu')")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "No demo users should remain after V3 migration");
                }

                // 3. Verify V4 schema expansion tables exist
                String[] v4Tables = {
                        "refresh_tokens",
                        "student_subject_enrollments",
                        "live_sessions",
                        "assignments",
                        "assignment_submissions",
                        "examinations",
                        "examination_results"
                };
                for (String table : v4Tables) {
                    try (ResultSet rs = stmt.executeQuery(
                            "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '" + table + "')")) {
                        assertTrue(rs.next());
                        assertTrue(rs.getBoolean(1), "Table '" + table + "' must be created by V4 migration");
                    }
                }

                // 4. Verify V4 added columns exist
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'users' AND column_name = 'avatar_url')")) {
                    assertTrue(rs.next());
                    assertTrue(rs.getBoolean(1), "Column 'avatar_url' must exist on 'users'");
                }
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'subjects' AND column_name = 'semester')")) {
                    assertTrue(rs.next());
                    assertTrue(rs.getBoolean(1), "Column 'semester' must exist on 'subjects'");
                }

                // 5. Verify legitimate user can be inserted and primary key sequence works
                stmt.executeUpdate(
                        "INSERT INTO users (email, password_hash, full_name, role, status, avatar_url, phone_number) " +
                        "VALUES ('legitimate.student@university.edu', 'hash123', 'Real Student', 'ROLE_STUDENT', 'APPROVED', 'https://storage/avatar.png', '+1234567890')");

                try (ResultSet rs = stmt.executeQuery("SELECT id, email, avatar_url FROM users WHERE email = 'legitimate.student@university.edu'")) {
                    assertTrue(rs.next());
                    assertEquals("legitimate.student@university.edu", rs.getString("email"));
                    assertEquals("https://storage/avatar.png", rs.getString("avatar_url"));
                }
            }
        }
    }

    @Test
    @DisplayName("Scenario B: Existing PostgreSQL database with V1-V3 applied, followed by V4 upgrade preserves data and backfills")
    void scenarioB_ExistingPostgresWithLegitimateData_PreservedAfterV4() throws Exception {
        try (EmbeddedPostgres pg = EmbeddedPostgres.start()) {
            DataSource dataSource = pg.getDatabase("postgres", "postgres");

            // Step 1: Migrate up to V3 only
            Flyway flywayV3 = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .target("3")
                    .load();

            int appliedV3 = flywayV3.migrate().migrationsExecuted;
            assertEquals(3, appliedV3, "V1, V2 and V3 must be applied initially");

            // Step 2: Insert legitimate non-demo data into the V3 database
            Long legitimateFacultyId;
            Long legitimateStudentId;
            Integer csSubjectId;

            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // Insert legitimate professor
                stmt.executeUpdate(
                        "INSERT INTO users (email, password_hash, full_name, role, status) " +
                        "VALUES ('dr.legitimate@university.edu', 'legit_hash', 'Dr. Legitimate Professor', 'ROLE_FACULTY', 'APPROVED')",
                        Statement.RETURN_GENERATED_KEYS);

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    assertTrue(keys.next());
                    legitimateFacultyId = keys.getLong(1);
                }

                // Insert faculty profile
                stmt.executeUpdate(
                        "INSERT INTO faculty_profiles (user_id, employee_id, department) " +
                        "VALUES (" + legitimateFacultyId + ", 'EMP-REAL-999', 'Department of Physics')");

                // Insert legitimate student
                stmt.executeUpdate(
                        "INSERT INTO users (email, password_hash, full_name, role, status) " +
                        "VALUES ('student.real@university.edu', 'student_hash', 'Real Enrolled Student', 'ROLE_STUDENT', 'APPROVED')",
                        Statement.RETURN_GENERATED_KEYS);

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    assertTrue(keys.next());
                    legitimateStudentId = keys.getLong(1);
                }

                // Insert student profile enrolled in BSC-CS Year 1
                stmt.executeUpdate(
                        "INSERT INTO student_profiles (user_id, roll_number, course_id, current_year) " +
                        "VALUES (" + legitimateStudentId + ", 'CS-2026-001', " +
                        "(SELECT id FROM courses WHERE code = 'BSC-CS'), 1)");

                // Get ID of CS101 (BSC-CS Year 1 subject)
                try (ResultSet rs = stmt.executeQuery("SELECT id FROM subjects WHERE code = 'CS101'")) {
                    assertTrue(rs.next());
                    csSubjectId = rs.getInt(1);
                }

                // Insert legitimate study material authored by legitimate professor
                stmt.executeUpdate(
                        "INSERT INTO study_materials (subject_id, uploader_id, title, description, file_url) " +
                        "VALUES (" + csSubjectId + ", " + legitimateFacultyId + ", " +
                        "'Quantum Mechanics Lecture Notes', 'Official lecture notes', 'https://storage.university.edu/physics.pdf')");
            }

            // Step 3: Now apply V4 upgrade (migrate to target 4)
            Flyway flywayV4 = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .target("4")
                    .load();

            int appliedV4 = flywayV4.migrate().migrationsExecuted;
            assertEquals(1, appliedV4, "V4 must execute on top of existing V3 database");

            // Step 4: Verify that legitimate data is preserved AND V4 features work
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // Legitimate user remains intact
                try (ResultSet rs = stmt.executeQuery("SELECT id, email, full_name FROM users WHERE id = " + legitimateFacultyId)) {
                    assertTrue(rs.next(), "Legitimate professor must NOT be deleted");
                    assertEquals("dr.legitimate@university.edu", rs.getString("email"));
                }

                // Legitimate student profile remains intact
                try (ResultSet rs = stmt.executeQuery("SELECT roll_number FROM student_profiles WHERE user_id = " + legitimateStudentId)) {
                    assertTrue(rs.next(), "Legitimate student profile must NOT be deleted");
                    assertEquals("CS-2026-001", rs.getString("roll_number"));
                }

                // Student enrollment was automatically backfilled for CS101
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT status FROM student_subject_enrollments WHERE student_id = " + legitimateStudentId + " AND subject_id = " + csSubjectId)) {
                    assertTrue(rs.next(), "Student must be backfilled into subject enrollment");
                    assertEquals("ENROLLED", rs.getString("status"));
                }

                // Create an assignment and submission in the upgraded database
                stmt.executeUpdate(
                        "INSERT INTO assignments (subject_id, faculty_id, title, description, due_date, max_points) " +
                        "VALUES (" + csSubjectId + ", " + legitimateFacultyId + ", 'HW1: Python Basics', 'Complete exercises 1 to 5', NOW() + INTERVAL '7 days', 100)",
                        Statement.RETURN_GENERATED_KEYS);

                Long assignmentId;
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    assertTrue(keys.next());
                    assignmentId = keys.getLong(1);
                }

                stmt.executeUpdate(
                        "INSERT INTO assignment_submissions (assignment_id, student_id, file_url, status) " +
                        "VALUES (" + assignmentId + ", " + legitimateStudentId + ", 'https://storage/hw1.py', 'SUBMITTED')");

                try (ResultSet rs = stmt.executeQuery(
                        "SELECT status, file_url FROM assignment_submissions WHERE assignment_id = " + assignmentId)) {
                    assertTrue(rs.next());
                    assertEquals("SUBMITTED", rs.getString("status"));
                    assertEquals("https://storage/hw1.py", rs.getString("file_url"));
                }
            }
        }
    }
}
