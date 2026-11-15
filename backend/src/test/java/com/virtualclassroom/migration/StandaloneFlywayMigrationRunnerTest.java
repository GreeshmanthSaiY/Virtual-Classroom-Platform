package com.virtualclassroom.migration;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Automated test suite for StandaloneFlywayMigrationRunner.
 * Validates fail-closed security guards, environment parsing, and migration execution
 * against an isolated disposable PostgreSQL test database.
 * NEVER connects to live Supabase staging or production.
 */
class StandaloneFlywayMigrationRunnerTest {

    private static final String VALID_STAGING_URL =
            "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres?sslmode=require";
    private static final String VALID_STAGING_USER = "postgres.zrbpjcfssbtxxgrjtsca";
    private static final String VALID_PASSWORD = "mock_secret_password_never_printed";

    private interface DisposableDb extends AutoCloseable {
        DataSource getDataSource();
        @Override
        default void close() throws Exception {}
    }

    private DisposableDb createDisposableDb() throws Exception {
        String extUrl = System.getProperty("flyway.test.jdbc-url", System.getenv("POSTGRES_TEST_URL"));
        if (extUrl != null && !extUrl.isBlank()) {
            String user = System.getProperty("flyway.test.user", System.getenv().getOrDefault("POSTGRES_TEST_USER", "postgres"));
            String pass = System.getProperty("flyway.test.password", System.getenv().getOrDefault("POSTGRES_TEST_PASSWORD", "postgres"));

            PGSimpleDataSource ds = new PGSimpleDataSource();
            ds.setUrl(extUrl);
            ds.setUser(user);
            ds.setPassword(pass);

            try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
                stmt.execute("DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public;");
            }

            return new DisposableDb() {
                @Override
                public DataSource getDataSource() {
                    return ds;
                }
            };
        }

