package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleSlotSuggestionDTO {
    private Long timeSlotId;
    private String label;
    private Boolean hasAvailableResource;
    private Integer availableResourceCount;
}






