package org.fyp.tmssep490be.dtos.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Gender;

import java.time.LocalDate;

/**
 * Data của mỗi student trong Excel (sau khi parse)
 * Multi-skill format: "Level-Score" (e.g., "B1-75")
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
    private String facebookUrl;
    private String address;
    private Gender gender;
    private LocalDate dob;

    // Multi-skill assessments (format: "Level-Score")
    private String general;      // "B1-75" → level=B1, score=75
    private String reading;      // "A2-68" → level=A2, score=68
    private String writing;      // "B1-72" → level=B1, score=72
    private String speaking;     // "A2-65" → level=A2, score=65
    private String listening;    // "B1-70" → level=B1, score=70

    // Resolution result (sau khi system xử lý)
    private StudentResolutionStatus status;  // FOUND/CREATE/DUPLICATE/ERROR
    private Long resolvedStudentId;  // Nếu FOUND
    private String errorMessage;  // Nếu ERROR
}
