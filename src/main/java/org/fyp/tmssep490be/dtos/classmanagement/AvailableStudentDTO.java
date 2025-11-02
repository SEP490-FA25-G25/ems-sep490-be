package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.*;

import java.time.LocalDate;

/**
 * DTO for available students to enroll in a class
 * Includes smart priority based on skill assessment matching
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableStudentDTO {
    private Long id;
    private String studentCode;
    private String fullName;
    private String email;
    private String phone;
    private String level; // Current student level

    // Branch info
    private Long branchId;
    private String branchName;

    // Skill assessment matching info
    private Integer matchPriority; // 1: Perfect match (Subject + Level), 2: Subject match only, 3: No match
    private String matchingSkillLevel; // Level code from assessment that matches
    private LocalDate lastAssessmentDate;
    private Integer lastAssessmentScore;

    // Enrollment info
    private Integer activeEnrollments;
    private Boolean canEnroll; // Based on max concurrent enrollments

    // Additional context
    private String notes; // Why this student is recommended
}
