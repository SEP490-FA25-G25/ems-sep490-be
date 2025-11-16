package org.fyp.tmssep490be.dtos.studentattendance;

import lombok.Builder;
import lombok.Data;
import org.fyp.tmssep490be.entities.enums.AttendanceStatus;
import org.fyp.tmssep490be.entities.enums.HomeworkStatus;

import java.time.LocalDate;

@Data
@Builder
public class StudentAttendanceReportSessionDTO {
    private Long sessionId;
    private LocalDate date;
    private Integer index; // optional, may be null if not modeled
    private String status; // Session status textual

    private AttendanceStatus attendanceStatus;
    private HomeworkStatus homeworkStatus;
    private boolean isMakeup;
    private String note;

    private MakeupInfo makeupSessionInfo;

    @Data
    @Builder
    public static class MakeupInfo {
        private Long sessionId;
        private Long classId;
        private String classCode;
        private LocalDate date;
        private Boolean attended;
    }
}


