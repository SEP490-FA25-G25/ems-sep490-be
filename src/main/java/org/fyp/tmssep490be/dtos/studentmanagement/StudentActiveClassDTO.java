package org.fyp.tmssep490be.dtos.studentmanagement;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentActiveClassDTO {
    private Long classId;
    private String classCode;
    private String className;
    private String courseName;
    private String branchName;
    private LocalDate startDate;
    private LocalDate plannedEndDate;
    private String enrollmentStatus;
    private OffsetDateTime enrolledAt;
}