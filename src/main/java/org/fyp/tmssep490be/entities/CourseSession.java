package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.base.BaseEntity;
import org.fyp.tmssep490be.entities.enums.Skill;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "course_session", uniqueConstraints = {
    @UniqueConstraint(name = "uq_course_session_phase_sequence", columnNames = {"phase_id", "sequence_no"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSession extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id", nullable = false)
    private CoursePhase phase;

    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    @Column(length = 500)
    private String topic;

    @Column(name = "student_task", columnDefinition = "TEXT")
    private String studentTask;

    @Column(name = "skill_set", columnDefinition = "skill_enum[]")
    @Enumerated(EnumType.STRING)
    private Skill[] skillSet;

    @OneToMany(mappedBy = "courseSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseMaterial> courseMaterials = new HashSet<>();

    @OneToMany(mappedBy = "courseSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<CourseSessionCLOMapping> courseSessionCLOMappings = new HashSet<>();

    @OneToMany(mappedBy = "courseSession", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Session> sessions = new HashSet<>();
}
