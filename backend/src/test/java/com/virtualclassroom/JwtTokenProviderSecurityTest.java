package com.virtualclassroom;

import com.virtualclassroom.security.JwtTokenProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderSecurityTest {

    private static final String VALID_SECRET = "dGVzdC1sb2NhbC1kZXZlbG9wbWVudC1zZWNyZXQta2V5LWZvci12aXJ0dWFsLWNsYXNzcm9vbQ==";

    @Test
    @DisplayName("Throws exception if JWT secret is null or blank")
    void constructor_BlankSecret_ThrowsException() {
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider("", 86400000L));
        assertThrows(IllegalStateException.class, () -> new JwtTokenProvider(null, 86400000L));
    }

    @Test
    @DisplayName("Throws exception if production profile uses fallback dev secret")
    void constructor_ProductionProfileWithDevSecret_ThrowsException() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new JwtTokenProvider(JwtTokenProvider.LOCAL_DEV_FALLBACK_SECRET, 86400000L, prodEnv));

        assertTrue(ex.getMessage().contains("Production profile cannot use the default development JWT secret"));
    }

    @Test
    @DisplayName("Permits fallback dev secret in local/test profile")
    void constructor_LocalProfileWithDevSecret_Succeeds() {
        MockEnvironment localEnv = new MockEnvironment();
        localEnv.setActiveProfiles("local");

        assertDoesNotThrow(() -> new JwtTokenProvider(JwtTokenProvider.LOCAL_DEV_FALLBACK_SECRET, 86400000L, localEnv));
    }

    @Test
    @DisplayName("Permits custom secure 256-bit secret in production profile")
    void constructor_ProductionProfileWithSecureSecret_Succeeds() {
        MockEnvironment prodEnv = new MockEnvironment();
        prodEnv.setActiveProfiles("prod");

        // 32-byte secure base64 string
        String secureSecret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
        assertDoesNotThrow(() -> new JwtTokenProvider(secureSecret, 86400000L, prodEnv));
    }
}
