package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.config.AbstractRepositoryTest;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.fyp.tmssep490be.utils.TestDataBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for enrollment-related database operations.
 * Uses modern Spring Boot 3.5.7 @DataJpaTest pattern with Testcontainers.
 * TestDataBuilder provides consistent test data creation.
 */
@DataJpaTest
@DisplayName("EnrollmentRepository Integration Tests")
class EnrollmentRepositoryTest extends AbstractRepositoryTest {

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private ClassRepository classRepository;

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private SubjectRepository subjectRepository;

    @Autowired
    private LevelRepository levelRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private CenterRepository centerRepository;

    private ClassEntity testClass;
    private Student testStudent1;
    private Student testStudent2;
    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        // Note: With @Transactional in AbstractRepositoryTest, data cleanup is automatic
        // TestDataBuilder provides consistent, reusable test data patterns

        // Create test data using TestDataBuilder for consistency
        Center center = TestDataBuilder.buildCenter()
                .code("CT001")
                .name("Test Center")
                .build();
        center = centerRepository.save(center);

        Branch branch = TestDataBuilder.buildBranch()
                .code("BR001")
                .name("Test Branch")
                .address("Test Address")
                .center(center)
                .build();
        branch = branchRepository.save(branch);

        Subject subject = TestDataBuilder.buildSubject()
                .code("ENG")
                .name("English")
                .build();
        subject = subjectRepository.save(subject);

        Level level = TestDataBuilder.buildLevel()
                .code("A1")
                .name("Beginner A1")
                .subject(subject)
                .sortOrder(1)
                .build();
        level = levelRepository.save(level);

        // Create a UserAccount for enrolled_by foreign key constraint
        testUser = TestDataBuilder.buildUserAccount()
                .email("test@enrollment.com")
                .fullName("Test Enrollment User")
                .status(UserStatus.ACTIVE)
                .build();
        testUser = userAccountRepository.save(testUser);

        Course course = TestDataBuilder.buildCourse()
                .code("ENG-A1-V1")
                .name("English A1")
                .subject(subject)
                .level(level)
                .build();
        course = courseRepository.save(course);

        testClass = TestDataBuilder.buildClassEntity()
                .code("CLASS001")
                .name("Test Class")
                .course(course)
                .branch(branch)
                .maxCapacity(20)
                .build();
        testClass = classRepository.save(testClass);

        // Create students using TestDataBuilder
        UserAccount user1 = TestDataBuilder.buildUserAccount()
                .email("student1@test.com")
                .fullName("Student 1")
                .gender(Gender.MALE)
                .build();
        user1 = userAccountRepository.save(user1);

        testStudent1 = TestDataBuilder.buildStudent()
                .userAccount(user1)
                .studentCode("ST001")
                .build();
        testStudent1 = studentRepository.save(testStudent1);

        UserAccount user2 = TestDataBuilder.buildUserAccount()
                .email("student2@test.com")
                .fullName("Student 2")
                .gender(Gender.FEMALE)
                .build();
        user2 = userAccountRepository.save(user2);

        testStudent2 = TestDataBuilder.buildStudent()
                .userAccount(user2)
                .studentCode("ST002")
                .build();
        testStudent2 = studentRepository.save(testStudent2);
    }

    @Test
    @DisplayName("Should count enrolled students correctly")
    void shouldCountEnrolledStudents() {
        // Arrange - Use TestDataBuilder for consistent test data
        Enrollment enrollment1 = TestDataBuilder.buildEnrollment()
                .classId(testClass.getId())
                .studentId(testStudent1.getId())
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(testUser.getId())
                .build();
        enrollmentRepository.save(enrollment1);

        Enrollment enrollment2 = TestDataBuilder.buildEnrollment()
                .classId(testClass.getId())
                .studentId(testStudent2.getId())
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(testUser.getId())
                .build();
        enrollmentRepository.save(enrollment2);

        // Act
        int count = enrollmentRepository.countByClassIdAndStatus(
                testClass.getId(), EnrollmentStatus.ENROLLED
        );

        // Assert
        assertThat(count).isEqualTo(2);
    }

    @Test
    @DisplayName("Should check if student already enrolled")
    void shouldCheckIfStudentAlreadyEnrolled() {
        // Arrange - Use TestDataBuilder for consistent test data
        Enrollment enrollment = TestDataBuilder.buildEnrollment()
                .classId(testClass.getId())
                .studentId(testStudent1.getId())
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(testUser.getId())
                .build();
        enrollmentRepository.save(enrollment);

        // Act & Assert
        assertThat(enrollmentRepository.existsByClassIdAndStudentIdAndStatus(
                testClass.getId(), testStudent1.getId(), EnrollmentStatus.ENROLLED
        )).isTrue();

        assertThat(enrollmentRepository.existsByClassIdAndStudentIdAndStatus(
                testClass.getId(), testStudent2.getId(), EnrollmentStatus.ENROLLED
        )).isFalse();
    }

    @Test
    @DisplayName("Should not count dropped students")
    void shouldNotCountDroppedStudents() {
        // Arrange - Use TestDataBuilder for consistent test data
        Enrollment enrollment1 = TestDataBuilder.buildEnrollment()
                .classId(testClass.getId())
                .studentId(testStudent1.getId())
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(testUser.getId())
                .build();
        enrollmentRepository.save(enrollment1);

        Enrollment enrollment2 = TestDataBuilder.buildEnrollment()
                .classId(testClass.getId())
                .studentId(testStudent2.getId())
                .status(EnrollmentStatus.DROPPED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(testUser.getId())
                .build();
        enrollmentRepository.save(enrollment2);

        // Act
        int count = enrollmentRepository.countByClassIdAndStatus(
                testClass.getId(), EnrollmentStatus.ENROLLED
        );

        // Assert
        assertThat(count).isEqualTo(1); // Only enrolled student, not dropped
    }

    @Test
    @DisplayName("Should save enrollment with capacity override")
    void shouldSaveEnrollmentWithCapacityOverride() {
        // Arrange - Use TestDataBuilder for consistent test data
        Enrollment enrollment = TestDataBuilder.buildEnrollment()
                .classId(testClass.getId())
                .studentId(testStudent1.getId())
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(testUser.getId())
                .capacityOverride(true)
                .overrideReason("High demand - additional resources allocated")
                .build();

        // Act
        Enrollment saved = enrollmentRepository.save(enrollment);

        // Assert
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCapacityOverride()).isTrue();
        assertThat(saved.getOverrideReason()).isEqualTo("High demand - additional resources allocated");
    }

    @Test
    @DisplayName("Should track enrolled_by for audit")
    void shouldTrackEnrolledBy() {
        // Arrange - Create academic affairs user for enrolled_by foreign key constraint
        UserAccount academicUser = TestDataBuilder.buildUserAccount()
                .email("academic@enrollment.com")
                .fullName("Academic Affairs User")
                .status(UserStatus.ACTIVE)
                .build();
        academicUser = userAccountRepository.save(academicUser);

        Enrollment enrollment = TestDataBuilder.buildEnrollment()
                .classId(testClass.getId())
                .studentId(testStudent1.getId())
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(academicUser.getId())
                .build();

        // Act
        Enrollment saved = enrollmentRepository.save(enrollment);

        // Assert
        assertThat(saved.getEnrolledBy()).isEqualTo(academicUser.getId());
        assertThat(saved.getEnrolledAt()).isNotNull();
    }
}
