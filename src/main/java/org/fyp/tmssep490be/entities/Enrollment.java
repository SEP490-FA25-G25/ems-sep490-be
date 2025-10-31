package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.base.BaseEntity;
import org.fyp.tmssep490be.entities.enums.EnrollmentStatus;

import java.time.OffsetDateTime;

@Entity
@Table(name = "enrollment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Enrollment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private Student student;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EnrollmentStatus status = EnrollmentStatus.ENROLLED;

    @Column(name = "enrolled_at")
    private OffsetDateTime enrolledAt;

    @Column(name = "left_at")
    private OffsetDateTime leftAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "join_session_id")
    private Session joinSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "left_session_id")
    private Session leftSession;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrolled_by")
    private UserAccount enrolledBy;
}
