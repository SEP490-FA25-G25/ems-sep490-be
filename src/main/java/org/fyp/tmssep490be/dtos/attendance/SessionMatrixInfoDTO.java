package org.fyp.tmssep490be.dtos.attendance;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SessionMatrixInfoDTO {
    private Long sessionId;
    private LocalDate date;
    private String timeSlotName;
    private String status;
}