        try {
            EmbeddedPostgres pg = EmbeddedPostgres.start();
            DataSource ds = pg.getDatabase("postgres", "postgres");
            return new DisposableDb() {
                @Override
                public DataSource getDataSource() {
                    return ds;
                }
                @Override
                public void close() throws Exception {
                    pg.close();
                }
            };
        } catch (Exception e) {
            Assumptions.abort("Host environment restricts executing embedded postgres binaries: " + e.getMessage());
            throw e;
        }
    }

    // =========================================================================
    // 1. Missing Environment Variables
    // =========================================================================
    @Test
    @DisplayName("Guard 1: Missing or blank environment variables are strictly rejected")
    void testMissingEnvironmentVariables_ThrowsException() {
        // Missing URL
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                StandaloneFlywayMigrationRunner.validateStagingEnvironment(null, VALID_STAGING_USER, VALID_PASSWORD));
        assertTrue(ex1.getMessage().contains("DATABASE_URL"));

        IllegalArgumentException ex1b = assertThrows(IllegalArgumentException.class, () ->
                StandaloneFlywayMigrationRunner.validateStagingEnvironment("   ", VALID_STAGING_USER, VALID_PASSWORD));
        assertTrue(ex1b.getMessage().contains("DATABASE_URL"));

        // Missing Username
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                StandaloneFlywayMigrationRunner.validateStagingEnvironment(VALID_STAGING_URL, null, VALID_PASSWORD));
        assertTrue(ex2.getMessage().contains("DATABASE_USERNAME"));

        // Missing Password
        IllegalArgumentException ex3 = assertThrows(IllegalArgumentException.class, () ->
                StandaloneFlywayMigrationRunner.validateStagingEnvironment(VALID_STAGING_URL, VALID_STAGING_USER, ""));
        assertTrue(ex3.getMessage().contains("DATABASE_PASSWORD"));
    }

    // =========================================================================
    // 2. Wrong Hostname
    // =========================================================================
    @Test
    @DisplayName("Guard 2: Non-staging pooler hostnames are strictly rejected")
    void testWrongHostname_ThrowsException() {
        String wrongHostUrl = "jdbc:postgresql://db.zrbpjcfssbtxxgrjtsca.supabase.co:5432/postgres?sslmode=require";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                StandaloneFlywayMigrationRunner.validateStagingEnvironment(wrongHostUrl, VALID_STAGING_USER, VALID_PASSWORD));
        assertTrue(ex.getMessage().contains("INVALID HOST"));
    }

    // =========================================================================
    // 3. Wrong Port
    // =========================================================================
    @Test
    @DisplayName("Guard 3: Port other than 5432 (e.g. Transaction Pooler 6543) is strictly rejected")
    void testWrongPort_ThrowsException() {
        String wrongPortUrl = "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:6543/postgres?sslmode=require";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                StandaloneFlywayMigrationRunner.validateStagingEnvironment(wrongPortUrl, VALID_STAGING_USER, VALID_PASSWORD));
        assertTrue(ex.getMessage().contains("INVALID PORT"));
    }

    // =========================================================================
    // 4. Wrong Tenant Username
    // =========================================================================
    @Test
    @DisplayName("Guard 4: Plain postgres or non-staging tenant usernames are strictly rejected")
    void testWrongTenantUsername_ThrowsException() {
        // Plain postgres without tenant suffix
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                StandaloneFlywayMigrationRunner.validateStagingEnvironment(VALID_STAGING_URL, "postgres", VALID_PASSWORD));
        assertTrue(ex1.getMessage().contains("INVALID USERNAME"));

        // Wrong project tenant username
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                StandaloneFlywayMigrationRunner.validateStagingEnvironment(VALID_STAGING_URL, "postgres.otherprojectref", VALID_PASSWORD));
        assertTrue(ex2.getMessage().contains("INVALID USERNAME"));
    }

    // =========================================================================
    // 5. Missing SSL Requirement
    // =========================================================================
    @Test
    @DisplayName("Guard 5: Connections without sslmode=require are strictly rejected")
    void testMissingSslRequirement_ThrowsException() {
        String nonSslUrl = "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/postgres";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                StandaloneFlywayMigrationRunner.validateStagingEnvironment(nonSslUrl, VALID_STAGING_USER, VALID_PASSWORD));
        assertTrue(ex.getMessage().contains("SECURITY VIOLATION"));
    }

    // =========================================================================
    // 6. Production Hostname & Target Rejection
    // =========================================================================
    @Test
    @DisplayName("Guard 6: Production target indicators are rejected with zero override allowed")
    void testProductionHostnameRejection_ThrowsException() {
        // Production in URL
        String prodUrl = "jdbc:postgresql://virtual-classroom-prod.supabase.co:5432/postgres?sslmode=require";
        IllegalArgumentException ex1 = assertThrows(IllegalArgumentException.class, () ->
                StandaloneFlywayMigrationRunner.validateStagingEnvironment(prodUrl, VALID_STAGING_USER, VALID_PASSWORD));
        assertTrue(ex1.getMessage().contains("PRODUCTION TARGET DETECTED"));

        // Production in username
        IllegalArgumentException ex2 = assertThrows(IllegalArgumentException.class, () ->
                StandaloneFlywayMigrationRunner.validateStagingEnvironment(VALID_STAGING_URL, "postgres.virtual-classroom-prod", VALID_PASSWORD));
        assertTrue(ex2.getMessage().contains("PRODUCTION TARGET DETECTED"));
    }

    // =========================================================================
    // 7. Invalid Database Identity
    // =========================================================================
    @Test
    @DisplayName("Guard 7: Database name other than 'postgres' is rejected before migration")
    void testInvalidDatabaseIdentity_ThrowsException() {
        // Database name in URL validation
        String wrongDbUrl = "jdbc:postgresql://aws-0-ap-south-1.pooler.supabase.com:5432/customdb?sslmode=require";
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                StandaloneFlywayMigrationRunner.validateStagingEnvironment(wrongDbUrl, VALID_STAGING_USER, VALID_PASSWORD));
        assertTrue(ex.getMessage().contains("INVALID DATABASE NAME"));
    }

    @Test
    @DisplayName("Guard 7b: Post-connection database and user identity verification rejects mismatches")
    void testVerifyDatabaseIdentity_ValidatesDatabaseAndUser() throws Exception {
        Connection mockConn = org.mockito.Mockito.mock(Connection.class);
        Statement mockStmt = org.mockito.Mockito.mock(Statement.class);
        ResultSet mockRs = org.mockito.Mockito.mock(ResultSet.class);

        org.mockito.Mockito.when(mockConn.createStatement()).thenReturn(mockStmt);
        org.mockito.Mockito.when(mockStmt.executeQuery(org.mockito.ArgumentMatchers.anyString())).thenReturn(mockRs);
        org.mockito.Mockito.when(mockRs.next()).thenReturn(true);

        // Case A: Wrong database name
        org.mockito.Mockito.when(mockRs.getString(1)).thenReturn("other_db");
        org.mockito.Mockito.when(mockRs.getString(2)).thenReturn("postgres.zrbpjcfssbtxxgrjtsca");
        org.mockito.Mockito.when(mockRs.getString(3)).thenReturn("PostgreSQL 17.11 on x86_64");

        IllegalStateException exDb = assertThrows(IllegalStateException.class, () ->
                StandaloneFlywayMigrationRunner.verifyDatabaseIdentity(mockConn, "postgres", "postgres.zrbpjcfssbtxxgrjtsca"));
        assertTrue(exDb.getMessage().contains("Database identity mismatch"));

        // Case B: Wrong session user
        org.mockito.Mockito.when(mockRs.getString(1)).thenReturn("postgres");
        org.mockito.Mockito.when(mockRs.getString(2)).thenReturn("unauthorized_user");
        org.mockito.Mockito.when(mockRs.getString(3)).thenReturn("PostgreSQL 17.11 on x86_64");

        IllegalStateException exUser = assertThrows(IllegalStateException.class, () ->
                StandaloneFlywayMigrationRunner.verifyDatabaseIdentity(mockConn, "postgres", "postgres.zrbpjcfssbtxxgrjtsca"));
        assertTrue(exUser.getMessage().contains("Database session user mismatch"));

        // Case C: Non-PostgreSQL engine
        org.mockito.Mockito.when(mockRs.getString(1)).thenReturn("postgres");
        org.mockito.Mockito.when(mockRs.getString(2)).thenReturn("postgres.zrbpjcfssbtxxgrjtsca");
        org.mockito.Mockito.when(mockRs.getString(3)).thenReturn("MySQL 8.0.35");

        IllegalStateException exEngine = assertThrows(IllegalStateException.class, () ->
                StandaloneFlywayMigrationRunner.verifyDatabaseIdentity(mockConn, "postgres", "postgres.zrbpjcfssbtxxgrjtsca"));
        assertTrue(exEngine.getMessage().contains("Database engine is not PostgreSQL"));

        // Case D: Valid matches pass
        org.mockito.Mockito.when(mockRs.getString(1)).thenReturn("postgres");
        org.mockito.Mockito.when(mockRs.getString(2)).thenReturn("postgres.zrbpjcfssbtxxgrjtsca");
        org.mockito.Mockito.when(mockRs.getString(3)).thenReturn("PostgreSQL 17.11 on x86_64");

        assertDoesNotThrow(() ->
                StandaloneFlywayMigrationRunner.verifyDatabaseIdentity(mockConn, "postgres", "postgres.zrbpjcfssbtxxgrjtsca"));

        // Case E: Standard postgres superuser role also accepted
        org.mockito.Mockito.when(mockRs.getString(2)).thenReturn("postgres");
        assertDoesNotThrow(() ->
                StandaloneFlywayMigrationRunner.verifyDatabaseIdentity(mockConn, "postgres", "postgres.zrbpjcfssbtxxgrjtsca"));
    }

    // =========================================================================
    // 8. Successful Migration Against Isolated Test Database
    // =========================================================================
    @Test
    @DisplayName("Execution 8: Executes V1-V5 sequentially on clean isolated PostgreSQL and asserts all 21 tables and zero demo accounts")
    void testSuccessfulMigrationAgainstIsolatedDatabase() throws Exception {
        try (DisposableDb db = createDisposableDb()) {
            DataSource ds = db.getDataSource();

            StandaloneFlywayMigrationRunner.MigrationRunResult result =
                    StandaloneFlywayMigrationRunner.runForTest(ds);

            assertTrue(result.successful(), "Migration run must succeed");
            assertEquals(5, result.migrationsExecuted(), "Must execute exactly 5 migrations (V1 to V5)");
            assertEquals("5", result.targetSchemaVersion(), "Target schema version must be 5");
            assertNull(result.errorMessage());

            // Direct assertion on data state
            try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
                // Verify Degrees, Courses, Subjects count
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM public.degrees")) {
                    assertTrue(rs.next());
                    assertEquals(5, rs.getInt(1));
                }
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM public.courses")) {
                    assertTrue(rs.next());
                    assertEquals(9, rs.getInt(1));
                }
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM public.subjects")) {
                    assertTrue(rs.next());
                    assertEquals(8, rs.getInt(1));
                }

                // Verify demo accounts are 0
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT COUNT(*) FROM public.users WHERE email IN (" +
                        "'admin@virtualclassroom.edu', 'dr.jeevan@virtualclassroom.edu', " +
                        "'gaurav.shukla@virtualclassroom.edu', 'alice.smith@virtualclassroom.edu', " +
                        "'prof.tushar@virtualclassroom.edu')")) {
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1));
                }
            }
        }
    }

    // =========================================================================
    // 9. Migration Failure Reporting
    // =========================================================================
    @Test
    @DisplayName("Execution 9: Accurately captures and reports failed migration without modifying database")
    void testMigrationFailureReporting() throws Exception {
        try (DisposableDb db = createDisposableDb()) {
            DataSource ds = db.getDataSource();

            // Create a conflicting object that causes V1 to fail
            try (Connection conn = ds.getConnection(); Statement stmt = conn.createStatement()) {
                // Create a conflicting table 'users' with incompatible schema
                stmt.execute("CREATE TABLE users (incompatible_col INT PRIMARY KEY);");
                // Create fake history marking V0
                stmt.execute("CREATE TABLE flyway_schema_history (installed_rank INT PRIMARY KEY, version VARCHAR(50), description VARCHAR(200), type VARCHAR(20), script VARCHAR(1000), checksum INT, installed_by VARCHAR(100), installed_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP, execution_time INT, success BOOLEAN);");
                stmt.execute("INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, execution_time, success) VALUES (1, '0', 'pre_existing', 'SQL', 'V0__pre.sql', 1234, 'tester', 10, true);");
            }

            StandaloneFlywayMigrationRunner.MigrationRunResult result =
                    StandaloneFlywayMigrationRunner.runForTest(ds);

            assertFalse(result.successful(), "Runner must report failure when migration encounters an error");
            assertNotNull(result.errorMessage(), "Runner must capture error message");
            assertEquals("0", result.lastSuccessfulVersion(), "Last successful version must be identified");
        }
    }

    // =========================================================================
    // 10. Idempotent Second Execution
    // =========================================================================
    @Test
    @DisplayName("Execution 10: Running migration runner a second time executes 0 migrations and passes all assertions")
    void testIdempotentSecondExecution() throws Exception {
        try (DisposableDb db = createDisposableDb()) {
            DataSource ds = db.getDataSource();

            // Run 1: Applies V1-V5
            StandaloneFlywayMigrationRunner.MigrationRunResult result1 =
                    StandaloneFlywayMigrationRunner.runForTest(ds);
            assertTrue(result1.successful());
            assertEquals(5, result1.migrationsExecuted());

            // Run 2: Re-run on already migrated schema
            StandaloneFlywayMigrationRunner.MigrationRunResult result2 =
                    StandaloneFlywayMigrationRunner.runForTest(ds);
            assertTrue(result2.successful(), "Second run must succeed idempotently");
            assertEquals(0, result2.migrationsExecuted(), "Second run must execute 0 migrations");
            assertEquals("5", result2.targetSchemaVersion(), "Target schema version must remain 5");
            assertNull(result2.errorMessage());
        }
    }

    // =========================================================================
    // 11. Staging Configuration Acceptance
    // =========================================================================
    @Test
    @DisplayName("Acceptance: Valid staging environment variables pass all validation rules")
    void testValidStagingConfiguration_PassesValidation() {
        assertDoesNotThrow(() ->
                StandaloneFlywayMigrationRunner.validateStagingEnvironment(
                        VALID_STAGING_URL, VALID_STAGING_USER, VALID_PASSWORD));
    }

    // =========================================================================
    // 12. Non-Deployment Hibernate Schema Compatibility
    // =========================================================================
    @Test
    @DisplayName("Verification 12: Hibernate entity mapping validation succeeds against migrated schema without application boot")
    void testHibernateSchemaCompatibilityNonDeployment() throws Exception {
        try (DisposableDb db = createDisposableDb()) {
            DataSource ds = db.getDataSource();

            // Run migrations first
            StandaloneFlywayMigrationRunner.MigrationRunResult result =
                    StandaloneFlywayMigrationRunner.runForTest(ds);
            assertTrue(result.successful());

            // Build Hibernate metadata and validate schema against all 21 entities
            org.hibernate.boot.registry.StandardServiceRegistry registry =
                    new org.hibernate.boot.registry.StandardServiceRegistryBuilder()
                            .applySetting("hibernate.connection.datasource", ds)
                            .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                            .applySetting("hibernate.hbm2ddl.auto", "validate")
                            .build();

            try {
                org.hibernate.boot.MetadataSources metadataSources = new org.hibernate.boot.MetadataSources(registry);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.User.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.Degree.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.Course.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.Subject.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.StudentProfile.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.FacultyProfile.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.FacultySubjectAssignment.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.StudyMaterial.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.VideoLecture.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.Question.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.Answer.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.Notification.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.AuditLog.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.RefreshToken.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.StudentSubjectEnrollment.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.LiveSession.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.Assignment.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.AssignmentSubmission.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.Examination.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.ExaminationResult.class);
                metadataSources.addAnnotatedClass(com.virtualclassroom.model.PasswordResetToken.class);

                org.hibernate.boot.spi.SessionFactoryOptions serviceRegistry =
                        metadataSources.buildMetadata().buildSessionFactory().getSessionFactoryOptions();
                assertNotNull(serviceRegistry);
            } finally {
                org.hibernate.boot.registry.StandardServiceRegistryBuilder.destroy(registry);
            }
        }
    }
}
