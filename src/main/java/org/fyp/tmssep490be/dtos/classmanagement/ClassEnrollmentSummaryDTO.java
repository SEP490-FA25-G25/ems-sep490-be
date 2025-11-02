package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.*;

import java.time.LocalDate;

/**
 * Quick enrollment summary for class
 * Used in GET /api/v1/classes/{id}/summary endpoint
 * Provides lightweight capacity information for list views and quick validations
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassEnrollmentSummaryDTO {

    private Long classId;
    private String classCode;
    private String className;

    // Capacity information
    private Integer currentEnrolled;
    private Integer maxCapacity;
    private Integer availableSlots;
    private Double utilizationRate;

    // Enrollment capability
    private Boolean canEnrollStudents;
    private String enrollmentRestrictionReason;

    // Status information
    private String status;
    private String approvalStatus;
    private LocalDate startDate;

    /**
     * Get capacity percentage for UI display
     */
    public Double getCapacityPercentage() {
        if (maxCapacity == null || maxCapacity == 0) {
            return 0.0;
        }
        return (double) currentEnrolled / maxCapacity * 100;
    }

    /**
     * Check if class is full
     */
    public Boolean isFull() {
        return availableSlots != null && availableSlots <= 0;
    }

    /**
     * Get capacity status for UI
     */
    public String getCapacityStatus() {
        if (isFull()) {
            return "FULL";
        } else if (utilizationRate != null && utilizationRate >= 90.0) {
            return "ALMOST_FULL";
        } else if (utilizationRate != null && utilizationRate >= 70.0) {
            return "GOOD_ENROLLMENT";
        } else {
            return "LOW_ENROLLMENT";
        }
    }
}