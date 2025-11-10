package org.fyp.tmssep490be.dtos.teacherrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for listing teacher's future sessions (for request creation)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSessionDTO {
    private Long sessionId;
    private LocalDate date;
    private LocalTime startTime;
    private LocalTime endTime;
    private String className;
    private String courseName;
    private String topic;
    private Long daysFromNow;
    private String requestStatus; // "Có thể tạo request" hoặc "Đang chờ xử lý"
    private boolean hasPendingRequest; // true nếu đã có request pending/waiting_confirm/approved
}



