package org.fyp.tmssep490be.dtos.classmanagement;

import lombok.*;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for available students to enroll in a class
 * Includes complete replacement skill assessment history and smart priority based on skill assessment matching
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableStudentDTO {
    private Long id;
    private String studentCode;
    private String fullName;
    private String email;
    private String phone;

    // Branch info
    private Long branchId;
    private String branchName;

    // Complete replacement skill assessment history
    private List<SkillAssessmentDTO> replacementSkillAssessments;

    // Class matching information
    private ClassMatchInfoDTO classMatchInfo;

    // Enrollment info
    private Integer activeEnrollments;
    private Boolean canEnroll; // Based on max concurrent enrollments

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillAssessmentDTO {
        private Long id;
        private String skill; // GENERAL, READING, WRITING, SPEAKING, LISTENING
        private LevelInfoDTO level;
        private Integer score;
        private LocalDate assessmentDate;
        private String assessmentType;
        private String note;
        private AssessorDTO assessedBy;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LevelInfoDTO {
        private Long id;
        private String code;
        private String name;
        private SubjectInfoDTO subject;
        private Integer expectedDurationHours;
        private String description;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubjectInfoDTO {
        private Long id;
        private String name;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssessorDTO {
        private Long id;
        private String fullName;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassMatchInfoDTO {
        private Integer matchPriority; // 1: Perfect match (Subject + Level), 2: Subject match only, 3: No match
        private String matchingSkill; // The skill that matches
        private LevelInfoDTO matchingLevel; // The level that matches
        private String matchReason; // Explanation of why this student is recommended
    }
}
