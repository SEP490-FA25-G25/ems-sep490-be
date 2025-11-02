package org.fyp.tmssep490be.dtos.studentmanagement;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentQuickSearchDTO {
    private Long id;
    private String studentCode;
    private String fullName;
    private String email;
    private String level;
    private Long branchId;
    private String branchName;
    private Boolean isAvailable; // true if not currently enrolled in conflicting classes
}