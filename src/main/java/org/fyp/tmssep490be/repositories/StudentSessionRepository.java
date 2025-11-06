package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StudentSessionRepository extends JpaRepository<StudentSession, Long> {
    /**
     * Count số học viên trong một session
     * Bao gồm cả học viên học bù và học viên tham gia buổi đó
     */
    @Query("SELECT COUNT(ss) FROM StudentSession ss WHERE ss.session.id = :sessionId")
    long countBySessionId(@Param("sessionId") Long sessionId);
}
