package com.virtualclassroom;

import com.virtualclassroom.controller.AuthController;
import com.virtualclassroom.dto.AuthResponse;
import com.virtualclassroom.dto.LoginRequest;
import com.virtualclassroom.dto.RefreshTokenRequest;
import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.User;
import com.virtualclassroom.model.UserStatus;
import com.virtualclassroom.security.CustomUserDetailsService;
import com.virtualclassroom.security.JwtAuthenticationFilter;
import com.virtualclassroom.security.SecurityConfig;
import com.virtualclassroom.service.AuthService;
import com.virtualclassroom.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.nio.file.Path;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Milestone 16.1 - Automated Verification for Staging Configuration Remediation
 * Covers:
 * 1. Staging Spring Boot profile parsing and datasource configuration contract.
 * 2. Storage bucket allowlist enforcement for canonical vs suffixed buckets.
 * 3. Cross-site refresh-cookie behavior (SameSite=None; Secure for HTTPS, SameSite=Lax for HTTP).
 * 4. Refresh token rotation and logout cookie clearing attribute parity.
 * 5. CORS credential policy and origin parsing.
 */
class StagingConfigurationRemediationTest {

    @TempDir
    Path tempStorageDir;

    // =========================================================================
    // 1. Staging Profile Configuration Tests
    // =========================================================================

