package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CourseAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseAssessmentRepository extends JpaRepository<CourseAssessment, Long> {
}
