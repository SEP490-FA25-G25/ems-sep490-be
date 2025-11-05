package org.fyp.tmssep490be.dtos.studentmanagement;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Skill;

import java.math.BigDecimal;

/**
 * Input DTO for skill assessment during student creation
 * Used to create initial ReplacementSkillAssessment records
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkillAssessmentInput {

    @NotNull(message = "Skill is required")
    private Skill skill;

    @NotNull(message = "Level ID is required")
    private Long levelId;

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

    private String note;
}
