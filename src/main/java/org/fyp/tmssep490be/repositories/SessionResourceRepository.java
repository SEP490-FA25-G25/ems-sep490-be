package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.SessionResource;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SessionResourceRepository extends JpaRepository<SessionResource, Long> {
    
    /**
     * Check if resource is booked at a specific date and time slot
     * Used for validation before approving MODALITY_CHANGE or RESCHEDULE
     * @param excludeSessionId Session ID to exclude from check (when updating existing session)
     */
    @Query("SELECT COUNT(sr) > 0 FROM SessionResource sr " +
           "JOIN sr.session s " +
           "WHERE sr.resource.id = :resourceId " +
           "AND s.date = :date " +
           "AND s.timeSlotTemplate.id = :timeSlotTemplateId " +
           "AND s.status IN :statuses " +
           "AND (:excludeSessionId IS NULL OR s.id != :excludeSessionId)")
    boolean existsByResourceIdAndDateAndTimeSlotAndStatusIn(
            @Param("resourceId") Long resourceId,
            @Param("date") LocalDate date,
            @Param("timeSlotTemplateId") Long timeSlotTemplateId,
            @Param("statuses") List<SessionStatus> statuses,
            @Param("excludeSessionId") Long excludeSessionId
    );
    
    /**
     * Find all session resources for a session
     */
    @Query("SELECT sr FROM SessionResource sr WHERE sr.session.id = :sessionId")
    List<SessionResource> findBySessionId(@Param("sessionId") Long sessionId);
    
    /**
     * Delete all session resources for a session
     */
    @Modifying
    @Query("DELETE FROM SessionResource sr WHERE sr.session.id = :sessionId")
    void deleteBySessionId(@Param("sessionId") Long sessionId);
}
