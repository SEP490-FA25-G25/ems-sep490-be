package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;

import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * DTO for listing Teacher Requests (simplified version)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherRequestListDTO {

    private Long id;
    private TeacherRequestType requestType;
    private RequestStatus status;
    private Long sessionId;
    private LocalDate sessionDate;
    private String className;
    private String classCode;
    private String requestReason; // Lý do tạo request (có thể truncated ở frontend nếu cần)
    private OffsetDateTime submittedAt;
    private OffsetDateTime decidedAt;
}



