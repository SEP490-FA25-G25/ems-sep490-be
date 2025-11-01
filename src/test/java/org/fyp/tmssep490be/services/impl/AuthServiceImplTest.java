package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.auth.AuthResponse;
import org.fyp.tmssep490be.dtos.auth.LoginRequest;
import org.fyp.tmssep490be.dtos.auth.RefreshTokenRequest;
import org.fyp.tmssep490be.entities.Role;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.UserRole;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.exceptions.InvalidTokenException;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.security.JwtTokenProvider;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthServiceImpl
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceImplTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private UserAccount testUser;
    private UserPrincipal testUserPrincipal;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        // Create test user with email as login
        testUser = new UserAccount();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("encodedPassword");
        testUser.setFullName("Test User");
        testUser.setStatus(UserStatus.ACTIVE);

        // Create role
        Role role = new Role();
        role.setId(1L);
        role.setCode("ADMIN");
        role.setName("Administrator");

        UserRole userRole = new UserRole();
        UserRole.UserRoleId userRoleId = new UserRole.UserRoleId();
        userRoleId.setUserId(1L);
        userRoleId.setRoleId(1L);
        userRole.setId(userRoleId);
        userRole.setRole(role);
        userRole.setUserAccount(testUser);

        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(userRole);
        testUser.setUserRoles(userRoles);
        testUser.setUserBranches(new HashSet<>());

        // Create UserPrincipal (6 arguments: id, email, passwordHash, fullName, status, authorities)
        testUserPrincipal = new UserPrincipal(
                1L,
                "test@example.com",
                "encodedPassword",
                "Test User",
                UserStatus.ACTIVE,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        // Create mock authentication
        authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(testUserPrincipal);
        lenient().when(authentication.getAuthorities()).thenAnswer(invocation ->
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(jwtTokenProvider.generateAccessToken(authentication))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(1L, "test@example.com"))
                .thenReturn("refresh-token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getFullName()).isEqualTo("Test User");
        assertThat(response.getRoles()).containsExactly("ADMIN");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider).generateAccessToken(authentication);
        verify(jwtTokenProvider).generateRefreshToken(1L, "test@example.com");
    }

    @Test
    @DisplayName("Should throw exception when credentials are invalid")
    void shouldThrowExceptionWhenCredentialsInvalid() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtTokenProvider, never()).generateAccessToken(any());
        verify(jwtTokenProvider, never()).generateRefreshToken(anyLong(), anyString());
    }

    @Test
    @DisplayName("Should refresh token successfully with valid refresh token")
    void shouldRefreshTokenSuccessfully() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(jwtTokenProvider.validateRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("valid-refresh-token")).thenReturn(1L);
        when(userAccountRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(jwtTokenProvider.generateAccessToken(1L, "test@example.com", "ROLE_ADMIN"))
                .thenReturn("new-access-token");
        when(jwtTokenProvider.generateRefreshToken(1L, "test@example.com"))
                .thenReturn("new-refresh-token");

        // Act
        AuthResponse response = authService.refreshToken(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("test@example.com");
        assertThat(response.getRoles()).containsExactly("ADMIN");

        verify(jwtTokenProvider).validateRefreshToken("valid-refresh-token");
        verify(jwtTokenProvider).getUserIdFromJwt("valid-refresh-token");
        verify(userAccountRepository).findById(1L);
    }

    @Test
    @DisplayName("Should throw exception when refresh token is invalid")
    void shouldThrowExceptionWhenRefreshTokenInvalid() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("invalid-refresh-token");

        when(jwtTokenProvider.validateRefreshToken("invalid-refresh-token")).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessage("Invalid or expired refresh token");

        verify(jwtTokenProvider).validateRefreshToken("invalid-refresh-token");
        verify(jwtTokenProvider, never()).getUserIdFromJwt(anyString());
        verify(userAccountRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Should throw exception when user not found during refresh")
    void shouldThrowExceptionWhenUserNotFoundDuringRefresh() {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest("valid-refresh-token");

        when(jwtTokenProvider.validateRefreshToken("valid-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromJwt("valid-refresh-token")).thenReturn(999L);
        when(userAccountRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessage("User not found");

        verify(jwtTokenProvider).validateRefreshToken("valid-refresh-token");
        verify(jwtTokenProvider).getUserIdFromJwt("valid-refresh-token");
        verify(userAccountRepository).findById(999L);
    }

    @Test
    @DisplayName("Should handle user with multiple roles")
    void shouldHandleUserWithMultipleRoles() {
        // Arrange
        Role managerRole = new Role();
        managerRole.setId(2L);
        managerRole.setCode("MANAGER");
        managerRole.setName("Manager");

        UserRole userRole2 = new UserRole();
        UserRole.UserRoleId userRoleId2 = new UserRole.UserRoleId();
        userRoleId2.setUserId(1L);
        userRoleId2.setRoleId(2L);
        userRole2.setId(userRoleId2);
        userRole2.setRole(managerRole);
        userRole2.setUserAccount(testUser);

        testUser.getUserRoles().add(userRole2);

        UserPrincipal multiRoleUser = new UserPrincipal(
                1L,
                "test@example.com",
                "encodedPassword",
                "Test User",
                UserStatus.ACTIVE,
                List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_MANAGER")
                )
        );

        Authentication multiRoleAuth = mock(Authentication.class);
        when(multiRoleAuth.getPrincipal()).thenReturn(multiRoleUser);
        when(multiRoleAuth.getAuthorities()).thenAnswer(invocation ->
                List.of(
                        new SimpleGrantedAuthority("ROLE_ADMIN"),
                        new SimpleGrantedAuthority("ROLE_MANAGER")
                )
        );

        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(multiRoleAuth);
        when(jwtTokenProvider.generateAccessToken(multiRoleAuth))
                .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(1L, "test@example.com"))
                .thenReturn("refresh-token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertThat(response.getRoles()).containsExactlyInAnyOrder("ADMIN", "MANAGER");
    }

    @Test
    @DisplayName("Should handle disabled user account")
    void shouldHandleDisabledUserAccount() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.DisabledException("Account is disabled"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(org.springframework.security.authentication.DisabledException.class)
                .hasMessage("Account is disabled");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Should handle locked user account")
    void shouldHandleLockedUserAccount() {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.LockedException("Account is locked"));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(org.springframework.security.authentication.LockedException.class)
                .hasMessage("Account is locked");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
