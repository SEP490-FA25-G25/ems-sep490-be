package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.base.BaseEntity;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "course_phase", uniqueConstraints = {
    @UniqueConstraint(name = "uq_course_phase_course_number", columnNames = {"course_id", "phase_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoursePhase extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(name = "phase_number", nullable = false)
    private Integer phaseNumber;

    @Column(length = 255)
    private String name;

    @Column(name = "duration_weeks")
    private Integer durationWeeks;

    @Column(name = "learning_focus", columnDefinition = "TEXT")
    private String learningFocus;

    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseSession> courseSessions = new HashSet<>();

    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseMaterial> courseMaterials = new HashSet<>();

    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<StudentFeedback> studentFeedbacks = new HashSet<>();

    @OneToMany(mappedBy = "phase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<QAReport> qaReports = new HashSet<>();
}
