package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for swap candidate teachers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwapCandidateDTO {
    private Long teacherId;
    private String fullName;
    private String email;
    private Integer skillPriority; // Higher = better match
    private Integer availabilityPriority; // Higher = more available
    private Boolean hasConflict; // true if teacher has conflict at session time
}

