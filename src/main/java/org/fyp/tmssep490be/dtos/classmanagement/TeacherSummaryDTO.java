package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.*;

/**
 * Summary information about a teacher teaching in a class
 * Used for displaying teacher lists in class management views
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherSummaryDTO {

    private Long id;
    private Long teacherId;
    private String fullName;
    private String email;
    private String phone;
    private String employeeCode;

    /**
     * Number of sessions this teacher teaches in the class
     * Helps identify primary vs assistant teachers
     */
    private Integer sessionCount;
}
