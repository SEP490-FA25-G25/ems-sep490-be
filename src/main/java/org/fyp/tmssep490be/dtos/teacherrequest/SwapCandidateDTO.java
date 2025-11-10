package org.fyp.tmssep490be.dtos.teacherrequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SwapCandidateDTO {
    private Long teacherId;
    private String fullName;
    private String email;
    private Integer skillPriority; // 3 = exact match, 2 = general, 1 = other
    private Integer availabilityPriority; // 2 = full-time or available, 1 = other
    private Boolean hasConflict; // true if teacher has conflicting schedule
}

