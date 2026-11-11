package com.virtualclassroom;

import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.User;
import com.virtualclassroom.model.UserStatus;
import com.virtualclassroom.security.CustomUserDetailsService;
import com.virtualclassroom.security.JwtAuthenticationFilter;
import com.virtualclassroom.security.JwtTokenProvider;
import com.virtualclassroom.security.UserPrincipal;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterSecurityTest {

    @Mock
    private JwtTokenProvider tokenProvider;
    @Mock
    private CustomUserDetailsService customUserDetailsService;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter jwtFilter;

    @BeforeEach
    void setUp() {
        jwtFilter = new JwtAuthenticationFilter(tokenProvider, customUserDetailsService);
        SecurityContextHolder.clearContext();
    }

    private void seedPreExistingSecurityContext() {
        Authentication preExisting = new UsernamePasswordAuthenticationToken(
                "stale-user", "stale-creds", Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(preExisting);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication(), "Pre-existing context must be set");
    }

    @Test
    @DisplayName("1. Active Account: Filter permits approved user and populates SecurityContext")
    void doFilter_ActiveAccount_SetsAuthenticationAndContinuesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer valid.active.jwt");
        when(tokenProvider.validateToken("valid.active.jwt")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("valid.active.jwt")).thenReturn(1L);

        User approvedUser = User.builder()
                .id(1L)
                .email("active.student@virtualclassroom.edu")
                .passwordHash("hashed")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.APPROVED)
                .build();
        UserPrincipal principal = UserPrincipal.create(approvedUser);
        when(customUserDetailsService.loadUserById(1L)).thenReturn(principal);

        jwtFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("active.student@virtualclassroom.edu", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain, times(1)).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    }

    @Test
    @DisplayName("2. Pending Account: Filter blocks pending user with 401 and clears SecurityContext")
    void doFilter_PendingAccount_BlocksWith401AndClearsContext() throws ServletException, IOException {
        seedPreExistingSecurityContext();

        when(request.getHeader("Authorization")).thenReturn("Bearer pending.jwt.token");
        when(tokenProvider.validateToken("pending.jwt.token")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("pending.jwt.token")).thenReturn(2L);

        User pendingUser = User.builder()
                .id(2L)
                .email("pending.faculty@virtualclassroom.edu")
                .role(Role.ROLE_FACULTY)
                .status(UserStatus.PENDING)
                .build();
        UserPrincipal principal = UserPrincipal.create(pendingUser);
        when(customUserDetailsService.loadUserById(2L)).thenReturn(principal);

        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        jwtFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Security context must be cleared for pending user");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("PENDING"));
    }

    @Test
    @DisplayName("3. Rejected Account: Filter blocks rejected user with 401 and clears SecurityContext")
    void doFilter_RejectedAccount_BlocksWith401AndClearsContext() throws ServletException, IOException {
        seedPreExistingSecurityContext();

        when(request.getHeader("Authorization")).thenReturn("Bearer rejected.jwt.token");
        when(tokenProvider.validateToken("rejected.jwt.token")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("rejected.jwt.token")).thenReturn(3L);

        User rejectedUser = User.builder()
                .id(3L)
                .email("rejected.user@virtualclassroom.edu")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.REJECTED)
                .build();
        UserPrincipal principal = UserPrincipal.create(rejectedUser);
        when(customUserDetailsService.loadUserById(3L)).thenReturn(principal);

        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        jwtFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Security context must be cleared for rejected user");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("REJECTED"));
    }

    @Test
    @DisplayName("4. Suspended Account: Filter blocks suspended user with 401 and clears SecurityContext")
    void doFilter_SuspendedAccount_BlocksWith401AndClearsContext() throws ServletException, IOException {
        seedPreExistingSecurityContext();

        when(request.getHeader("Authorization")).thenReturn("Bearer suspended.jwt.token");
        when(tokenProvider.validateToken("suspended.jwt.token")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("suspended.jwt.token")).thenReturn(4L);

        User suspendedUser = User.builder()
                .id(4L)
                .email("suspended.faculty@virtualclassroom.edu")
                .role(Role.ROLE_FACULTY)
                .status(UserStatus.SUSPENDED)
                .build();
        UserPrincipal principal = UserPrincipal.create(suspendedUser);
        when(customUserDetailsService.loadUserById(4L)).thenReturn(principal);

        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        jwtFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Security context must be cleared for suspended user");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("SUSPENDED"));
    }

    @Test
    @DisplayName("5. Disabled Account: Generic UserDetails with isEnabled=false is blocked with 401")
    void doFilter_DisabledAccount_BlocksWith401() throws ServletException, IOException {
        seedPreExistingSecurityContext();

        when(request.getHeader("Authorization")).thenReturn("Bearer disabled.jwt.token");
        when(tokenProvider.validateToken("disabled.jwt.token")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("disabled.jwt.token")).thenReturn(5L);

        UserDetails disabledDetails = mock(UserDetails.class);
        when(disabledDetails.isEnabled()).thenReturn(false);
        when(customUserDetailsService.loadUserById(5L)).thenReturn(disabledDetails);

        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        jwtFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Security context must be cleared for disabled user");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("INACTIVE"));
    }

    @Test
    @DisplayName("6. Locked Account: Generic UserDetails with isAccountNonLocked=false is blocked with 401")
    void doFilter_LockedAccount_BlocksWith401() throws ServletException, IOException {
        seedPreExistingSecurityContext();

        when(request.getHeader("Authorization")).thenReturn("Bearer locked.jwt.token");
        when(tokenProvider.validateToken("locked.jwt.token")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("locked.jwt.token")).thenReturn(6L);

        UserDetails lockedDetails = mock(UserDetails.class);
        when(lockedDetails.isEnabled()).thenReturn(true);
        when(lockedDetails.isAccountNonLocked()).thenReturn(false);
        when(customUserDetailsService.loadUserById(6L)).thenReturn(lockedDetails);

        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        jwtFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Security context must be cleared for locked user");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("INACTIVE"));
    }

    @Test
    @DisplayName("7. Expired JWT: validateToken returns false, ensures SecurityContext is cleared")
    void doFilter_ExpiredJwt_ClearsContextAndDoesNotAuthenticate() throws ServletException, IOException {
        seedPreExistingSecurityContext();

        when(request.getHeader("Authorization")).thenReturn("Bearer expired.jwt.token");
        when(tokenProvider.validateToken("expired.jwt.token")).thenReturn(false);

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Security context must be null after expired JWT");
        verify(filterChain, times(1)).doFilter(request, response);
        verify(customUserDetailsService, never()).loadUserById(anyLong());
    }

    @Test
    @DisplayName("8. Expired JWT Exception: tokenProvider throws ExpiredJwtException, caught and cleared")
    void doFilter_ExpiredJwtException_ClearsContextSafely() throws ServletException, IOException {
        seedPreExistingSecurityContext();

        when(request.getHeader("Authorization")).thenReturn("Bearer expired.jwt.token");
        when(tokenProvider.validateToken("expired.jwt.token")).thenThrow(new ExpiredJwtException(null, null, "Token has expired"));

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Security context must be null after ExpiredJwtException");
        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    @DisplayName("9. Invalid Signature: tokenProvider throws SignatureException, caught and cleared")
    void doFilter_InvalidSignature_ClearsContextSafely() throws ServletException, IOException {
        seedPreExistingSecurityContext();

        when(request.getHeader("Authorization")).thenReturn("Bearer forged.jwt.token");
        when(tokenProvider.validateToken("forged.jwt.token")).thenThrow(new SignatureException("JWT signature does not match"));

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication(), "Security context must be null after SignatureException");
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
