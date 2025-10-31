package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.MappingStatus;

import java.io.Serializable;

@Entity
@Table(name = "course_assessment_clo_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAssessmentCLOMapping implements Serializable {

    @EmbeddedId
    private CourseAssessmentCLOMappingId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("courseAssessmentId")
    @JoinColumn(name = "course_assessment_id")
    private CourseAssessment courseAssessment;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cloId")
    @JoinColumn(name = "clo_id")
    private CLO clo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MappingStatus status;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class CourseAssessmentCLOMappingId implements Serializable {
        @Column(name = "course_assessment_id")
        private Long courseAssessmentId;

        @Column(name = "clo_id")
        private Long cloId;
    }
}
