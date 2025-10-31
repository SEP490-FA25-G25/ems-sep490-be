package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CourseAssessmentCLOMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseAssessmentCLOMappingRepository extends JpaRepository<CourseAssessmentCLOMapping, Long> {
}
