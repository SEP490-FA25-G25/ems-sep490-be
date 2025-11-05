package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachingSlotRepository extends JpaRepository<TeachingSlot, TeachingSlot.TeachingSlotId> {

    @Query("SELECT ts FROM TeachingSlot ts WHERE ts.session.classEntity.id = :classId AND ts.status = :status")
    List<TeachingSlot> findByClassEntityIdAndStatus(@Param("classId") Long classId, @Param("status") TeachingSlotStatus status);
    
    /**
     * Check if teacher owns (is assigned to) a session
     * Teacher owns session if there's a teaching_slot with status SCHEDULED or SUBSTITUTED
     */
    boolean existsByIdSessionIdAndIdTeacherIdAndStatusIn(
            Long sessionId,
            Long teacherId,
            List<TeachingSlotStatus> statuses
    );
}
