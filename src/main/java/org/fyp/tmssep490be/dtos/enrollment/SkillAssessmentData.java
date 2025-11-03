package org.fyp.tmssep490be.dtos.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Skill;

/**
 * Helper class for parsed skill assessment data
 * Used to hold parsed level and score from "Level-Score" format
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillAssessmentData {
    private Skill skill;
    private String levelCode;  // e.g., "B1", "A2", "N3"
    private Integer score;     // e.g., 75, 68, 90
    private boolean isValid;   // Whether the parsing was successful
    private String errorMessage; // Error message if parsing failed

    /**
     * Create empty/invalid assessment data
     */
    public static SkillAssessmentData empty(Skill skill) {
        return SkillAssessmentData.builder()
                .skill(skill)
                .levelCode(null)
                .score(null)
                .isValid(false)
                .build();
    }

    /**
     * Create valid assessment data
     */
    public static SkillAssessmentData valid(Skill skill, String levelCode, Integer score) {
        return SkillAssessmentData.builder()
                .skill(skill)
                .levelCode(levelCode)
                .score(score)
                .isValid(true)
                .build();
    }
}