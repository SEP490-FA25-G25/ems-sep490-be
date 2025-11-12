package org.fyp.tmssep490be.dtos.attendance;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SessionReportResponseDTO {
    private Long sessionId;
    private Long classId;
    private String classCode;
    private String courseCode;
    private String courseName;
    private LocalDate date;
    private String timeSlotName;
    private String teacherNote;
    private AttendanceSummaryDTO summary;
}


