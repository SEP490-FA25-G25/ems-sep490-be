package org.fyp.tmssep490be.services.impl;

import org.fyp.tmssep490be.dtos.classmanagement.AvailableStudentDTO;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;
import org.fyp.tmssep490be.entities.enums.Skill;
import org.fyp.tmssep490be.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Test for enhanced available students functionality
 * Verifies that complete replacement assessment data is returned
 */
@ExtendWith(MockitoExtension.class)
class ClassServiceImplEnhancedTest {

    @Mock
    private ClassRepository classRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private StudentRepository studentRepository;

    @Mock
    private ReplacementSkillAssessmentRepository skillAssessmentRepository;

    @Mock
    private UserBranchesRepository userBranchesRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private TeachingSlotRepository teachingSlotRepository;

    @InjectMocks
    private ClassServiceImpl classService;

    private ClassEntity testClass;
    private Student testStudent;
    private List<ReplacementSkillAssessment> testAssessments;

    @BeforeEach
    void setUp() {
        // Setup test class
        Subject subject = Subject.builder()
                .id(1L)
                .name("English")
                .build();

        Level level = Level.builder()
                .id(10L)
                .code("B1")
                .name("Intermediate")
                .subject(subject)
                .expectedDurationHours(60)
                .description("Intermediate English")
                .build();

        Course course = Course.builder()
                .id(100L)
                .code("ENG-B1-001")
                .name("Intermediate English Course")
                .level(level)
                .totalHours(60)
                .build();

        Branch branch = Branch.builder()
                .id(1L)
                .name("Main Branch")
                .build();

        testClass = ClassEntity.builder()
                .id(1000L)
                .course(course)
                .branch(branch)
                .maxCapacity(20)
                .status(ClassStatus.SCHEDULED)
                .approvalStatus(ApprovalStatus.APPROVED)
                .build();

        // Setup test student
        UserAccount userAccount = UserAccount.builder()
                .id(5000L)
                .fullName("John Doe")
                .email("john.doe@test.com")
                .phone("+1234567890")
                .build();

        UserBranches userBranch = UserBranches.builder()
                .userAccount(userAccount)
                .branch(branch)
                .build();
        userAccount.getUserBranches().add(userBranch);

        testStudent = Student.builder()
                .id(2000L)
                .studentCode("STU001")
                .userAccount(userAccount)
                .build();

        // Setup test assessments
        Level assessedLevel = Level.builder()
                .id(10L)
                .code("B1")
                .name("Intermediate")
                .subject(subject)
                .expectedDurationHours(60)
                .build();

        UserAccount assessor = UserAccount.builder()
                .id(6000L)
                .fullName("Jane Smith")
                .build();

        testAssessments = List.of(
                ReplacementSkillAssessment.builder()
                        .id(100L)
                        .student(testStudent)
                        .skill(Skill.READING)
                        .level(assessedLevel)
                        .rawScore(BigDecimal.valueOf(85))
                        .scaledScore(BigDecimal.valueOf(85))
                        .scoreScale("0-100")
                        .assessmentCategory("PLACEMENT")
                        .assessmentDate(LocalDate.of(2024, 10, 15))
                        .assessmentType("Placement Test")
                        .note("Good comprehension")
                        .assessedBy(assessor)
                        .build(),
                ReplacementSkillAssessment.builder()
                        .id(101L)
                        .student(testStudent)
                        .skill(Skill.WRITING)
                        .level(assessedLevel)
                        .rawScore(BigDecimal.valueOf(72))
                        .scaledScore(BigDecimal.valueOf(7.2))
                        .scoreScale("0-9")
                        .assessmentCategory("PLACEMENT")
                        .assessmentDate(LocalDate.of(2024, 10, 15))
                        .assessmentType("Placement Test")
                        .note("Needs improvement")
                        .assessedBy(assessor)
                        .build()
        );
    }

