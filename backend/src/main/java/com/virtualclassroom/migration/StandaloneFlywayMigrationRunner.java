package com.virtualclassroom.migration;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationState;
import org.flywaydb.core.api.output.MigrateOutput;
import org.flywaydb.core.api.output.MigrateResult;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

/**
 * Standalone, independently executable Flyway migration runner for the Virtual Classroom Platform.
 * Executes migrations V1 through V5 without launching the Spring Boot web application.
 *
 * <p>STRICT SAFETY GUARANTEES:
 * <ul>
 *   <li>Never launches SpringApplication or Spring MVC.</li>
 *   <li>Never opens public HTTP ports, controllers, or background tasks.</li>
 *   <li>Enforces fail-closed validation of Supabase Staging pooler hostname, port, database, SSL, and tenant username.</li>
 *   <li>Strictly rejects any production targets (zero production overrides).</li>
 *   <li>Maintains canonical public.flyway_schema_history using bundled Flyway Core 10.20.1.</li>
 *   <li>Never prints or logs database passwords.</li>
 * </ul>
 */
public class StandaloneFlywayMigrationRunner {

    public static final String EXPECTED_STAGING_HOST = "aws-0-ap-south-1.pooler.supabase.com";
    public static final int EXPECTED_PORT = 5432;
    public static final String EXPECTED_DATABASE = "postgres";
    public static final String EXPECTED_STAGING_TENANT_USER = "postgres.zrbpjcfssbtxxgrjtsca";
    public static final String EXPECTED_STAGING_PROJECT_REF = "zrbpjcfssbtxxgrjtsca";
    public static final String REQUIRED_SSL_MODE = "sslmode=require";

    public static final List<String> EXPECTED_TABLES = List.of(
            "users",
            "degrees",
            "courses",
            "subjects",
            "student_profiles",
            "faculty_profiles",
            "faculty_subject_assignments",
            "study_materials",
            "video_lectures",
            "questions",
            "answers",
            "notifications",
            "audit_logs",
            "refresh_tokens",
            "student_subject_enrollments",
            "live_sessions",
            "assignments",
            "assignment_submissions",
            "examinations",
            "examination_results",
            "password_reset_tokens"
    );

    public static final List<String> TARGET_DEMO_EMAILS = List.of(
            "admin@virtualclassroom.edu",
            "dr.jeevan@virtualclassroom.edu",
            "gaurav.shukla@virtualclassroom.edu",
            "alice.smith@virtualclassroom.edu",
            "prof.tushar@virtualclassroom.edu"
    );

    public record MigrationRunResult(
            boolean successful,
            int migrationsExecuted,
            String targetSchemaVersion,
            List<String> executedMigrationSummaries,
            String lastSuccessfulVersion,
            String failedVersion,
            String errorMessage
    ) {}

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("   VIRTUAL CLASSROOM PLATFORM — STANDALONE FLYWAY MIGRATION RUNNER");
        System.out.println("================================================================================");

