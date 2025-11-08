package org.fyp.tmssep490be.dtos.enrollment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Skill;

import java.math.BigDecimal;

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
    
    /**
     * Raw score from assessment (e.g., 32 out of 40 questions)
     */
    private BigDecimal rawScore;
    
    /**
     * Scaled/converted score (e.g., 7.5 for IELTS, 750 for TOEIC)
     */
    private BigDecimal scaledScore;
    
    /**
     * Score scale description (e.g., "0-9", "0-990", "N1-N5")
     */
    private String scoreScale;
    
    /**
     * Assessment category (e.g., PLACEMENT, MOCK, OFFICIAL, PRACTICE)
     */
    private String assessmentCategory;
    
    private boolean isValid;   // Whether the parsing was successful
    private String errorMessage; // Error message if parsing failed

    /**
     * Create empty/invalid assessment data
     */
    public static SkillAssessmentData empty(Skill skill) {
        return SkillAssessmentData.builder()
                .skill(skill)
                .levelCode(null)
                .rawScore(null)
                .scaledScore(null)
                .scoreScale(null)
                .assessmentCategory(null)
                .isValid(false)
                .build();
    }

    /**
     * Create valid assessment data with flexible scoring
     */
    public static SkillAssessmentData valid(Skill skill, String levelCode, BigDecimal rawScore, 
                                           BigDecimal scaledScore, String scoreScale, String assessmentCategory) {
        return SkillAssessmentData.builder()
                .skill(skill)
                .levelCode(levelCode)
                .rawScore(rawScore)
                .scaledScore(scaledScore)
                .scoreScale(scoreScale)
                .assessmentCategory(assessmentCategory)
                .isValid(true)
                .build();
    }
}