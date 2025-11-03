package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.config.AbstractRepositoryTest;
import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository tests for enrollment-related database operations
 */
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

    @BeforeEach
    void setUp() {
        // Clean up
        enrollmentRepository.deleteAll();
        classRepository.deleteAll();
        studentRepository.deleteAll();
        userAccountRepository.deleteAll();
        courseRepository.deleteAll();
        levelRepository.deleteAll();
        subjectRepository.deleteAll();
        branchRepository.deleteAll();
        centerRepository.deleteAll();

        // Create test data
        Center center = new Center();
        center.setCode("CT001");
        center.setName("Test Center");
        center = centerRepository.save(center);

        Branch branch = new Branch();
        branch.setCode("BR001");
        branch.setName("Test Branch");
        branch.setAddress("Test Address");
        branch.setCenter(center);
        branch = branchRepository.save(branch);

        Subject subject = new Subject();
        subject.setCode("ENG");
        subject.setName("English");
        subject = subjectRepository.save(subject);

        Level level = new Level();
        level.setCode("A1");
        level.setName("Beginner A1");
        level.setSubject(subject);
        level.setSortOrder(1);
        level = levelRepository.save(level);

        Course course = new Course();
        course.setCode("ENG-A1-V1");
        course.setName("English A1");
        course.setSubject(subject);
        course.setLevel(level);
        course.setVersion(1);
        course = courseRepository.save(course);

        testClass = new ClassEntity();
        testClass.setCode("CLASS001");
        testClass.setName("Test Class");
        testClass.setCourse(course);
        testClass.setBranch(branch);
        testClass.setMaxCapacity(20);
        testClass.setStartDate(java.time.LocalDate.now().plusDays(7)); // Required field
        testClass.setPlannedEndDate(java.time.LocalDate.now().plusWeeks(12));
        testClass.setModality(org.fyp.tmssep490be.entities.enums.Modality.ONLINE); // Required field
        testClass = classRepository.save(testClass);

        // Create students
        UserAccount user1 = new UserAccount();
        user1.setEmail("student1@test.com");
        user1.setFullName("Student 1");
        user1.setGender(org.fyp.tmssep490be.entities.enums.Gender.MALE); // Required field
        user1 = userAccountRepository.save(user1);

        testStudent1 = new Student();
        testStudent1.setUserAccount(user1);
        testStudent1.setStudentCode("ST001");
        testStudent1 = studentRepository.save(testStudent1);

        UserAccount user2 = new UserAccount();
        user2.setEmail("student2@test.com");
        user2.setFullName("Student 2");
        user2.setGender(org.fyp.tmssep490be.entities.enums.Gender.FEMALE); // Required field
        user2 = userAccountRepository.save(user2);

        testStudent2 = new Student();
        testStudent2.setUserAccount(user2);
        testStudent2.setStudentCode("ST002");
        testStudent2 = studentRepository.save(testStudent2);
    }

    @Test
    @DisplayName("Should count enrolled students correctly")
    void shouldCountEnrolledStudents() {
        // Arrange
        Enrollment enrollment1 = Enrollment.builder()
                .classId(testClass.getId())
                .studentId(testStudent1.getId())
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(1L)
                .build();
        enrollmentRepository.save(enrollment1);

        Enrollment enrollment2 = Enrollment.builder()
                .classId(testClass.getId())
                .studentId(testStudent2.getId())
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(1L)
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
        // Arrange
        Enrollment enrollment = Enrollment.builder()
                .classId(testClass.getId())
                .studentId(testStudent1.getId())
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(1L)
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
        // Arrange
        Enrollment enrollment1 = Enrollment.builder()
                .classId(testClass.getId())
                .studentId(testStudent1.getId())
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(1L)
                .build();
        enrollmentRepository.save(enrollment1);

        Enrollment enrollment2 = Enrollment.builder()
                .classId(testClass.getId())
                .studentId(testStudent2.getId())
                .status(EnrollmentStatus.DROPPED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(1L)
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
        // Arrange
        Enrollment enrollment = Enrollment.builder()
                .classId(testClass.getId())
                .studentId(testStudent1.getId())
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(1L)
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
        // Arrange
        Long academicAffairUserId = 100L;
        Enrollment enrollment = Enrollment.builder()
                .classId(testClass.getId())
                .studentId(testStudent1.getId())
                .status(EnrollmentStatus.ENROLLED)
                .enrolledAt(OffsetDateTime.now())
                .enrolledBy(academicAffairUserId)
                .build();

        // Act
        Enrollment saved = enrollmentRepository.save(enrollment);

        // Assert
        assertThat(saved.getEnrolledBy()).isEqualTo(academicAffairUserId);
        assertThat(saved.getEnrolledAt()).isNotNull();
    }
}