        try {
            String url = resolveEnvVar("DATABASE_URL");
            if (url == null || url.trim().isEmpty()) {
                url = "jdbc:postgresql://" + EXPECTED_STAGING_HOST + ":" + EXPECTED_PORT + "/" + EXPECTED_DATABASE + "?sslmode=require";
            }
            String username = resolveEnvVar("DATABASE_USERNAME");
            if (username == null || username.trim().isEmpty()) {
                username = EXPECTED_STAGING_TENANT_USER;
            }
            String password = resolveEnvVar("DATABASE_PASSWORD");

            MigrationRunResult result = runForStaging(url, username, password);

            if (result.successful()) {
                System.out.println("\n================================================================================");
                System.out.println("   MIGRATION RUN COMPLETED SUCCESSFULLY — EXIT CODE 0");
                System.out.println("================================================================================");
                System.exit(0);
            } else {
                System.err.println("\n================================================================================");
                System.err.println("   MIGRATION RUN FAILED — EXIT CODE 1");
                System.err.println("   Failed Version: " + result.failedVersion());
                System.err.println("   Last Successful Version: " + result.lastSuccessfulVersion());
                System.err.println("   Error Message: " + result.errorMessage());
                System.err.println("================================================================================");
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("\nFATAL EXECUTION ABORTED: " + e.getMessage());
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }

    /**
     * Resolves an environment variable from System.getenv or a local .env file.
     * Never prints or logs resolved secret values.
     */
    public static String resolveEnvVar(String key) {
        String val = System.getenv(key);
        if (val != null && !val.trim().isEmpty()) {
            return val.trim();
        }
        List<Path> candidates = List.of(
                Paths.get(".env"),
                Paths.get("../.env"),
                Paths.get("backend/.env")
        );
        for (Path p : candidates) {
            if (Files.exists(p)) {
                try {
                    for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                        line = line.trim();
                        if (line.startsWith("#") || !line.contains("=")) {
                            continue;
                        }
                        int eq = line.indexOf('=');
                        String k = line.substring(0, eq).trim();
                        if (k.equals(key)) {
                            String v = line.substring(eq + 1).trim();
                            if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
                                v = v.substring(1, v.length() - 1);
                            }
                            if (!v.isEmpty()) {
                                return v;
                            }
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
        return null;
    }

    /**
     * Executes the migration runner with full fail-closed staging environment validation.
     */
    public static MigrationRunResult runForStaging(String url, String username, String password) {
        validateStagingEnvironment(url, username, password);

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            verifyDatabaseIdentity(conn, EXPECTED_DATABASE, EXPECTED_STAGING_TENANT_USER);
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to verify pre-migration database identity: " + e.getMessage(), e);
        }

        Flyway flyway = Flyway.configure()
                .dataSource(url, username, password)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        return executeMigrationInternal(flyway, flyway.getConfiguration().getDataSource(), true);
    }

    /**
     * Executes the migration runner for isolated test environments (e.g. testcontainers / local postgres).
     */
    public static MigrationRunResult runForTest(DataSource dataSource) {
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        return executeMigrationInternal(flyway, dataSource, false);
    }

    /**
     * Validates that the provided configuration strictly targets the approved Supabase staging project.
     * Throws IllegalArgumentException on any violation.
     */
    public static void validateStagingEnvironment(String url, String username, String password) {
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("DATABASE_URL environment variable is mandatory and cannot be empty.");
        }
        if (username == null || username.trim().isEmpty()) {
            throw new IllegalArgumentException("DATABASE_USERNAME environment variable is mandatory and cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("DATABASE_PASSWORD environment variable is mandatory and cannot be empty.");
        }

        String normalizedUrl = url.trim().toLowerCase(Locale.ROOT);
        String normalizedUser = username.trim();

        // 1. Strict rejection of production targets (Zero production override)
        if (normalizedUrl.contains("prod") || normalizedUrl.contains("production") ||
                normalizedUser.contains("prod") || normalizedUser.contains("production")) {
            throw new IllegalArgumentException("PRODUCTION TARGET DETECTED. Standalone runner strictly rejects production execution. Aborting.");
        }

        // 2. Reject localhost or arbitrary fallback targets
        if (normalizedUrl.contains("localhost") || normalizedUrl.contains("127.0.0.1") || normalizedUrl.contains("h2:")) {
            throw new IllegalArgumentException("STAGING SAFETY VIOLATION: Database URL must not target local/embedded databases. URL must target staging pooler.");
        }

        // 3. Hostname validation
        if (!normalizedUrl.contains(EXPECTED_STAGING_HOST.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("INVALID HOST: DATABASE_URL must target exact staging pooler: " + EXPECTED_STAGING_HOST);
        }

        // 4. Port validation
        if (!normalizedUrl.contains(":" + EXPECTED_PORT + "/")) {
            throw new IllegalArgumentException("INVALID PORT: DATABASE_URL must connect on port " + EXPECTED_PORT + " (Supavisor Session Pooler).");
        }

        // 5. Database name validation
        if (!normalizedUrl.contains("/" + EXPECTED_DATABASE + "?") && !normalizedUrl.endsWith("/" + EXPECTED_DATABASE)) {
            throw new IllegalArgumentException("INVALID DATABASE NAME: DATABASE_URL must specify target database /" + EXPECTED_DATABASE);
        }

        // 6. SSL requirement validation
        if (!normalizedUrl.contains(REQUIRED_SSL_MODE.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("SECURITY VIOLATION: DATABASE_URL must enforce SSL with parameter '" + REQUIRED_SSL_MODE + "'.");
        }

        // 7. Tenant-qualified username validation
        if (!normalizedUser.equals(EXPECTED_STAGING_TENANT_USER)) {
            throw new IllegalArgumentException("INVALID USERNAME: DATABASE_USERNAME must be tenant-qualified ('" + EXPECTED_STAGING_TENANT_USER + "') for pooler routing.");
        }
    }

    /**
     * Verifies that the connected database matches the expected database name and is a valid PostgreSQL engine.
     */
    public static void verifyDatabaseIdentity(Connection conn, String expectedDatabase) throws SQLException {
        verifyDatabaseIdentity(conn, expectedDatabase, null);
    }

    /**
     * Verifies that the connected database matches the expected database name, session user, and is a valid PostgreSQL engine.
     */
    public static void verifyDatabaseIdentity(Connection conn, String expectedDatabase, String expectedUser) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT current_database(), current_user, version()")) {
            if (!rs.next()) {
                throw new IllegalStateException("Database identity query returned no rows.");
            }
            String actualDb = rs.getString(1);
            String sessionUser = rs.getString(2);
            String versionStr = rs.getString(3);

            if (!expectedDatabase.equalsIgnoreCase(actualDb)) {
                throw new IllegalStateException("Database identity mismatch! Connected to '" + actualDb + "', expected '" + expectedDatabase + "'.");
            }
            if (expectedUser != null && !expectedUser.isBlank()) {
                boolean userMatches = expectedUser.equalsIgnoreCase(sessionUser) ||
                        "postgres".equalsIgnoreCase(sessionUser) ||
                        sessionUser.toLowerCase(Locale.ROOT).startsWith("postgres.");
                if (!userMatches) {
                    throw new IllegalStateException("Database session user mismatch! Connected as '" + sessionUser +
                            "', expected '" + expectedUser + "' or 'postgres'.");
                }
            }
            if (versionStr == null || !versionStr.toLowerCase(Locale.ROOT).contains("postgresql")) {
                throw new IllegalStateException("Database engine is not PostgreSQL! Version report: " + versionStr);
            }

            System.out.println("   [Verified] Target Database : " + actualDb);
            System.out.println("   [Verified] Session User    : " + sessionUser);
            System.out.println("   [Verified] Engine Version  : " + versionStr.split("\n")[0]);
        }
    }

    private static MigrationRunResult executeMigrationInternal(Flyway flyway, DataSource dataSource, boolean isStagingTarget) {
        System.out.println("\n[1/3] Initializing Flyway Migration Engine...");
        MigrationInfo[] preInfo = flyway.info().all();
        System.out.println("   Canonical migration files discovered: " + preInfo.length);
        for (MigrationInfo info : preInfo) {
            System.out.println("     - " + info.getScript() + " [" + info.getState() + "]");
        }

        System.out.println("\n[2/3] Executing Database Migrations V1 through V5...");
        List<String> summaries = new ArrayList<>();
        String lastSuccess = null;
        String failedVersion = null;

        try {
            MigrateResult migrateResult = flyway.migrate();
            System.out.println("   Migrations executed in this run: " + migrateResult.migrationsExecuted);
            System.out.println("   Current schema target version   : " + migrateResult.targetSchemaVersion);

            if (migrateResult.migrations != null) {
                for (MigrateOutput output : migrateResult.migrations) {
                    String summary = String.format("V%s (%s) -> execution_time=%dms",
                            output.version, output.description, output.executionTime);
                    summaries.add(summary);
                    System.out.println("     + Installed: " + summary);
                    lastSuccess = output.version;
                }
            }

            // Capture recorded checksums from Flyway Info
            for (MigrationInfo info : flyway.info().all()) {
                if (info.getState() == MigrationState.SUCCESS) {
                    System.out.println(String.format("     * Verified History: V%s - %s [checksum=%s, installed_on=%s]",
                            info.getVersion(), info.getDescription(), info.getChecksum(), info.getInstalledOn()));
                }
            }

            System.out.println("\n[3/3] Executing Post-Migration Integrity Assertions...");
            try (Connection conn = dataSource.getConnection()) {
                verifyPostMigrationState(conn);
            }

            MigrationInfo currentInfo = flyway.info().current();
            String resolvedVersion = (currentInfo != null && currentInfo.getVersion() != null)
                    ? currentInfo.getVersion().getVersion()
                    : (migrateResult.targetSchemaVersion != null ? migrateResult.targetSchemaVersion : "5");

            return new MigrationRunResult(
                    true,
                    migrateResult.migrationsExecuted,
                    resolvedVersion,
                    summaries,
                    lastSuccess != null ? lastSuccess : resolvedVersion,
                    null,
                    null
            );

        } catch (Exception e) {
            // Determine last successful version and failed version from flyway info
            try {
                for (MigrationInfo info : flyway.info().all()) {
                    if (info.getState().isApplied()) {
                        lastSuccess = info.getVersion() != null ? info.getVersion().getVersion() : null;
                    } else if (info.getState() == MigrationState.FAILED) {
                        failedVersion = info.getVersion() != null ? info.getVersion().getVersion() : "UNKNOWN";
                    }
                }
            } catch (Exception ignored) {}

            System.err.println("\n********************************************************************************");
            System.err.println("CRITICAL: MIGRATION EXECUTION FAILED!");
            System.err.println("Last successfully applied version : " + (lastSuccess != null ? "V" + lastSuccess : "NONE"));
            System.err.println("Failed migration version          : " + (failedVersion != null ? "V" + failedVersion : "UNKNOWN"));
            System.err.println("Error details                     : " + e.getMessage());
            System.err.println("--------------------------------------------------------------------------------");
            System.err.println("RECOVERY AND INVESTIGATION INSTRUCTIONS:");
            System.err.println("1. Flyway executes each migration script in its own transaction.");
            System.err.println("2. Any script that succeeded before the failure remains recorded in flyway_schema_history.");
            System.err.println("3. PostgreSQL setval() sequence advancements are not transactionally rolled back.");
            System.err.println("4. DO NOT run 'flyway clean' or manually delete database tables.");
            System.err.println("5. DO NOT manually edit flyway_schema_history records.");
            System.err.println("6. Investigate the failure reason above and resolve before attempting retry.");
            System.err.println("********************************************************************************");

            return new MigrationRunResult(
                    false,
                    summaries.size(),
                    null,
                    summaries,
                    lastSuccess,
                    failedVersion,
                    e.getMessage()
            );
        }
    }

    /**
     * Comprehensive post-migration integrity verification.
     * Asserts that all 21 application tables, 5 migration records, reference data,
     * sequences, and zero demo accounts are confirmed in the live database.
     */
    public static void verifyPostMigrationState(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {

            // 1. Verify Flyway Schema History (Must contain 5 records with success = true)
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT version, success FROM public.flyway_schema_history WHERE version IN ('1','2','3','4','5') ORDER BY installed_rank")) {
                int count = 0;
                while (rs.next()) {
                    count++;
                    boolean success = rs.getBoolean("success");
                    if (!success) {
                        throw new IllegalStateException("Flyway schema history shows failed migration for version: " + rs.getString("version"));
                    }
                }
                if (count != 5) {
                    throw new IllegalStateException("Expected exactly 5 recorded Flyway migrations in flyway_schema_history, but found: " + count);
                }
                System.out.println("   [Assertion Passed] Exactly 5 Flyway migrations verified with success=true.");
            }

            // 2. Verify all 21 expected application tables exist in the public schema
            for (String expectedTable : EXPECTED_TABLES) {
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '" + expectedTable + "')")) {
                    if (!rs.next() || !rs.getBoolean(1)) {
                        throw new IllegalStateException("Missing expected application table: " + expectedTable);
                    }
                }
            }
            System.out.println("   [Assertion Passed] All 21 expected application tables confirmed present.");

            // 3. Verify Academic Reference Data Counts (5 Degrees, 9 Courses, 8 Subjects)
            assertEntityCount(stmt, "degrees", 5);
            assertEntityCount(stmt, "courses", 9);
            assertEntityCount(stmt, "subjects", 8);
            System.out.println("   [Assertion Passed] Reference curriculum counts verified: 5 Degrees, 9 Courses, 8 Subjects.");

            // 4. Verify Demo Account Cleanup (Target demo accounts must be 0)
            StringBuilder emailInClause = new StringBuilder();
            for (int i = 0; i < TARGET_DEMO_EMAILS.size(); i++) {
                if (i > 0) emailInClause.append(", ");
                emailInClause.append("'").append(TARGET_DEMO_EMAILS.get(i)).append("'");
            }
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM public.users WHERE email IN (" + emailInClause + ")")) {
                if (rs.next()) {
                    int demoCount = rs.getInt(1);
                    if (demoCount != 0) {
                        throw new IllegalStateException("V3 PURGE FAILURE: Target demo accounts still exist in users table! Found count: " + demoCount);
                    }
                }
            }

            // 5. Verify zero institutional demo accounts remain
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM public.users WHERE email LIKE '%@virtualclassroom.edu'")) {
                if (rs.next()) {
                    int institutionalCount = rs.getInt(1);
                    if (institutionalCount != 0) {
                        throw new IllegalStateException("Demo domain accounts still exist! Count: " + institutionalCount);
                    }
                }
            }
            System.out.println("   [Assertion Passed] Zero target demo accounts remain in the database.");

