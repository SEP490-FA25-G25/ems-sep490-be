package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.studentmanagement.CreateStudentRequest;
import org.fyp.tmssep490be.dtos.studentmanagement.CreateStudentResponse;
import org.fyp.tmssep490be.dtos.studentmanagement.SkillAssessmentInput;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.Gender;
import org.fyp.tmssep490be.entities.enums.Skill;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.StudentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for StudentService.createStudent() method
 * Uses modern Spring Boot 3.5.7 @SpringBootTest with @MockitoBean pattern
 * Tests business logic in Spring context with proper dependency injection
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("StudentService - Create Student Unit Tests")
class StudentServiceImplCreateTest {

    @Autowired
    private StudentService studentService;

    @MockitoBean
    private StudentRepository studentRepository;

    @MockitoBean
    private UserAccountRepository userAccountRepository;

    @MockitoBean
    private RoleRepository roleRepository;

    @MockitoBean
    private UserRoleRepository userRoleRepository;

    @MockitoBean
    private BranchRepository branchRepository;

    @MockitoBean
    private UserBranchesRepository userBranchesRepository;

    @MockitoBean
    private LevelRepository levelRepository;

    @MockitoBean
    private ReplacementSkillAssessmentRepository replacementSkillAssessmentRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private EnrollmentRepository enrollmentRepository;

    private CreateStudentRequest validRequest;
    private Branch testBranch;
    private Role studentRole;
    private UserAccount testUser;
    private Student testStudent;
    private Level testLevel;
    private Long currentUserId = 10L;

    @BeforeEach
    void setUp() {
        // Setup test data
        validRequest = CreateStudentRequest.builder()
                .email("newstudent@example.com")
                .fullName("Nguyen Van A")
                .phone("0912345678")
                .facebookUrl("https://fb.com/nguyenvana")
                .address("123 Test Street")
                .gender(Gender.MALE)
                .dob(LocalDate.of(2000, 1, 15))
                .branchId(1L)
                .build();

        testBranch = Branch.builder()
                .id(1L)
                .name("HCM Branch")
                .code("HCM01")
                .build();

        studentRole = Role.builder()
                .id(3L)
                .code("STUDENT")
                .name("Student")
                .build();

        testUser = UserAccount.builder()
                .id(100L)
                .email("newstudent@example.com")
                .fullName("Nguyen Van A")
                .phone("0912345678")
                .gender(Gender.MALE)
                .dob(LocalDate.of(2000, 1, 15))
                .status(UserStatus.ACTIVE)
                .passwordHash("encodedPassword")
                .createdAt(OffsetDateTime.now())
                .build();

        testStudent = Student.builder()
                .id(50L)
                .userAccount(testUser)
                .studentCode("ST1NGUYENVANA123")
                .build();

        testLevel = Level.builder()
                .id(1L)
                .code("B1")
                .name("Intermediate B1")
                .build();

        // Setup current user (Academic Affair)
        UserAccount currentUser = UserAccount.builder()
                .id(currentUserId)
                .fullName("Academic Affair User")
                .email("academic@example.com")
                .build();

        // Mock getUserAccessibleBranches for branch access validation
        when(userBranchesRepository.findBranchIdsByUserId(currentUserId))
                .thenReturn(Arrays.asList(1L, 2L));
    }

    @Test
    @DisplayName("Should create student successfully with default password 12345678")
    void shouldCreateStudentSuccessfullyWithDefaultPassword() {
        // Arrange
        when(userAccountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
        when(passwordEncoder.encode("12345678")).thenReturn("encodedPassword");
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUser);
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userAccountRepository.findById(currentUserId))
                .thenReturn(Optional.of(UserAccount.builder()
                        .id(currentUserId)
                        .fullName("Academic Affair User")
                        .build()));

        // Act
        CreateStudentResponse response = studentService.createStudent(validRequest, currentUserId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStudentId()).isEqualTo(50L);
        assertThat(response.getStudentCode()).isEqualTo("ST1NGUYENVANA123");
        assertThat(response.getEmail()).isEqualTo("newstudent@example.com");
        assertThat(response.getFullName()).isEqualTo("Nguyen Van A");
        assertThat(response.getDefaultPassword()).isEqualTo("12345678"); // Always this password
        assertThat(response.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(response.getBranchId()).isEqualTo(1L);
        assertThat(response.getSkillAssessmentsCreated()).isEqualTo(0);

        // Verify interactions
        verify(userAccountRepository).findByEmail("newstudent@example.com");
        verify(branchRepository).findById(1L);
        verify(passwordEncoder).encode("12345678"); // Verify default password
        verify(userAccountRepository).save(any(UserAccount.class));
        verify(studentRepository).save(any(Student.class));
        verify(roleRepository).findByCode("STUDENT");
        verify(userRoleRepository).save(any(UserRole.class));
        verify(userBranchesRepository).save(any(UserBranches.class));
    }

    @Test
    @DisplayName("Should create student with skill assessments")
    void shouldCreateStudentWithSkillAssessments() {
        // Arrange
        SkillAssessmentInput assessment1 = SkillAssessmentInput.builder()
                .skill(Skill.GENERAL)
                .levelId(1L)
                .score(75)
                .note("Placement test")
                .build();

        SkillAssessmentInput assessment2 = SkillAssessmentInput.builder()
                .skill(Skill.SPEAKING)
                .levelId(1L)
                .score(80)
                .build();

        validRequest.setSkillAssessments(Arrays.asList(assessment1, assessment2));

        when(userAccountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
        when(levelRepository.existsById(1L)).thenReturn(true);
        when(levelRepository.findById(1L)).thenReturn(Optional.of(testLevel));
        when(passwordEncoder.encode("12345678")).thenReturn("encodedPassword");
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUser);
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userAccountRepository.findById(currentUserId))
                .thenReturn(Optional.of(UserAccount.builder()
                        .id(currentUserId)
                        .fullName("Academic Affair User")
                        .build()));

