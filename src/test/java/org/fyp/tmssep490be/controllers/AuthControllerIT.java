package org.fyp.tmssep490be.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.fyp.tmssep490be.dtos.auth.AuthResponse;
import org.fyp.tmssep490be.dtos.auth.LoginRequest;
import org.fyp.tmssep490be.dtos.auth.RefreshTokenRequest;
import org.fyp.tmssep490be.entities.Role;
import org.fyp.tmssep490be.entities.UserAccount;
import org.fyp.tmssep490be.entities.UserRole;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.repositories.RoleRepository;
import org.fyp.tmssep490be.repositories.UserAccountRepository;
import org.fyp.tmssep490be.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController.
 * Tests authentication endpoints with real Spring Security context.
 * Uses modern Spring Boot 3.5.7 testing patterns with @SpringBootTest.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("AuthController Integration Tests")
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private UserAccount testUser;
    private LoginRequest loginRequest;
    private RefreshTokenRequest refreshTokenRequest;

    @BeforeEach
    void setUp() {
        // Create test user withTestDataBuilder
        testUser = TestDataBuilder.buildUserAccount()
                .email("test@example.com")
                .fullName("Test User")
                .status(UserStatus.ACTIVE)
                .build();

        // Encode password
        testUser.setPasswordHash(passwordEncoder.encode("password123"));

        // Add role - save role first to get ID
        Role role = new Role();
        role.setCode("ADMIN");
        role.setName("Administrator");
        role = roleRepository.save(role);

        // Create UserRole with proper composite key
        UserRole.UserRoleId userRoleId = new UserRole.UserRoleId();
        userRoleId.setUserId(testUser.getId());
        userRoleId.setRoleId(role.getId());

        UserRole userRole = new UserRole();
        userRole.setId(userRoleId);
        userRole.setUserAccount(testUser);
        userRole.setRole(role);

        testUser.setUserRoles(new HashSet<>());
        testUser.getUserRoles().add(userRole);

        testUser = userAccountRepository.save(testUser);

        // Create test requests
        loginRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        refreshTokenRequest = RefreshTokenRequest.builder()
                .refreshToken("dummy-refresh-token")
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should login successfully with valid credentials")
    void shouldLoginSuccessfully() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data").exists())
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").isNumber());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should reject invalid credentials")
    void shouldRejectInvalidCredentials() throws Exception {
        // Arrange
        LoginRequest invalidRequest = LoginRequest.builder()
                .email("test@example.com")
                .password("wrongpassword")
                .build();

        // Act
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)));

        // Assert
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should reject non-existent user")
    void shouldRejectNonExistentUser() throws Exception {
        // Arrange
        LoginRequest nonExistentRequest = LoginRequest.builder()
                .email("nonexistent@example.com")
                .password("password123")
                .build();

        // Act
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(nonExistentRequest)));

        // Assert
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should validate request body")
    void shouldValidateRequestBody() throws Exception {
        // Arrange - Empty request
        LoginRequest emptyRequest = LoginRequest.builder().build();

        // Act
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)));

        // Assert
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Should refresh token successfully")
    void shouldRefreshTokenSuccessfully() throws Exception {
        // First, login to get a valid refresh token
        ResultActions loginResult = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)));

        String responseContent = loginResult.andReturn().getResponse().getContentAsString();
        // Parse the ResponseObject wrapper to get the actual AuthResponse
        com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(responseContent);
        String refreshTokenValue = jsonNode.get("data").get("refreshToken").asText();

        refreshTokenRequest = RefreshTokenRequest.builder()
                .refreshToken(refreshTokenValue)
                .build();

        // Act - Refresh token
        ResultActions result = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest)));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresIn").isNumber());
    }

    @Test
    @DisplayName("POST /api/v1/auth/refresh - Should reject invalid refresh token")
    void shouldRejectInvalidRefreshToken() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(post("/api/v1/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(refreshTokenRequest)));

        // Assert
        result.andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/logout - Should logout successfully")
    void shouldLogoutSuccessfully() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(post("/api/v1/auth/logout")
                .contentType(MediaType.APPLICATION_JSON));

        // Assert
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should reject inactive user")
    void shouldRejectInactiveUser() throws Exception {
        // Arrange - Create inactive user
        UserAccount inactiveUser = TestDataBuilder.buildUserAccount()
                .email("inactive@example.com")
                .fullName("Inactive User")
                .status(UserStatus.INACTIVE)
                .build();
        inactiveUser.setPasswordHash(passwordEncoder.encode("password123"));
        inactiveUser = userAccountRepository.save(inactiveUser);

        LoginRequest inactiveRequest = LoginRequest.builder()
                .email("inactive@example.com")
                .password("password123")
                .build();

        // Act
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(inactiveRequest)));

        // Assert - Spring Security returns 403 Forbidden for disabled accounts
        result.andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/auth/login - Should handle malformed JSON")
    void shouldHandleMalformedJson() throws Exception {
        // Act
        ResultActions result = mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"));

        // Assert
        result.andExpect(status().isBadRequest());
    }
}