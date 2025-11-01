package org.fyp.tmssep490be.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.fyp.tmssep490be.dtos.auth.LoginRequest;
import org.fyp.tmssep490be.dtos.auth.RefreshTokenRequest;
import org.fyp.tmssep490be.entities.Role;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.UserRole;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.repositories.RoleRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.repositories.UserRoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashSet;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EntityManager entityManager;

    private UserAccount testUser;
    private Role adminRole;

    @BeforeEach
    void setUp() {
        // Clean up
        userRoleRepository.deleteAll();
        userAccountRepository.deleteAll();
        roleRepository.deleteAll();

        // Create role
        adminRole = new Role();
        adminRole.setCode("ADMIN");
        adminRole.setName("Administrator");
        adminRole = roleRepository.save(adminRole);

        // Create test user with email as login
        testUser = new UserAccount();
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash(passwordEncoder.encode("password123"));
        testUser.setFullName("Test User");
        testUser.setPhone("1234567890");
        testUser.setGender(Gender.MALE);
        testUser.setStatus(UserStatus.ACTIVE);
        testUser.setUserRoles(new HashSet<>());
        testUser.setUserBranches(new HashSet<>());
        testUser = userAccountRepository.save(testUser);

        // Assign role
        UserRole userRole = new UserRole();
        UserRole.UserRoleId userRoleId = new UserRole.UserRoleId();
        userRoleId.setUserId(testUser.getId());
        userRoleId.setRoleId(adminRole.getId());
        userRole.setId(userRoleId);
        userRole.setUserAccount(testUser);
        userRole.setRole(adminRole);
        userRoleRepository.save(userRole);

        // Flush and clear to ensure the role relationship is persisted
        // and will be loaded fresh during authentication
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("Should login successfully with valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        // Arrange - LoginRequest uses email, not username
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.data.email").value("test@example.com"))
                .andExpect(jsonPath("$.data.fullName").value("Test User"))
                .andExpect(jsonPath("$.data.roles", hasSize(1)))
                .andExpect(jsonPath("$.data.roles[0]").value("ADMIN"));
    }

    @Test
    @DisplayName("Should return 401 with invalid credentials")
    void shouldReturn401WithInvalidCredentials() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("test@example.com", "wrongpassword");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("Should return 401 with non-existent email")
    void shouldReturn401WithNonExistentEmail() throws Exception {
        // Arrange
        LoginRequest loginRequest = new LoginRequest("nonexistent@example.com", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 400 with missing email")
    void shouldReturn400WithMissingEmail() throws Exception {
        // Arrange
        String invalidRequest = "{\"password\":\"password123\"}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("Should return 400 with missing password")
    void shouldReturn400WithMissingPassword() throws Exception {
        // Arrange
        String invalidRequest = "{\"email\":\"test@example.com\"}";

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    @DisplayName("Should refresh token successfully with valid refresh token")
    void shouldRefreshTokenSuccessfully() throws Exception {
        // Arrange - First login to get tokens
        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String refreshToken = objectMapper.readTree(loginResponse)
                .get("data")
                .get("refreshToken")
                .asText();

        RefreshTokenRequest refreshRequest = new RefreshTokenRequest(refreshToken);

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.userId").value(testUser.getId()))
                .andExpect(jsonPath("$.data.email").value("test@example.com"));
    }

    @Test
    @DisplayName("Should return 401 with invalid refresh token")
    void shouldReturn401WithInvalidRefreshToken() throws Exception {
        // Arrange
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest("invalid-token");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("Should logout successfully")
    void shouldLogoutSuccessfully() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Logout successful - please discard your tokens"));
    }

    @Test
    @DisplayName("Should handle disabled user account")
    void shouldHandleDisabledUserAccount() throws Exception {
        // Arrange - Disable user
        testUser.setStatus(UserStatus.INACTIVE);
        userAccountRepository.save(testUser);

        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account is disabled"));
    }

    @Test
    @DisplayName("Should handle suspended user account")
    void shouldHandleSuspendedUserAccount() throws Exception {
        // Arrange - Suspend user
        testUser.setStatus(UserStatus.SUSPENDED);
        userAccountRepository.save(testUser);

        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account is locked"));
    }

    @Test
    @DisplayName("Should handle user with multiple roles")
    void shouldHandleUserWithMultipleRoles() throws Exception {
        // Arrange - Create additional role
        Role managerRole = new Role();
        managerRole.setCode("MANAGER");
        managerRole.setName("Manager");
        managerRole = roleRepository.save(managerRole);

        // Assign manager role
        UserRole userRole = new UserRole();
        UserRole.UserRoleId userRoleId = new UserRole.UserRoleId();
        userRoleId.setUserId(testUser.getId());
        userRoleId.setRoleId(managerRole.getId());
        userRole.setId(userRoleId);
        userRole.setUserAccount(testUser);
        userRole.setRole(managerRole);
        userRoleRepository.save(userRole);

        // CRITICAL: Flush changes to DB and clear persistence context
        // This ensures the authentication service loads fresh user data with both roles
        entityManager.flush();
        entityManager.clear();

        LoginRequest loginRequest = new LoginRequest("test@example.com", "password123");

        // Act & Assert
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roles", hasSize(2)))
                .andExpect(jsonPath("$.data.roles", containsInAnyOrder("ADMIN", "MANAGER")));
    }
}