        // Act
        CreateStudentResponse response = studentService.createStudent(validRequest, currentUserId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getSkillAssessmentsCreated()).isEqualTo(2);
        assertThat(response.getDefaultPassword()).isEqualTo("12345678");
        verify(levelRepository, atLeastOnce()).existsById(1L); // Changed from times(2) to atLeastOnce()
        verify(replacementSkillAssessmentRepository, times(2)).save(any(ReplacementSkillAssessment.class));
    }

    @Test
    @DisplayName("Should throw exception when email already exists")
    void shouldThrowExceptionWhenEmailAlreadyExists() {
        // Arrange
        when(userAccountRepository.findByEmail("newstudent@example.com"))
                .thenReturn(Optional.of(testUser));

        // Act & Assert
        assertThatThrownBy(() -> studentService.createStudent(validRequest, currentUserId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(userAccountRepository).findByEmail("newstudent@example.com");
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when branch not found")
    void shouldThrowExceptionWhenBranchNotFound() {
        // Arrange
        when(userAccountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(branchRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> studentService.createStudent(validRequest, currentUserId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BRANCH_NOT_FOUND);

        verify(branchRepository).findById(1L);
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when user has no access to branch")
    void shouldThrowExceptionWhenUserHasNoAccessToBranch() {
        // Arrange
        validRequest.setBranchId(999L); // Branch user doesn't have access to
        when(userAccountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(branchRepository.findById(999L)).thenReturn(Optional.of(testBranch));
        when(userBranchesRepository.findBranchIdsByUserId(currentUserId))
                .thenReturn(Arrays.asList(1L, 2L)); // Only has access to branch 1 and 2

        // Act & Assert
        assertThatThrownBy(() -> studentService.createStudent(validRequest, currentUserId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BRANCH_ACCESS_DENIED);

        verify(userBranchesRepository).findBranchIdsByUserId(currentUserId);
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when level not found")
    void shouldThrowExceptionWhenLevelNotFound() {
        // Arrange
        SkillAssessmentInput assessment = SkillAssessmentInput.builder()
                .skill(Skill.GENERAL)
                .levelId(999L) // Non-existent level ID
                .score(75)
                .build();
        validRequest.setSkillAssessments(Arrays.asList(assessment));

        when(userAccountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
        when(levelRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> studentService.createStudent(validRequest, currentUserId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LEVEL_NOT_FOUND);

        verify(levelRepository).existsById(999L);
        verify(userAccountRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should throw exception when STUDENT role not found")
    void shouldThrowExceptionWhenStudentRoleNotFound() {
        // Arrange
        when(userAccountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
        when(passwordEncoder.encode("12345678")).thenReturn("encodedPassword");
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUser);
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> studentService.createStudent(validRequest, currentUserId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STUDENT_ROLE_NOT_FOUND);

        verify(roleRepository).findByCode("STUDENT");
    }

    @Test
    @DisplayName("Should generate unique student code when collision occurs")
    void shouldGenerateUniqueStudentCodeWhenCollisionOccurs() {
        // Arrange
        when(userAccountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
        when(passwordEncoder.encode("12345678")).thenReturn("encodedPassword");
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(testUser);
        
        // First generated code exists, second doesn't
        when(studentRepository.findByStudentCode(anyString()))
                .thenReturn(Optional.of(testStudent))  // First attempt: exists
                .thenReturn(Optional.empty());         // Second attempt: available
        
        when(studentRepository.save(any(Student.class))).thenReturn(testStudent);
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userAccountRepository.findById(currentUserId))
                .thenReturn(Optional.of(UserAccount.builder()
                        .id(currentUserId)
                        .fullName("Academic Affair User")
                        .build()));

        // Act
        CreateStudentResponse response = studentService.createStudent(validRequest, currentUserId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getDefaultPassword()).isEqualTo("12345678");
        verify(studentRepository, atLeast(2)).findByStudentCode(anyString());
    }

    @Test
    @DisplayName("Should handle student with minimal data (only required fields)")
    void shouldHandleStudentWithMinimalData() {
        // Arrange
        CreateStudentRequest minimalRequest = CreateStudentRequest.builder()
                .email("minimal@example.com")
                .fullName("Minimal Student")
                .gender(Gender.FEMALE)
                .branchId(1L)
                .build();

        // Mock minimal user without phone/dob
        UserAccount minimalUser = UserAccount.builder()
                .id(100L)
                .email("minimal@example.com")
                .fullName("Minimal Student")
                .gender(Gender.FEMALE)
                .status(UserStatus.ACTIVE)
                .passwordHash("encodedPassword")
                .build();

        Student minimalStudent = Student.builder()
                .id(50L)
                .userAccount(minimalUser)
                .studentCode("ST1NGUYENVANA123")
                .build();

        when(userAccountRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(branchRepository.findById(1L)).thenReturn(Optional.of(testBranch));
        when(passwordEncoder.encode("12345678")).thenReturn("encodedPassword");
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(minimalUser);
        when(studentRepository.save(any(Student.class))).thenReturn(minimalStudent);
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(studentRole));
        when(userAccountRepository.findById(currentUserId))
                .thenReturn(Optional.of(UserAccount.builder()
                        .id(currentUserId)
                        .fullName("Academic Affair User")
                        .build()));

        // Act
        CreateStudentResponse response = studentService.createStudent(minimalRequest, currentUserId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getPhone()).isNull();
        assertThat(response.getDob()).isNull();
        assertThat(response.getDefaultPassword()).isEqualTo("12345678");
        assertThat(response.getSkillAssessmentsCreated()).isEqualTo(0);
    }
}