    @Test
    void getAvailableStudentsForClass_ShouldReturnCompleteAssessmentData() {
        // Given
        Long classId = 1000L;
        Long userId = 3000L;
        Pageable pageable = PageRequest.of(0, 20);

        when(classRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(userBranchesRepository.findBranchIdsByUserId(userId)).thenReturn(List.of(1L));
        when(studentRepository.findAvailableStudentsForClass(eq(classId), eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testStudent)));
        when(skillAssessmentRepository.findByStudentIdIn(List.of(2000L))).thenReturn(testAssessments);
        when(enrollmentRepository.countByStudentIdAndStatus(2000L, EnrollmentStatus.ENROLLED)).thenReturn(1);

        // When
        Page<AvailableStudentDTO> result = classService.getAvailableStudentsForClass(
                classId, null, pageable, userId
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        AvailableStudentDTO studentDTO = result.getContent().get(0);

        // Verify basic student info
        assertThat(studentDTO.getId()).isEqualTo(2000L);
        assertThat(studentDTO.getStudentCode()).isEqualTo("STU001");
        assertThat(studentDTO.getFullName()).isEqualTo("John Doe");
        assertThat(studentDTO.getEmail()).isEqualTo("john.doe@test.com");
        assertThat(studentDTO.getBranchId()).isEqualTo(1L);
        assertThat(studentDTO.getBranchName()).isEqualTo("Main Branch");
        assertThat(studentDTO.getActiveEnrollments()).isEqualTo(1);
        assertThat(studentDTO.getCanEnroll()).isTrue();

        // Verify complete assessment data is included
        assertThat(studentDTO.getReplacementSkillAssessments()).hasSize(2);

        AvailableStudentDTO.SkillAssessmentDTO readingAssessment = studentDTO.getReplacementSkillAssessments().get(0);
        assertThat(readingAssessment.getSkill()).isEqualTo("READING");
        assertThat(readingAssessment.getScore()).isEqualTo(85);
        assertThat(readingAssessment.getAssessmentDate()).isEqualTo(LocalDate.of(2024, 10, 15));
        assertThat(readingAssessment.getAssessmentType()).isEqualTo("Placement Test");
        assertThat(readingAssessment.getNote()).isEqualTo("Good comprehension");
        assertThat(readingAssessment.getAssessedBy().getFullName()).isEqualTo("Jane Smith");

        // Verify level information
        AvailableStudentDTO.LevelInfoDTO levelInfo = readingAssessment.getLevel();
        assertThat(levelInfo.getCode()).isEqualTo("B1");
        assertThat(levelInfo.getName()).isEqualTo("Intermediate");
        assertThat(levelInfo.getSubject().getName()).isEqualTo("English");
        assertThat(levelInfo.getExpectedDurationHours()).isEqualTo(60);

        // Verify class match information
        AvailableStudentDTO.ClassMatchInfoDTO matchInfo = studentDTO.getClassMatchInfo();
        assertThat(matchInfo.getMatchPriority()).isEqualTo(1); // Perfect match
        assertThat(matchInfo.getMatchingSkill()).isEqualTo("READING");
        assertThat(matchInfo.getMatchReason()).isEqualTo("Perfect match - Assessment matches both Subject AND Level");
        assertThat(matchInfo.getMatchingLevel().getCode()).isEqualTo("B1");
    }

    @Test
    void getAvailableStudentsForClass_ShouldHandleStudentWithNoAssessments() {
        // Given
        Long classId = 1000L;
        Long userId = 3000L;
        Pageable pageable = PageRequest.of(0, 20);

        when(classRepository.findById(classId)).thenReturn(Optional.of(testClass));
        when(userBranchesRepository.findBranchIdsByUserId(userId)).thenReturn(List.of(1L));
        when(studentRepository.findAvailableStudentsForClass(eq(classId), eq(1L), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(testStudent)));
        when(skillAssessmentRepository.findByStudentIdIn(List.of(2000L))).thenReturn(List.of());
        when(enrollmentRepository.countByStudentIdAndStatus(2000L, EnrollmentStatus.ENROLLED)).thenReturn(0);

        // When
        Page<AvailableStudentDTO> result = classService.getAvailableStudentsForClass(
                classId, null, pageable, userId
        );

        // Then
        assertThat(result.getContent()).hasSize(1);
        AvailableStudentDTO studentDTO = result.getContent().get(0);

        // Verify basic student info still works
        assertThat(studentDTO.getId()).isEqualTo(2000L);
        assertThat(studentDTO.getCanEnroll()).isTrue();

        // Verify empty assessment list
        assertThat(studentDTO.getReplacementSkillAssessments()).isEmpty();

        // Verify no match information
        AvailableStudentDTO.ClassMatchInfoDTO matchInfo = studentDTO.getClassMatchInfo();
        assertThat(matchInfo.getMatchPriority()).isEqualTo(3); // No match
        assertThat(matchInfo.getMatchingSkill()).isNull();
        assertThat(matchInfo.getMatchReason()).isEqualTo("No skill assessment found for this course's subject");
    }
}