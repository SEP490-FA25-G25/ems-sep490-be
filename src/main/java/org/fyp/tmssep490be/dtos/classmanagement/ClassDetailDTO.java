package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;

import java.time.LocalDate;
import java.util.List;

/**
 * Comprehensive class information for detail view
 * Used in GET /api/v1/classes/{id} endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassDetailDTO {

    private Long id;
    private String code;
    private String name;

    // Related entities
    private CourseDTO course;
    private BranchDTO branch;

    // Class information
    private Modality modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private LocalDate actualEndDate;
    private Short[] scheduleDays;
    private Integer maxCapacity;

    // Status information
    private ClassStatus status;
    private ApprovalStatus approvalStatus;
    private String rejectionReason;

    // Dates
    private LocalDate submittedAt;
    private LocalDate decidedAt;
    private String decidedByName;

    // Room information
    private String room;

    // Teacher information
    private String teacherName;

    // Schedule summary
    private String scheduleSummary;

    // Enrollment summary
    private EnrollmentSummary enrollmentSummary;

    // Upcoming sessions (next 5)
    private List<SessionDTO> upcomingSessions;

    /**
     * Nested DTO for course information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CourseDTO {
        private Long id;
        private String code;
        private String name;
        private String description;
        private Integer totalHours;
        private Integer durationWeeks;
        private Integer sessionPerWeek;
    }

    /**
     * Nested DTO for branch information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchDTO {
        private Long id;
        private String code;
        private String name;
        private String address;
        private String phone;
        private String email;
    }

    /**
     * Nested DTO for enrollment summary
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EnrollmentSummary {
        private Integer currentEnrolled;
        private Integer maxCapacity;
        private Integer availableSlots;
        private Double utilizationRate;
        private Boolean canEnrollStudents;
        private String enrollmentRestrictionReason;
    }

    /**
     * Nested DTO for session information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionDTO {
        private Long id;
        private LocalDate date;
        private String startTime;
        private String endTime;
        private String teacherName;
        private String room;
        private String status;
        private String type;
    }
}