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

    /**
     * Find teaching slots by teacher ID, date range, and session status
     */
    @Query("SELECT ts FROM TeachingSlot ts " +
           "JOIN FETCH ts.session s " +
           "JOIN FETCH s.timeSlotTemplate " +
           "JOIN FETCH s.classEntity c " +
           "JOIN FETCH c.course " +
           "LEFT JOIN FETCH s.courseSession " +
           "WHERE ts.teacher.id = :teacherId " +
           "AND ts.status IN :statuses " +
           "AND s.status = :sessionStatus " +
           "AND s.date >= :startDate " +
           "AND s.date <= :endDate " +
           "ORDER BY s.date ASC, s.timeSlotTemplate.startTime ASC")
    List<TeachingSlot> findByTeacherIdAndDateRange(
            @Param("teacherId") Long teacherId,
            @Param("statuses") List<TeachingSlotStatus> statuses,
            @Param("sessionStatus") org.fyp.tmssep490be.entities.enums.SessionStatus sessionStatus,
            @Param("startDate") java.time.LocalDate startDate,
            @Param("endDate") java.time.LocalDate endDate
    );

    /**
     * Find all teaching slots for a teacher on a specific date
     * Includes all sessions (even those not started yet)
     */
    @Query("SELECT ts FROM TeachingSlot ts " +
           "JOIN FETCH ts.session s " +
           "JOIN FETCH s.timeSlotTemplate tst " +
           "JOIN FETCH s.classEntity c " +
           "JOIN FETCH c.course co " +
           "LEFT JOIN FETCH s.courseSession cs " +
           "WHERE ts.teacher.id = :teacherId " +
           "AND ts.status IN ('SCHEDULED', 'SUBSTITUTED') " +
           "AND s.date = :date " +
           "AND s.status != 'CANCELLED' " +
           "ORDER BY tst.startTime ASC")
    List<TeachingSlot> findByTeacherIdAndDate(
            @Param("teacherId") Long teacherId,
            @Param("date") java.time.LocalDate date
    );
}
