package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.base.BaseEntity;
import org.fyp.tmssep490be.entities.enums.AssessmentKind;
import org.fyp.tmssep490be.entities.enums.Skill;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "course_assessment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAssessment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssessmentKind kind;

    @Column(name = "duration_minutes")
    private Integer durationMinutes;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "skill_enum[]")
    @Enumerated(EnumType.STRING)
    private Skill[] skills;

    @Column(name = "max_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal maxScore;

    @Column(columnDefinition = "TEXT")
    private String note;

    @OneToMany(mappedBy = "courseAssessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseAssessmentCLOMapping> courseAssessmentCLOMappings = new HashSet<>();

    @OneToMany(mappedBy = "courseAssessment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Assessment> assessments = new HashSet<>();
}
