package org.fyp.tmssep490be.dtos.studentattendance;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class StudentAttendanceOverviewItemDTO {
    private Long classId;
    private String classCode;
    private Long courseId;
    private String courseCode;
    private String courseName;

    private int totalSessions;
    private int attended;
    private int absent;
    private int excused;
    private int upcoming;
    private double attendanceRate;

    private String status;
    private OffsetDateTime lastUpdated;
}




