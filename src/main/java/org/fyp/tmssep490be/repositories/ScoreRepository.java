package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CourseAssessment;
import org.fyp.tmssep490be.entities.Enrollment;
import org.fyp.tmssep490be.entities.Score;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScoreRepository extends JpaRepository<Score, Long> {

    @Query("SELECT s FROM Score s WHERE s.student.id = :studentId AND s.assessment.courseAssessment.id = :courseAssessmentId")
    Optional<Score> findByEnrollmentAndAssessment(@Param("studentId") Long studentId, @Param("courseAssessmentId") Long courseAssessmentId);
}
