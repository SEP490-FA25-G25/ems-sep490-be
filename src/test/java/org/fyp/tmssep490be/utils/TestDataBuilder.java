package org.fyp.tmssep490be.utils;

import org.fyp.tmssep490be.entities.*;
import org.fyp.tmssep490be.entities.enums.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Test data builder utility for creating test entities.
 * Provides fluent API for building test data with sensible defaults.
 *
 * Usage:
 * <pre>
 * Center center = TestDataBuilder.buildCenter()
 *     .name("Test Center")
 *     .build();
 * </pre>
 */
public class TestDataBuilder {

    // Center Builder
    public static CenterBuilder buildCenter() {
        return new CenterBuilder();
    }

    public static class CenterBuilder {
        private final Center center = new Center();

        public CenterBuilder() {
            // Set defaults
            center.setCode("TC001");
            center.setName("Test Training Center");
            center.setDescription("A test training center");
            center.setAddress("123 Test Street");
            center.setPhone("0123456789");
            center.setEmail("test@center.com");
        }

        public CenterBuilder code(String code) {
            center.setCode(code);
            return this;
        }

        public CenterBuilder name(String name) {
            center.setName(name);
            return this;
        }

        public CenterBuilder address(String address) {
            center.setAddress(address);
            return this;
        }

        public CenterBuilder phone(String phone) {
            center.setPhone(phone);
            return this;
        }

        public CenterBuilder email(String email) {
            center.setEmail(email);
            return this;
        }

        public Center build() {
            return center;
        }
    }

    // Branch Builder
    public static BranchBuilder buildBranch() {
        return new BranchBuilder();
    }

    public static class BranchBuilder {
        private final Branch branch = new Branch();

        public BranchBuilder() {
            // Set defaults
            branch.setCode("BR001");
            branch.setName("Test Branch");
            branch.setAddress("456 Branch Street");
            branch.setPhone("0987654321");
            branch.setEmail("test@branch.com");
            branch.setDistrict("Test District");
            branch.setCity("Test City");
        }

        public BranchBuilder center(Center center) {
            branch.setCenter(center);
            return this;
        }

        public BranchBuilder code(String code) {
            branch.setCode(code);
            return this;
        }

        public BranchBuilder name(String name) {
            branch.setName(name);
            return this;
        }

        public Branch build() {
            return branch;
        }
    }

    // Subject Builder
    public static SubjectBuilder buildSubject() {
        return new SubjectBuilder();
    }

    public static class SubjectBuilder {
        private final Subject subject = new Subject();

        public SubjectBuilder() {
            // Set defaults
            subject.setCode("ENG");
            subject.setName("English");
            subject.setDescription("English language training");
        }

        public SubjectBuilder code(String code) {
            subject.setCode(code);
            return this;
        }

        public SubjectBuilder name(String name) {
            subject.setName(name);
            return this;
        }

        public SubjectBuilder description(String description) {
            subject.setDescription(description);
            return this;
        }

        public Subject build() {
            return subject;
        }
    }

    // Level Builder
    public static LevelBuilder buildLevel() {
        return new LevelBuilder();
    }

    public static class LevelBuilder {
        private final Level level = new Level();

        public LevelBuilder() {
            // Set defaults
            level.setCode("A1");
            level.setName("Beginner");
            level.setDescription("Beginner level");
            level.setSortOrder(1);
            level.setExpectedDurationHours(40);
        }

        public LevelBuilder subject(Subject subject) {
            level.setSubject(subject);
            return this;
        }

        public LevelBuilder code(String code) {
            level.setCode(code);
            return this;
        }

        public LevelBuilder name(String name) {
            level.setName(name);
            return this;
        }

        public LevelBuilder sortOrder(Integer order) {
            level.setSortOrder(order);
            return this;
        }

        public Level build() {
            return level;
        }
    }

    // Course Builder
    public static CourseBuilder buildCourse() {
        return new CourseBuilder();
    }

    public static class CourseBuilder {
        private final Course course = new Course();

        public CourseBuilder() {
            // Set defaults - need both subject and level
            course.setCode("ENG-A1-2024");
            course.setLogicalCourseCode("ENG-A1");
            course.setName("English A1 Course");
            course.setDescription("Beginner English course");
            course.setTotalHours(80);
            course.setDurationWeeks(10);
            course.setSessionPerWeek(2);
            course.setHoursPerSession(new BigDecimal("2.00"));
            course.setVersion(1);
            course.setStatus(CourseStatus.DRAFT);
            course.setApprovalStatus(ApprovalStatus.PENDING);
            course.setEffectiveDate(LocalDate.now().plusDays(30));
        }

        public CourseBuilder subject(Subject subject) {
            course.setSubject(subject);
            return this;
        }

        public CourseBuilder level(Level level) {
            course.setLevel(level);
            // Also set subject from level if not already set
            if (level != null && course.getSubject() == null) {
                course.setSubject(level.getSubject());
            }
            return this;
        }

        public CourseBuilder code(String code) {
            course.setCode(code);
            return this;
        }

        public CourseBuilder name(String name) {
            course.setName(name);
            return this;
        }

        public CourseBuilder status(CourseStatus status) {
            course.setStatus(status);
            return this;
        }

        public CourseBuilder approvalStatus(ApprovalStatus approvalStatus) {
            course.setApprovalStatus(approvalStatus);
            return this;
        }

        public Course build() {
            return course;
        }
    }

    // Additional builders can be added as needed...
    // ClassEntity, Session, Enrollment, etc.
}
