package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.base.BaseEntity;

@Entity
@Table(name = "qa_report")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QAReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phase_id")
    private CoursePhase phase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private UserAccount reportedBy;

    @Column(name = "report_type", length = 100)
    private String reportType;

    @Column(length = 50)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String findings;

    @Column(name = "action_items", columnDefinition = "TEXT")
    private String actionItems;
}
