package org.fyp.tmssep490be.dtos.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseAssessmentDTO {
    private Long id;
    private String name;
    private String description;
    private String assessmentType;
    private BigDecimal weight;
    private BigDecimal maxScore;
    private String duration;
    private List<Long> sessionIds;
    private List<String> cloMappings;
    private Boolean isCompleted;
    private BigDecimal achievedScore;
    private String completedAt;
}