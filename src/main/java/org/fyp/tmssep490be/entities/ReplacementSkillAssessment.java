package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.Skill;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "replacement_skill_assessment", uniqueConstraints = {
    @UniqueConstraint(name = "uq_student_skill_assessment", columnNames = {"student_id", "skill", "assessment_date", "assessment_category"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReplacementSkillAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Skill skill;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "level_id")
    private Level level;

    /**
     * Raw score from the assessment (e.g., 32 out of 40 questions correct)
     */
    @Column(name = "raw_score", precision = 10, scale = 2)
    private BigDecimal rawScore;

    /**
     * Scaled/converted score (e.g., 7.5 for IELTS band, 750 for TOEIC)
     */
    @Column(name = "scaled_score", precision = 10, scale = 2)
    private BigDecimal scaledScore;

    /**
     * Score scale description (e.g., "0-9" for IELTS, "0-990" for TOEIC, "N1-N5" for JLPT)
     */
    @Column(name = "score_scale", length = 100)
    private String scoreScale;

    /**
     * Assessment category (e.g., PLACEMENT, MOCK, OFFICIAL, PRACTICE)
     */
    @Column(name = "assessment_category", length = 50)
    private String assessmentCategory;

    @Column(name = "assessment_date", nullable = false)
    private LocalDate assessmentDate;

    @Column(name = "assessment_type", length = 100)
    private String assessmentType;

    @Column(columnDefinition = "TEXT")
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assessed_by")
    private UserAccount assessedBy;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private OffsetDateTime updatedAt;
}
