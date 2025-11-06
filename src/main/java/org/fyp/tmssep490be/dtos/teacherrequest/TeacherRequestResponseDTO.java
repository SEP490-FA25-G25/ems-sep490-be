package org.fyp.tmssep490be.dtos.teacherrequest;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO for Teacher Request response
 * Fields with null value will be excluded from JSON response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TeacherRequestResponseDTO {

    private Long id;
    private TeacherRequestType requestType;
    private RequestStatus status;
    private Long sessionId;
    private Long replacementTeacherId;
    private LocalDate newDate;
    private Long newTimeSlotId;
    private Long newResourceId;
    private String requestReason;
    private String note;
    private OffsetDateTime submittedAt;
    private OffsetDateTime decidedAt;
    private Long newSessionId;
}