            // 6. Verify Child Table Demo Cleanup (No leftover demo child records)
            String[] childTables = {"answers", "questions", "study_materials", "video_lectures", "student_profiles", "faculty_profiles", "faculty_subject_assignments"};
            for (String childTable : childTables) {
                try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM public." + childTable)) {
                    if (rs.next() && rs.getInt(1) != 0) {
                        throw new IllegalStateException("Table '" + childTable + "' should have 0 demo rows following V3 purge, but contains: " + rs.getInt(1));
                    }
                }
            }
            System.out.println("   [Assertion Passed] Zero orphaned demonstration child records verified.");

            // 7. Verify Sequences Reset (users_id_seq and questions_id_seq)
            assertSequenceReset(stmt, "users_id_seq");
            assertSequenceReset(stmt, "questions_id_seq");
            System.out.println("   [Assertion Passed] Primary key sequences (users_id_seq, questions_id_seq) verified reset to 1.");

            // 8. Verify Key Unique Constraints
            String[] expectedConstraints = {"uk_student_subject_term", "uk_assignment_student", "uk_exam_student"};
            for (String constraintName : expectedConstraints) {
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_schema = 'public' AND constraint_name = '" + constraintName + "')")) {
                    if (!rs.next() || !rs.getBoolean(1)) {
                        throw new IllegalStateException("Expected unique constraint missing: " + constraintName);
                    }
                }
            }
            System.out.println("   [Assertion Passed] Key unique constraints (uk_student_subject_term, uk_assignment_student, uk_exam_student) verified.");

            // 9. Verify Custom Indexes (Must have custom idx_* indexes across V1, V4, V5)
            try (ResultSet rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname LIKE 'idx_%'")) {
                if (!rs.next()) {
                    throw new IllegalStateException("Query for custom indexes returned no rows.");
                }
                int indexCount = rs.getInt(1);
                if (indexCount < 24) {
                    throw new IllegalStateException("Custom indexes check failed. Expected at least 24 custom indexes, found: " + indexCount);
                }
                System.out.println("   [Assertion Passed] Custom indexes verified (Count: " + indexCount + ").");
            }
        }
    }

    private static void assertEntityCount(Statement stmt, String tableName, int expectedCount) throws SQLException {
        try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM public." + tableName)) {
            if (!rs.next()) {
                throw new IllegalStateException("Query on " + tableName + " returned no rows.");
            }
            int actual = rs.getInt(1);
            if (actual != expectedCount) {
                throw new IllegalStateException("Reference data count mismatch for '" + tableName + "': expected " + expectedCount + ", found " + actual);
            }
        }
    }

    private static void assertSequenceReset(Statement stmt, String sequenceName) throws SQLException {
        try (ResultSet rs = stmt.executeQuery(
                "SELECT last_value FROM pg_sequences WHERE schemaname = 'public' AND sequencename = '" + sequenceName + "'")) {
            if (rs.next()) {
                long lastVal = rs.getLong("last_value");
                if (lastVal > 1) {
                    throw new IllegalStateException("Sequence '" + sequenceName + "' is not cleanly reset to 1. Current: " + lastVal);
                }
            }
        }
    }
}