    @Test
    @DisplayName("Staging Profile: application-staging.yml exists and defines correct contract")
    void stagingProfile_ConfigurationFile_ValidatesContract() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource("application-staging.yml"));
        Properties props = yaml.getObject();

        assertNotNull(props, "application-staging.yml must be readable");
        assertEquals("${DATABASE_URL}", props.getProperty("spring.datasource.url"));
        assertEquals("${DATABASE_USERNAME}", props.getProperty("spring.datasource.username"));
        assertEquals("${DATABASE_PASSWORD}", props.getProperty("spring.datasource.password"));
        assertEquals("org.postgresql.Driver", props.getProperty("spring.datasource.driver-class-name"));
        assertEquals("validate", props.getProperty("spring.jpa.hibernate.ddl-auto"));
        assertEquals("true", props.getProperty("spring.flyway.enabled"));
        assertEquals("true", props.getProperty("spring.flyway.baseline-on-migrate"));
        assertEquals("${COOKIE_SECURE:true}", props.getProperty("app.security.cookie-secure"));
        assertEquals("${COOKIE_SAME_SITE:None}", props.getProperty("app.security.cookie-same-site"));
        assertEquals("${STORAGE_PROVIDER:supabase}", props.getProperty("app.storage.provider"));
        assertEquals("${SUPABASE_STORAGE_BUCKET:academic-assets}", props.getProperty("app.storage.supabase.bucket"));
    }

    // =========================================================================
    // 2. Storage Bucket Allowlist Tests
    // =========================================================================

    @Test
    @DisplayName("Storage: Accepts canonical staging bucket 'academic-assets'")
    void storageService_BucketAllowlist_AcceptsCanonicalStagingBucket() {
        assertDoesNotThrow(() -> new FileStorageService(
                "local", "", "", "academic-assets",
                tempStorageDir.toString(), 10 * 1024 * 1024
        ));
    }

    @Test
    @DisplayName("Storage: Rejects unapproved suffixed bucket 'academic-assets-staging'")
    void storageService_BucketAllowlist_RejectsSuffixedBucket() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                new FileStorageService(
                        "local", "", "", "academic-assets-staging",
                        tempStorageDir.toString(), 10 * 1024 * 1024
                )
        );
        assertTrue(ex.getMessage().contains("not in the approved bucket allowlist"));
        assertTrue(ex.getMessage().contains("academic-assets-staging"));
    }

    @Test
    @DisplayName("Storage: Accepts all predefined standard buckets")
    void storageService_BucketAllowlist_AcceptsAllStandardBuckets() {
        for (String bucket : FileStorageService.ALLOWED_BUCKETS) {
            assertDoesNotThrow(() -> new FileStorageService(
                    "local", "", "", bucket,
                    tempStorageDir.toString(), 10 * 1024 * 1024
            ), "Should accept allowed bucket: " + bucket);
        }
    }

    // =========================================================================
    // 3. Cross-Site Refresh-Cookie Configuration Tests
    // =========================================================================

    @Test
    @DisplayName("Cookies: Staging HTTPS default configures SameSite=None and Secure")
    void authController_CrossSiteStagingCookie_ConfiguresSameSiteNoneAndSecure() {
        AuthResponse rawAuthResponse = AuthResponse.builder()
                .token("jwt-access-token")
                .tokenType("Bearer")
                .id(1L)
                .email("student@university.edu")
                .fullName("Test Student")
                .role("ROLE_STUDENT")
                .status("APPROVED")
                .refreshToken("test-refresh-token-xyz")
                .build();

        AuthService mockAuth = mock(AuthService.class);
        when(mockAuth.login(any(), any())).thenReturn(rawAuthResponse);

        // cookieSecure = true (Staging / Production HTTPS default)
        AuthController controller = new AuthController(mockAuth, true);
        assertEquals("None", controller.getCookieSameSite());
        assertTrue(controller.isCookieSecure());

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.login(new LoginRequest("student@university.edu", "Password123!"), request, response);

        String setCookie = response.getHeader("Set-Cookie");
        assertNotNull(setCookie, "Set-Cookie header must be emitted");
        assertTrue(setCookie.contains("SameSite=None"), "Cross-site staging must use SameSite=None");
        assertTrue(setCookie.contains("Secure"), "SameSite=None must be paired with Secure");
        assertTrue(setCookie.contains("HttpOnly"), "Must be HttpOnly");
        assertTrue(setCookie.contains("Path=/api/v1/auth"), "Must restrict to auth path");
    }

    @Test
    @DisplayName("Cookies: Local HTTP default configures SameSite=Lax without Secure")
    void authController_LocalDevelopmentCookie_ConfiguresSameSiteLaxWithoutSecure() {
        AuthResponse rawAuthResponse = AuthResponse.builder()
                .token("jwt-access-token")
                .refreshToken("test-refresh-token-local")
                .build();

        AuthService mockAuth = mock(AuthService.class);
        when(mockAuth.login(any(), any())).thenReturn(rawAuthResponse);

        // cookieSecure = false (Local HTTP default)
        AuthController controller = new AuthController(mockAuth, false);
        assertEquals("Lax", controller.getCookieSameSite());
        assertFalse(controller.isCookieSecure());

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        controller.login(new LoginRequest("student@university.edu", "Password123!"), request, response);

        String setCookie = response.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("SameSite=Lax"), "Local development must use SameSite=Lax");
        assertFalse(setCookie.contains("Secure"), "HTTP development must not require Secure flag");
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Path=/api/v1/auth"));
    }

    // =========================================================================
    // 4. Token Rotation and Logout Attribute Parity Tests
    // =========================================================================

    @Test
    @DisplayName("Cookies: Refresh token rotation emits matching cross-site attributes")
    void authController_RefreshTokenRotation_EmitsMatchingCrossSiteCookie() {
        AuthResponse refreshedResponse = AuthResponse.builder()
                .token("new-jwt-access-token")
                .refreshToken("new-rotated-refresh-token")
                .build();

        AuthService mockAuth = mock(AuthService.class);
        when(mockAuth.refreshToken(eq("old-refresh-token"), any())).thenReturn(refreshedResponse);

        AuthController controller = new AuthController(mockAuth, true, "None");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        RefreshTokenRequest refreshReq = new RefreshTokenRequest("old-refresh-token");
        var entity = controller.refresh(refreshReq, null, request, response);

        assertNotNull(entity.getBody());
        assertNull(entity.getBody().getData().getRefreshToken(), "Refresh token must not be in payload for browser clients");

        String setCookie = response.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken=new-rotated-refresh-token"));
        assertTrue(setCookie.contains("SameSite=None"));
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("HttpOnly"));
    }

    @Test
    @DisplayName("Cookies: Logout clears cookie with identical Path, Secure and SameSite attributes")
    void authController_Logout_ClearsCookieWithMatchingAttributes() {
        AuthService mockAuth = mock(AuthService.class);
        AuthController controller = new AuthController(mockAuth, true, "None");

        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.logout(new RefreshTokenRequest("active-token"), null, response);

        String setCookie = response.getHeader("Set-Cookie");
        assertNotNull(setCookie);
        assertTrue(setCookie.contains("refreshToken="));
        assertTrue(setCookie.contains("Max-Age=0"));
        assertTrue(setCookie.contains("Path=/api/v1/auth"));
        assertTrue(setCookie.contains("SameSite=None"));
        assertTrue(setCookie.contains("Secure"));
        assertTrue(setCookie.contains("HttpOnly"));
    }

    // =========================================================================
    // 5. CORS Credential Policy Tests
    // =========================================================================

    @Test
    @DisplayName("CORS: Configuration permits credentials and trims configured origins")
    void securityConfig_CorsCredentials_AllowsExactOriginsAndHeaders() throws Exception {
        SecurityConfig config = new SecurityConfig(
                mock(CustomUserDetailsService.class),
                mock(JwtAuthenticationFilter.class)
        );

        // Inject allowed origins with surrounding whitespace
        var field = SecurityConfig.class.getDeclaredField("allowedOrigins");
        field.setAccessible(true);
        field.set(config, "https://virtual-classroom-staging.vercel.app, http://localhost:5173 ");

        CorsConfigurationSource source = config.corsConfigurationSource();
        assertNotNull(source);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setRequestURI("/api/v1/auth/login");
        CorsConfiguration cors = source.getCorsConfiguration(req);

        assertNotNull(cors);
        assertTrue(Boolean.TRUE.equals(cors.getAllowCredentials()), "CORS must allow credentials for cross-origin cookies");
        List<String> origins = cors.getAllowedOrigins();
        assertNotNull(origins);
        assertEquals(2, origins.size());
        assertEquals("https://virtual-classroom-staging.vercel.app", origins.get(0));
        assertEquals("http://localhost:5173", origins.get(1), "Whitespace should be cleanly trimmed");
        assertFalse(origins.contains("*"), "Wildcard origin cannot be used with allowCredentials");

        List<String> headers = cors.getAllowedHeaders();
        assertTrue(headers.contains("Origin"));
        assertTrue(headers.contains("Authorization"));
        assertTrue(headers.contains("X-Token-Transport"));
        assertTrue(headers.contains("X-Client-Type"));
    }
}
