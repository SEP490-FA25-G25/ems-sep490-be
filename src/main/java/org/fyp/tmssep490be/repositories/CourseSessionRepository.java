package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.CourseSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CourseSessionRepository extends JpaRepository<CourseSession, Long> {

    @Query("SELECT cs FROM CourseSession cs WHERE cs.phase.id = :phaseId ORDER BY cs.sequenceNo")
    List<CourseSession> findByPhaseIdOrderBySequenceNo(@Param("phaseId") Long phaseId);

    @Query("SELECT cs FROM CourseSession cs WHERE cs.phase.course.id = :courseId ORDER BY cs.phase.id, cs.sequenceNo")
    List<CourseSession> findByCourseIdOrderByPhaseIdAndSequenceNo(@Param("courseId") Long courseId);

    @Query("SELECT COUNT(cs) FROM CourseSession cs WHERE cs.phase.course.id = :courseId")
    int countByCourseId(@Param("courseId") Long courseId);
}
