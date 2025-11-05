package org.fyp.tmssep490be.dtos.studentmanagement;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fyp.tmssep490be.entities.enums.Skill;

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

    @NotNull(message = "Score is required")
    @Min(value = 0, message = "Score must be >= 0")
    @Max(value = 100, message = "Score must be <= 100")
    private Integer score;

    private String note;
}
