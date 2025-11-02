package org.fyp.tmssep490be.dtos.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Gender;

import java.time.LocalDate;

/**
 * Data của mỗi student trong Excel (sau khi parse)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentEnrollmentData {
    // From Excel
    private String studentCode;  // Nullable
    private String fullName;
    private String email;
    private String phone;
    private Gender gender;
    private LocalDate dob;
    private String level;  // A1, A2, B1...

    // Resolution result (sau khi system xử lý)
    private StudentResolutionStatus status;  // FOUND/CREATE/DUPLICATE/ERROR
    private Long resolvedStudentId;  // Nếu FOUND
    private String errorMessage;  // Nếu ERROR
}
