package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.*;
import org.fyp.tmssep490be.entities.enums.ApprovalStatus;
import org.fyp.tmssep490be.entities.enums.ClassStatus;
import org.fyp.tmssep490be.entities.enums.Modality;

import java.time.LocalDate;

/**
 * Compact class information for list view
 * Used in GET /api/v1/classes endpoint
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassListItemDTO {

    private Long id;
    private String code;
    private String name;
    private String courseName;
    private String courseCode;
    private String branchName;
    private String branchCode;

    private Modality modality;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private ClassStatus status;
    private ApprovalStatus approvalStatus;

    // Capacity information
    private Integer maxCapacity;
    private Integer currentEnrolled;
    private Integer availableSlots;
    private Double utilizationRate;

    // Teacher information (if available)
    private String teacherName;

    // Schedule summary
    private String scheduleSummary;

    // Quick checks for UI
    private Boolean canEnrollStudents;
    private String enrollmentRestrictionReason;
}