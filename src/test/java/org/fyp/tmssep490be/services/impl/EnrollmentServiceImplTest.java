package org.fyp.tmssep490be.services.impl;

import jakarta.persistence.EntityNotFoundException;
import org.fyp.tmssep490be.dtos.enrollment.*;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.repositories.*;
import org.fyp.tmssep490be.services.ExcelParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EnrollmentService Unit Tests")
class EnrollmentServiceImplTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private ClassRepository classRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private StudentSessionRepository studentSessionRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private UserRoleRepository userRoleRepository;
    @Mock
    private UserBranchesRepository userBranchesRepository;
    @Mock
    private ExcelParserService excelParserService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EnrollmentServiceImpl enrollmentService;

    private ClassEntity testClass;
    private MultipartFile mockFile;

    @BeforeEach
    void setUp() {
        Branch testBranch = new Branch();
        testBranch.setId(1L);

        testClass = new ClassEntity();
        testClass.setId(1L);
        testClass.setCode("ENG-A1-001");
        testClass.setName("Basic English A1");
        testClass.setMaxCapacity(20);
        testClass.setBranch(testBranch);
        testClass.setStartDate(LocalDate.now().plusDays(7));
        testClass.setApprovalStatus(ApprovalStatus.APPROVED);
        testClass.setStatus(ClassStatus.SCHEDULED);

        mockFile = mock(MultipartFile.class);
    }

    // ==================== Preview Tests ====================

    @Test
    @DisplayName("Should preview import successfully with sufficient capacity")
    void shouldPreviewImportSuccessfully() {
        // Arrange
        List<StudentEnrollmentData> parsedStudents = createParsedStudentsData(5);

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(excelParserService.parseStudentEnrollment(mockFile)).thenReturn(parsedStudents);
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(10);

        // Mock student resolution
        when(studentRepository.findByStudentCode("ST001")).thenReturn(Optional.of(createStudent(1L)));
        when(studentRepository.findByStudentCode("ST002")).thenReturn(Optional.empty());
        when(userAccountRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act
        ClassEnrollmentImportPreview preview = enrollmentService.previewClassEnrollmentImport(
                1L, mockFile, 100L
        );

        // Assert
        assertThat(preview).isNotNull();
        assertThat(preview.getClassId()).isEqualTo(1L);
        assertThat(preview.getClassCode()).isEqualTo("ENG-A1-001");
        assertThat(preview.getCurrentEnrolled()).isEqualTo(10);
        assertThat(preview.getMaxCapacity()).isEqualTo(20);
        assertThat(preview.getAvailableSlots()).isEqualTo(10);
        assertThat(preview.isExceedsCapacity()).isFalse();
        assertThat(preview.getRecommendation().getType()).isEqualTo(RecommendationType.OK);

        verify(excelParserService).parseStudentEnrollment(mockFile);
        verify(enrollmentRepository).countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED);
    }

    @Test
    @DisplayName("Should detect capacity exceeded in preview")
    void shouldDetectCapacityExceededInPreview() {
        // Arrange
        List<StudentEnrollmentData> parsedStudents = createParsedStudentsData(15);

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(excelParserService.parseStudentEnrollment(mockFile)).thenReturn(parsedStudents);
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(15);
        when(userAccountRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act
        ClassEnrollmentImportPreview preview = enrollmentService.previewClassEnrollmentImport(
                1L, mockFile, 100L
        );

        // Assert
        assertThat(preview.isExceedsCapacity()).isTrue();
        assertThat(preview.getExceededBy()).isEqualTo(10);
        assertThat(preview.getRecommendation().getType()).isIn(
                RecommendationType.OVERRIDE_AVAILABLE,
                RecommendationType.PARTIAL_SUGGESTED
        );
        assertThat(preview.getWarnings()).isNotEmpty();
    }

    @Test
    @DisplayName("Should throw exception when class not found")
    void shouldThrowExceptionWhenClassNotFound() {
        // Arrange
        when(classRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.previewClassEnrollmentImport(999L, mockFile, 100L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Class not found");
    }

    @Test
    @DisplayName("Should throw exception when class not approved")
    void shouldThrowExceptionWhenClassNotApproved() {
        // Arrange
        testClass.setApprovalStatus(ApprovalStatus.PENDING);
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.previewClassEnrollmentImport(1L, mockFile, 100L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("must be approved");
    }

    @Test
    @DisplayName("Should detect duplicate emails in Excel file")
    void shouldDetectDuplicateEmailsInExcelFile() {
        // Arrange
        List<StudentEnrollmentData> parsedStudents = Arrays.asList(
                createStudentData("ST001", "student@email.com", null),
                createStudentData("ST002", "student@email.com", null) // Duplicate email
        );

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(excelParserService.parseStudentEnrollment(mockFile)).thenReturn(parsedStudents);
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(5);

        // Act
        ClassEnrollmentImportPreview preview = enrollmentService.previewClassEnrollmentImport(
                1L, mockFile, 100L
        );

        // Assert
        assertThat(preview.getErrorCount()).isGreaterThan(0);
        long duplicates = preview.getStudents().stream()
                .filter(s -> s.getStatus() == StudentResolutionStatus.DUPLICATE)
                .count();
        assertThat(duplicates).isEqualTo(1);
    }

    // ==================== Execute Tests ====================

    @Test
    @DisplayName("Should execute enrollment with ALL strategy successfully")
    void shouldExecuteEnrollmentWithAllStrategy() {
        // Arrange
        ClassEnrollmentImportExecuteRequest request = createExecuteRequest(EnrollmentStrategy.ALL, 5);
        List<Session> futureSessions = createFutureSessions(10);

        when(classRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClass));
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(10);
        when(sessionRepository.findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                eq(1L), any(LocalDate.class), eq(SessionStatus.PLANNED)
        )).thenReturn(futureSessions);
        when(enrollmentRepository.existsByClassIdAndStudentIdAndStatus(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(enrollmentRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(studentSessionRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Mock student resolution
        for (StudentEnrollmentData data : request.getStudents()) {
            data.setStatus(StudentResolutionStatus.FOUND);
            data.setResolvedStudentId((long) data.hashCode());
        }

        // Act
        EnrollmentResult result = enrollmentService.executeClassEnrollmentImport(request, 100L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEnrolledCount()).isEqualTo(5);
        assertThat(result.getSessionsGeneratedPerStudent()).isEqualTo(10);
        assertThat(result.getTotalStudentSessionsCreated()).isEqualTo(50);

        verify(classRepository).findByIdWithLock(1L);
        verify(enrollmentRepository).saveAll(anyList());
        verify(studentSessionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw exception when capacity exceeded with ALL strategy")
    void shouldThrowExceptionWhenCapacityExceededWithAllStrategy() {
        // Arrange
        ClassEnrollmentImportExecuteRequest request = createExecuteRequest(EnrollmentStrategy.ALL, 15);

        when(classRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClass));
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(15);

        // Mock all students as FOUND
        for (StudentEnrollmentData data : request.getStudents()) {
            data.setStatus(StudentResolutionStatus.FOUND);
            data.setResolvedStudentId((long) data.hashCode());
        }

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.executeClassEnrollmentImport(request, 100L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("capacity exceeded");
    }

    @Test
    @DisplayName("Should execute enrollment with OVERRIDE strategy and log override")
    void shouldExecuteEnrollmentWithOverrideStrategy() {
        // Arrange
        ClassEnrollmentImportExecuteRequest request = createExecuteRequest(EnrollmentStrategy.OVERRIDE, 15);
        request.setOverrideReason("High demand from students, additional teacher support available");

        List<Session> futureSessions = createFutureSessions(10);

        when(classRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClass));
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(15);
        when(sessionRepository.findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                eq(1L), any(LocalDate.class), eq(SessionStatus.PLANNED)
        )).thenReturn(futureSessions);
        when(enrollmentRepository.existsByClassIdAndStudentIdAndStatus(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(enrollmentRepository.saveAll(anyList())).thenReturn(Collections.emptyList());
        when(studentSessionRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Mock all students as FOUND
        for (StudentEnrollmentData data : request.getStudents()) {
            data.setStatus(StudentResolutionStatus.FOUND);
            data.setResolvedStudentId((long) data.hashCode());
        }

        // Act
        EnrollmentResult result = enrollmentService.executeClassEnrollmentImport(request, 100L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEnrolledCount()).isEqualTo(15);

        // Verify capacity override was logged (in application logs, not in database)
        verify(enrollmentRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw exception when override reason too short")
    void shouldThrowExceptionWhenOverrideReasonTooShort() {
        // Arrange
        ClassEnrollmentImportExecuteRequest request = createExecuteRequest(EnrollmentStrategy.OVERRIDE, 15);
        request.setOverrideReason("Short reason"); // Less than 20 characters

        when(classRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClass));
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(15);

        // Mock all students as FOUND
        for (StudentEnrollmentData data : request.getStudents()) {
            data.setStatus(StudentResolutionStatus.FOUND);
            data.setResolvedStudentId((long) data.hashCode());
        }

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.executeClassEnrollmentImport(request, 100L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("Override reason required");
    }

    @Test
    @DisplayName("Should create new students when status is CREATE")
    void shouldCreateNewStudentsWhenStatusIsCreate() {
        // Arrange
        ClassEnrollmentImportExecuteRequest request = createExecuteRequest(EnrollmentStrategy.ALL, 2);
        List<Session> futureSessions = createFutureSessions(5);

        // Mark students as CREATE
        for (StudentEnrollmentData data : request.getStudents()) {
            data.setStatus(StudentResolutionStatus.CREATE);
        }

        when(classRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClass));
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(5);
        when(sessionRepository.findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                eq(1L), any(LocalDate.class), eq(SessionStatus.PLANNED)
        )).thenReturn(futureSessions);
        when(enrollmentRepository.existsByClassIdAndStudentIdAndStatus(anyLong(), anyLong(), any()))
                .thenReturn(false);

        // Mock student creation
        when(userAccountRepository.save(any(UserAccount.class))).thenReturn(createUserAccount(1L));
        when(studentRepository.save(any(Student.class))).thenReturn(createStudent(1L));
        when(roleRepository.findByCode("STUDENT")).thenReturn(Optional.of(createStudentRole()));
        when(passwordEncoder.encode(anyString())).thenReturn("encoded_password");

        // Act
        EnrollmentResult result = enrollmentService.executeClassEnrollmentImport(request, 100L);

        // Assert
        assertThat(result.getStudentsCreated()).isEqualTo(2);
        verify(userAccountRepository, times(2)).save(any(UserAccount.class));
        verify(studentRepository, times(2)).save(any(Student.class));
        verify(userRoleRepository, times(2)).save(any(UserRole.class));
        verify(userBranchesRepository, times(2)).save(any(UserBranches.class));
    }

    @Test
    @DisplayName("Should throw exception when student already enrolled")
    void shouldThrowExceptionWhenStudentAlreadyEnrolled() {
        // Arrange
        ClassEnrollmentImportExecuteRequest request = createExecuteRequest(EnrollmentStrategy.ALL, 1);
        List<Session> futureSessions = createFutureSessions(5);

        for (StudentEnrollmentData data : request.getStudents()) {
            data.setStatus(StudentResolutionStatus.FOUND);
            data.setResolvedStudentId(1L);
        }

        when(classRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClass));
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(5);
        when(sessionRepository.findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                eq(1L), any(LocalDate.class), eq(SessionStatus.PLANNED)
        )).thenReturn(futureSessions);
        when(enrollmentRepository.existsByClassIdAndStudentIdAndStatus(1L, 1L, EnrollmentStatus.ENROLLED))
                .thenReturn(true); // Already enrolled

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.executeClassEnrollmentImport(request, 100L))
                .isInstanceOf(CustomException.class)
                .hasMessageContaining("already enrolled");
    }

    @Test
    @DisplayName("Should handle mid-course enrollment correctly")
    void shouldHandleMidCourseEnrollmentCorrectly() {
        // Arrange
        testClass.setStartDate(LocalDate.now().minusDays(10)); // Class already started
        ClassEnrollmentImportExecuteRequest request = createExecuteRequest(EnrollmentStrategy.ALL, 1);
        List<Session> futureSessions = createFutureSessions(5);

        for (StudentEnrollmentData data : request.getStudents()) {
            data.setStatus(StudentResolutionStatus.FOUND);
            data.setResolvedStudentId(1L);
        }

        when(classRepository.findByIdWithLock(1L)).thenReturn(Optional.of(testClass));
        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(5);
        when(sessionRepository.findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                eq(1L), any(LocalDate.class), eq(SessionStatus.PLANNED)
        )).thenReturn(futureSessions);
        when(enrollmentRepository.existsByClassIdAndStudentIdAndStatus(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(enrollmentRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Enrollment> enrollments = invocation.getArgument(0);
            // Verify join_session_id is set for mid-course
            assertThat(enrollments.get(0).getJoinSessionId()).isNotNull();
            return enrollments;
        });
        when(studentSessionRepository.saveAll(anyList())).thenReturn(Collections.emptyList());

        // Act
        EnrollmentResult result = enrollmentService.executeClassEnrollmentImport(request, 100L);

        // Assert
        assertThat(result.getWarnings()).hasSize(1);
        assertThat(result.getWarnings().get(0)).contains("Mid-course enrollment");
    }

    // ==================== Helper Methods ====================

    private List<StudentEnrollmentData> createParsedStudentsData(int count) {
        List<StudentEnrollmentData> students = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            students.add(createStudentData("ST00" + (i + 1), "student" + i + "@email.com", null));
        }
        return students;
    }

    private StudentEnrollmentData createStudentData(String code, String email, StudentResolutionStatus status) {
        return StudentEnrollmentData.builder()
                .studentCode(code)
                .fullName("Student " + code)
                .email(email)
                .phone("090123456" + code.substring(code.length() - 1))
                .gender(Gender.MALE)
                .dob(LocalDate.of(1995, 1, 1))
                // Skill assessments format: "Level-Score"
                .general("A1-70")
                .reading("A1-68")
                .writing("A1-65")
                .speaking("A1-72")
                .listening("A1-70")
                .status(status)
                .build();
    }

    private ClassEnrollmentImportExecuteRequest createExecuteRequest(EnrollmentStrategy strategy, int studentCount) {
        List<StudentEnrollmentData> students = createParsedStudentsData(studentCount);
        return ClassEnrollmentImportExecuteRequest.builder()
                .classId(1L)
                .strategy(strategy)
                .students(students)
                .build();
    }

    private List<Session> createFutureSessions(int count) {
        List<Session> sessions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Session session = new Session();
            session.setId((long) (i + 1));
            session.setClassEntity(testClass);
            session.setDate(LocalDate.now().plusDays(i + 1));
            session.setStatus(SessionStatus.PLANNED);
            sessions.add(session);
        }
        return sessions;
    }

    private Student createStudent(Long id) {
        UserAccount userAccount = new UserAccount();
        userAccount.setId(id);

        Student student = new Student();
        student.setId(id);
        student.setUserAccount(userAccount);
        student.setStudentCode("ST" + id);
        return student;
    }

    private UserAccount createUserAccount(Long id) {
        UserAccount user = new UserAccount();
        user.setId(id);
        user.setEmail("student" + id + "@email.com");
        user.setFullName("Student " + id);
        return user;
    }

    private Role createStudentRole() {
        Role role = new Role();
        role.setId(3L);
        role.setCode("STUDENT");
        role.setName("Student");
        return role;
    }

    // ==================== Enroll Existing Students Tests ====================

    @Test
    @DisplayName("Should enroll existing students successfully")
    void shouldEnrollExistingStudentsSuccessfully() {
        // Arrange
        List<Long> studentIds = Arrays.asList(1L, 2L, 3L);
        List<Student> students = Arrays.asList(
                createStudent(1L),
                createStudent(2L),
                createStudent(3L)
        );
        List<Session> futureSessions = createFutureSessions(5);

        EnrollExistingStudentsRequest request = EnrollExistingStudentsRequest.builder()
                .classId(1L)
                .studentIds(studentIds)
                .overrideCapacity(false)
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findAllById(studentIds)).thenReturn(students);
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(10);
        when(sessionRepository.findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                eq(1L), any(LocalDate.class), eq(SessionStatus.PLANNED)
        )).thenReturn(futureSessions);
        when(enrollmentRepository.existsByClassIdAndStudentIdAndStatus(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(enrollmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentSessionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        EnrollmentResult result = enrollmentService.enrollExistingStudents(request, 100L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEnrolledCount()).isEqualTo(3);
        assertThat(result.getSessionsGeneratedPerStudent()).isEqualTo(5);
        assertThat(result.getTotalStudentSessionsCreated()).isEqualTo(15); // 3 students × 5 sessions

        verify(classRepository, atLeastOnce()).findById(1L); // Called in validateClassForEnrollment and enrollStudents
        verify(studentRepository).findAllById(studentIds);
        verify(enrollmentRepository).countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED);
        verify(enrollmentRepository).saveAll(anyList());
        verify(studentSessionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw exception when enrolling with insufficient capacity")
    void shouldThrowExceptionWhenInsufficientCapacity() {
        // Arrange
        List<Long> studentIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L); // 6 students
        List<Student> students = Arrays.asList(
                createStudent(1L), createStudent(2L), createStudent(3L),
                createStudent(4L), createStudent(5L), createStudent(6L)
        );

        EnrollExistingStudentsRequest request = EnrollExistingStudentsRequest.builder()
                .classId(1L)
                .studentIds(studentIds)
                .overrideCapacity(false) // No override
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findAllById(studentIds)).thenReturn(students);
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(17); // 17 enrolled + 6 requested = 23 > 20 capacity

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.enrollExistingStudents(request, 100L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", org.fyp.tmssep490be.exceptions.ErrorCode.CLASS_CAPACITY_EXCEEDED);

        verify(classRepository).findById(1L);
        verify(studentRepository).findAllById(studentIds);
        verify(enrollmentRepository).countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED);
        verify(enrollmentRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should enroll with capacity override when reason provided")
    void shouldEnrollWithCapacityOverride() {
        // Arrange
        List<Long> studentIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L);
        List<Student> students = Arrays.asList(
                createStudent(1L), createStudent(2L), createStudent(3L),
                createStudent(4L), createStudent(5L), createStudent(6L)
        );
        List<Session> futureSessions = createFutureSessions(5);

        EnrollExistingStudentsRequest request = EnrollExistingStudentsRequest.builder()
                .classId(1L)
                .studentIds(studentIds)
                .overrideCapacity(true)
                .overrideReason("High demand for this class. We have additional resources available.")
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findAllById(studentIds)).thenReturn(students);
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(17); // 17 + 6 = 23 > 20
        when(sessionRepository.findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                eq(1L), any(LocalDate.class), eq(SessionStatus.PLANNED)
        )).thenReturn(futureSessions);
        when(enrollmentRepository.existsByClassIdAndStudentIdAndStatus(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(enrollmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentSessionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        EnrollmentResult result = enrollmentService.enrollExistingStudents(request, 100L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getEnrolledCount()).isEqualTo(6);
        assertThat(result.getTotalStudentSessionsCreated()).isEqualTo(30); // 6 × 5

        verify(enrollmentRepository).saveAll(anyList());
        verify(studentSessionRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw exception when override reason is missing")
    void shouldThrowExceptionWhenOverrideReasonMissing() {
        // Arrange
        List<Long> studentIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L);
        List<Student> students = Arrays.asList(
                createStudent(1L), createStudent(2L), createStudent(3L),
                createStudent(4L), createStudent(5L), createStudent(6L)
        );

        EnrollExistingStudentsRequest request = EnrollExistingStudentsRequest.builder()
                .classId(1L)
                .studentIds(studentIds)
                .overrideCapacity(true)
                .overrideReason(null) // Missing reason
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findAllById(studentIds)).thenReturn(students);
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(17);

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.enrollExistingStudents(request, 100L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", org.fyp.tmssep490be.exceptions.ErrorCode.OVERRIDE_REASON_REQUIRED);

        verify(enrollmentRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw exception when override reason is too short for existing students")
    void shouldThrowExceptionWhenOverrideReasonTooShortForExistingStudents() {
        // Arrange
        List<Long> studentIds = Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L);
        List<Student> students = Arrays.asList(
                createStudent(1L), createStudent(2L), createStudent(3L),
                createStudent(4L), createStudent(5L), createStudent(6L)
        );

        EnrollExistingStudentsRequest request = EnrollExistingStudentsRequest.builder()
                .classId(1L)
                .studentIds(studentIds)
                .overrideCapacity(true)
                .overrideReason("Too short") // Less than 20 characters
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findAllById(studentIds)).thenReturn(students);
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED))
                .thenReturn(17);

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.enrollExistingStudents(request, 100L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", org.fyp.tmssep490be.exceptions.ErrorCode.OVERRIDE_REASON_TOO_SHORT);

        verify(enrollmentRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should remove duplicate student IDs from request")
    void shouldRemoveDuplicateStudentIds() {
        // Arrange
        List<Long> studentIdsWithDuplicates = Arrays.asList(1L, 2L, 3L, 2L, 1L); // Duplicates: 1, 2
        List<Student> students = Arrays.asList(
                createStudent(1L),
                createStudent(2L),
                createStudent(3L)
        );
        List<Session> futureSessions = createFutureSessions(5);

        EnrollExistingStudentsRequest request = EnrollExistingStudentsRequest.builder()
                .classId(1L)
                .studentIds(studentIdsWithDuplicates)
                .overrideCapacity(false)
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findAllById(anyList())).thenReturn(students);
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(10);
        when(sessionRepository.findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                eq(1L), any(LocalDate.class), eq(SessionStatus.PLANNED)
        )).thenReturn(futureSessions);
        when(enrollmentRepository.existsByClassIdAndStudentIdAndStatus(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(enrollmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentSessionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        EnrollmentResult result = enrollmentService.enrollExistingStudents(request, 100L);

        // Assert
        assertThat(result.getEnrolledCount()).isEqualTo(3); // Only 3 unique students
        verify(studentRepository).findAllById(argThat(ids -> ((java.util.Collection<?>) ids).size() == 3)); // Should query only 3
    }

    @Test
    @DisplayName("Should throw exception when some students not found")
    void shouldThrowExceptionWhenStudentsNotFound() {
        // Arrange
        List<Long> studentIds = Arrays.asList(1L, 2L, 999L); // 999 doesn't exist
        List<Student> students = Arrays.asList(
                createStudent(1L),
                createStudent(2L)
                // Student 999 not found
        );

        EnrollExistingStudentsRequest request = EnrollExistingStudentsRequest.builder()
                .classId(1L)
                .studentIds(studentIds)
                .overrideCapacity(false)
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findAllById(studentIds)).thenReturn(students);

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.enrollExistingStudents(request, 100L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Some students not found");

        verify(enrollmentRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw exception when class not approved for existing students enrollment")
    void shouldThrowExceptionWhenClassNotApprovedForExistingStudents() {
        // Arrange
        testClass.setApprovalStatus(ApprovalStatus.PENDING);

        List<Long> studentIds = Arrays.asList(1L, 2L);

        EnrollExistingStudentsRequest request = EnrollExistingStudentsRequest.builder()
                .classId(1L)
                .studentIds(studentIds)
                .overrideCapacity(false)
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.enrollExistingStudents(request, 100L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", org.fyp.tmssep490be.exceptions.ErrorCode.CLASS_NOT_APPROVED);

        verify(studentRepository, never()).findAllById(anyList());
    }

    @Test
    @DisplayName("Should throw exception when class not in scheduled status for existing students")
    void shouldThrowExceptionWhenClassNotScheduledForExistingStudents() {
        // Arrange
        testClass.setStatus(ClassStatus.DRAFT);

        List<Long> studentIds = Arrays.asList(1L, 2L);

        EnrollExistingStudentsRequest request = EnrollExistingStudentsRequest.builder()
                .classId(1L)
                .studentIds(studentIds)
                .overrideCapacity(false)
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.enrollExistingStudents(request, 100L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", org.fyp.tmssep490be.exceptions.ErrorCode.CLASS_INVALID_STATUS);

        verify(studentRepository, never()).findAllById(anyList());
    }

    @Test
    @DisplayName("Should throw exception when existing student already enrolled in class")
    void shouldThrowExceptionWhenExistingStudentAlreadyEnrolled() {
        // Arrange
        List<Long> studentIds = Arrays.asList(1L, 2L);
        List<Student> students = Arrays.asList(
                createStudent(1L),
                createStudent(2L)
        );
        List<Session> futureSessions = createFutureSessions(5);

        EnrollExistingStudentsRequest request = EnrollExistingStudentsRequest.builder()
                .classId(1L)
                .studentIds(studentIds)
                .overrideCapacity(false)
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findAllById(studentIds)).thenReturn(students);
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(10);
        when(sessionRepository.findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                eq(1L), any(LocalDate.class), eq(SessionStatus.PLANNED)
        )).thenReturn(futureSessions);
        when(enrollmentRepository.existsByClassIdAndStudentIdAndStatus(eq(1L), eq(1L), any()))
                .thenReturn(true); // Student 1 already enrolled

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.enrollExistingStudents(request, 100L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", org.fyp.tmssep490be.exceptions.ErrorCode.ENROLLMENT_ALREADY_EXISTS);

        verify(enrollmentRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should throw exception when no future sessions available")
    void shouldThrowExceptionWhenNoFutureSessions() {
        // Arrange
        List<Long> studentIds = Arrays.asList(1L, 2L);
        List<Student> students = Arrays.asList(
                createStudent(1L),
                createStudent(2L)
        );

        EnrollExistingStudentsRequest request = EnrollExistingStudentsRequest.builder()
                .classId(1L)
                .studentIds(studentIds)
                .overrideCapacity(false)
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findAllById(studentIds)).thenReturn(students);
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(10);
        when(sessionRepository.findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                eq(1L), any(LocalDate.class), eq(SessionStatus.PLANNED)
        )).thenReturn(Collections.emptyList()); // No future sessions

        // Act & Assert
        assertThatThrownBy(() -> enrollmentService.enrollExistingStudents(request, 100L))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", org.fyp.tmssep490be.exceptions.ErrorCode.NO_FUTURE_SESSIONS);

        verify(enrollmentRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("Should handle mid-course enrollment correctly")
    void shouldHandleMidCourseEnrollment() {
        // Arrange
        testClass.setStartDate(LocalDate.now().minusDays(10)); // Class already started

        List<Long> studentIds = Arrays.asList(1L, 2L);
        List<Student> students = Arrays.asList(
                createStudent(1L),
                createStudent(2L)
        );
        List<Session> futureSessions = createFutureSessions(5);

        EnrollExistingStudentsRequest request = EnrollExistingStudentsRequest.builder()
                .classId(1L)
                .studentIds(studentIds)
                .overrideCapacity(false)
                .build();

        when(classRepository.findById(1L)).thenReturn(Optional.of(testClass));
        when(studentRepository.findAllById(studentIds)).thenReturn(students);
        when(enrollmentRepository.countByClassIdAndStatus(1L, EnrollmentStatus.ENROLLED)).thenReturn(10);
        when(sessionRepository.findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
                eq(1L), any(LocalDate.class), eq(SessionStatus.PLANNED)
        )).thenReturn(futureSessions);
        when(enrollmentRepository.existsByClassIdAndStudentIdAndStatus(anyLong(), anyLong(), any()))
                .thenReturn(false);
        when(enrollmentRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentSessionRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        EnrollmentResult result = enrollmentService.enrollExistingStudents(request, 100L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getWarnings()).contains("Mid-course enrollment: Students will only be enrolled in future sessions");

        verify(enrollmentRepository).saveAll(anyList());
        verify(studentSessionRepository).saveAll(anyList());
    }
}
