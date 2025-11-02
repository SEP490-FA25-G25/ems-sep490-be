package org.fyp.tmssep490be.dtos.studentmanagement;

import lombok.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentDetailDTO {
    private Long id;
    private String studentCode;
    private String fullName;
    private String email;
    private String phone;
    private String address;
    private String gender;
    private LocalDate dateOfBirth;
    private String level;
    private String status;
    private OffsetDateTime lastLoginAt;
    private String branchName;
    private Long branchId;

    // Academic Information
    private Long totalEnrollments;
    private Long activeEnrollments;
    private Long completedEnrollments;
    private LocalDate firstEnrollmentDate;
    private LocalDate lastEnrollmentDate;

    // Current Active Classes
    private List<StudentActiveClassDTO> currentClasses;
}