package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CourseSessionCLOMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseSessionCLOMappingRepository extends JpaRepository<CourseSessionCLOMapping, Long> {
}
