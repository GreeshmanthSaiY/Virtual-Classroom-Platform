package com.virtualclassroom;

import com.virtualclassroom.model.Role;
import com.virtualclassroom.model.User;
import com.virtualclassroom.model.UserStatus;
import com.virtualclassroom.security.CustomUserDetailsService;
import com.virtualclassroom.security.JwtAuthenticationFilter;
import com.virtualclassroom.security.JwtTokenProvider;
import com.virtualclassroom.security.UserPrincipal;
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
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
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

    @Test
    @DisplayName("Filter blocks suspended user and sends 401 Unauthorized")
    void doFilter_SuspendedUser_BlocksWith401() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer mock.jwt.token");
        when(tokenProvider.validateToken("mock.jwt.token")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("mock.jwt.token")).thenReturn(99L);

        User suspendedUser = User.builder()
                .id(99L)
                .email("suspended@test.com")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.SUSPENDED)
                .build();
        UserPrincipal principal = UserPrincipal.create(suspendedUser);
        when(customUserDetailsService.loadUserById(99L)).thenReturn(principal);

        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        jwtFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("SUSPENDED"));
    }

    @Test
    @DisplayName("Filter blocks pending user and sends 401 Unauthorized")
    void doFilter_PendingUser_BlocksWith401() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer mock.jwt.token");
        when(tokenProvider.validateToken("mock.jwt.token")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("mock.jwt.token")).thenReturn(100L);

        User pendingUser = User.builder()
                .id(100L)
                .email("pending@test.com")
                .role(Role.ROLE_FACULTY)
                .status(UserStatus.PENDING)
                .build();
        UserPrincipal principal = UserPrincipal.create(pendingUser);
        when(customUserDetailsService.loadUserById(100L)).thenReturn(principal);

        StringWriter stringWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(stringWriter));

        jwtFilter.doFilter(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("PENDING"));
    }

    @Test
    @DisplayName("Filter permits approved user and sets SecurityContext")
    void doFilter_ApprovedUser_SetsAuthenticationAndContinuesChain() throws ServletException, IOException {
        when(request.getHeader("Authorization")).thenReturn("Bearer mock.jwt.token");
        when(tokenProvider.validateToken("mock.jwt.token")).thenReturn(true);
        when(tokenProvider.getUserIdFromJWT("mock.jwt.token")).thenReturn(1L);

        User approvedUser = User.builder()
                .id(1L)
                .email("approved@test.com")
                .passwordHash("hashed")
                .role(Role.ROLE_STUDENT)
                .status(UserStatus.APPROVED)
                .build();
        UserPrincipal principal = UserPrincipal.create(approvedUser);
        when(customUserDetailsService.loadUserById(1L)).thenReturn(principal);

        jwtFilter.doFilter(request, response, filterChain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("approved@test.com", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(filterChain, times(1)).doFilter(request, response);
    }
}
