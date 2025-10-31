package org.fyp.tmssep490be.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student_feedback_response")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StudentFeedbackResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private StudentFeedback feedback;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private FeedbackQuestion question;

    @Column(name = "rating")
    private Short rating;
}
