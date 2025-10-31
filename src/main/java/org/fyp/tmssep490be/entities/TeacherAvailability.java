package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;
import org.fyp.tmssep490be.entities.base.BaseEntity;

import java.io.Serializable;
import java.time.LocalDate;

@Entity
@Table(name = "teacher_availability")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherAvailability extends BaseEntity implements Serializable {

    @EmbeddedId
    private TeacherAvailabilityId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("teacherId")
    @JoinColumn(name = "teacher_id")
    private Teacher teacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("timeSlotTemplateId")
    @JoinColumn(name = "time_slot_template_id")
    private TimeSlotTemplate timeSlotTemplate;

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Embeddable
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TeacherAvailabilityId implements Serializable {
        @Column(name = "teacher_id")
        private Long teacherId;

        @Column(name = "time_slot_template_id")
        private Long timeSlotTemplateId;

        @Column(name = "day_of_week")
        private Short dayOfWeek;
    }
}
