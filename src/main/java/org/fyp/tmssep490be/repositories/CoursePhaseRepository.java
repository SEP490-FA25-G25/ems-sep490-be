package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CoursePhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CoursePhaseRepository extends JpaRepository<CoursePhase, Long> {
}
