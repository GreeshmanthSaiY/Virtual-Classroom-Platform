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
    @DisplayName("Scenario A: Fresh disposable PostgreSQL with V1, V2 and V3 executes cleanly without demo users")
    void scenarioA_FreshPostgresDatabase_AppliesAllMigrationsCleanly() throws Exception {
        try (EmbeddedPostgres pg = EmbeddedPostgres.start()) {
            DataSource dataSource = pg.getDatabase("postgres", "postgres");

            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load();

            int applied = flyway.migrate().migrationsExecuted;
            assertEquals(3, applied, "All 3 migrations (V1, V2, V3) must execute on a fresh PostgreSQL database");

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

                // 3. Verify legitimate user can be inserted and primary key sequence works
                stmt.executeUpdate(
                        "INSERT INTO users (email, password_hash, full_name, role, status) " +
                        "VALUES ('legitimate.student@university.edu', 'hash123', 'Real Student', 'ROLE_STUDENT', 'APPROVED')");

                try (ResultSet rs = stmt.executeQuery("SELECT id, email FROM users WHERE email = 'legitimate.student@university.edu'")) {
                    assertTrue(rs.next());
                    assertEquals("legitimate.student@university.edu", rs.getString("email"));
                }
            }
        }
    }

    @Test
    @DisplayName("Scenario B: Existing PostgreSQL database with V1 and V2 applied, followed by V3 preserves legitimate data")
    void scenarioB_ExistingPostgresWithLegitimateData_PreservedAfterV3() throws Exception {
        try (EmbeddedPostgres pg = EmbeddedPostgres.start()) {
            DataSource dataSource = pg.getDatabase("postgres", "postgres");

            // Step 1: Migrate up to V2 only
            Flyway flywayV2 = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .target("2")
                    .load();

            int appliedV2 = flywayV2.migrate().migrationsExecuted;
            assertEquals(2, appliedV2, "V1 and V2 must be applied initially");

            // Step 2: Insert legitimate non-demo data into the V2 database
            Long legitimateUserId;
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // Insert legitimate professor
                stmt.executeUpdate(
                        "INSERT INTO users (email, password_hash, full_name, role, status) " +
                        "VALUES ('dr.legitimate@university.edu', 'legit_hash', 'Dr. Legitimate Professor', 'ROLE_FACULTY', 'APPROVED')",
                        Statement.RETURN_GENERATED_KEYS);

                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    assertTrue(keys.next());
                    legitimateUserId = keys.getLong(1);
                }

                // Insert legitimate faculty profile
                stmt.executeUpdate(
                        "INSERT INTO faculty_profiles (user_id, employee_id, department) " +
                        "VALUES (" + legitimateUserId + ", 'EMP-REAL-999', 'Department of Physics')");

                // Insert legitimate study material authored by legitimate professor
                stmt.executeUpdate(
                        "INSERT INTO study_materials (subject_id, uploader_id, title, description, file_url) " +
                        "VALUES ((SELECT MIN(id) FROM subjects), " + legitimateUserId + ", " +
                        "'Quantum Mechanics Lecture Notes', 'Official lecture notes', 'https://storage.university.edu/physics.pdf')");

                // Verify demo users AND legitimate user both exist prior to V3
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users")) {
                    assertTrue(rs.next());
                    assertEquals(6, rs.getInt(1), "Should have 5 demo users + 1 legitimate user");
                }
            }

            // Step 3: Now apply V3 (migrate to target 3)
            Flyway flywayV3 = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .target("3")
                    .load();

            int appliedV3 = flywayV3.migrate().migrationsExecuted;
            assertEquals(1, appliedV3, "V3 must execute on top of existing V2 database");

            // Step 4: Verify that demo users are removed BUT legitimate data is preserved
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {

                // Demo users gone
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM users WHERE email IN (" +
                        "'admin@virtualclassroom.edu', 'dr.jeevan@virtualclassroom.edu', " +
                        "'gaurav.shukla@virtualclassroom.edu', 'alice.smith@virtualclassroom.edu', " +
                        "'prof.tushar@virtualclassroom.edu')")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "All 5 demo users must be deleted");
                }

                // Legitimate user remains intact
                try (ResultSet rs = stmt.executeQuery("SELECT id, email, full_name FROM users WHERE id = " + legitimateUserId)) {
                    assertTrue(rs.next(), "Legitimate user record must NOT be deleted");
                    assertEquals("dr.legitimate@university.edu", rs.getString("email"));
                    assertEquals("Dr. Legitimate Professor", rs.getString("full_name"));
                }

                // Legitimate faculty profile remains intact
                try (ResultSet rs = stmt.executeQuery("SELECT employee_id FROM faculty_profiles WHERE user_id = " + legitimateUserId)) {
                    assertTrue(rs.next(), "Legitimate faculty profile must NOT be deleted");
                    assertEquals("EMP-REAL-999", rs.getString("employee_id"));
                }

                // Legitimate study material remains intact
                try (ResultSet rs = stmt.executeQuery("SELECT title FROM study_materials WHERE uploader_id = " + legitimateUserId)) {
                    assertTrue(rs.next(), "Legitimate study material must NOT be deleted");
                    assertEquals("Quantum Mechanics Lecture Notes", rs.getString("title"));
                }
            }
        }
    }
}
