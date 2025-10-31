package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.enums.MappingStatus;

import java.io.Serializable;

@Entity
@Table(name = "course_session_clo_mapping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseSessionCLOMapping implements Serializable {

    @EmbeddedId
    private CourseSessionCLOMappingId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("courseSessionId")
    @JoinColumn(name = "course_session_id")
    private CourseSession courseSession;

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
    public static class CourseSessionCLOMappingId implements Serializable {
        @Column(name = "course_session_id")
        private Long courseSessionId;

        @Column(name = "clo_id")
        private Long cloId;
    }
}
