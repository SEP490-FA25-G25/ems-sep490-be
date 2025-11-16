package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeachingSlot;
import org.fyp.tmssep490be.entities.enums.TeachingSlotStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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

    @Query("""
            SELECT ts FROM TeachingSlot ts
            JOIN FETCH ts.session s
            JOIN FETCH s.timeSlotTemplate tst
            JOIN FETCH s.classEntity c
            JOIN FETCH c.course course
            LEFT JOIN FETCH s.courseSession cs
            WHERE ts.teacher.id = :teacherId
              AND ts.status IN ('SCHEDULED', 'SUBSTITUTED')
              AND s.date = :date
              AND s.status <> 'CANCELLED'
            ORDER BY tst.startTime ASC
            """)
    List<TeachingSlot> findByTeacherIdAndDate(
            @Param("teacherId") Long teacherId,
            @Param("date") LocalDate date
    );

    /**
     * Find teacher's future sessions within date range
     * Returns sessions with status PLANNED, within 7 days from today (or specific date range)
     */
    @Query("""
            SELECT ts FROM TeachingSlot ts
            JOIN FETCH ts.session s
            JOIN FETCH s.timeSlotTemplate tst
            JOIN FETCH s.classEntity c
            JOIN FETCH c.course course
            LEFT JOIN FETCH s.courseSession cs
            WHERE ts.teacher.id = :teacherId
              AND ts.status IN ('SCHEDULED', 'SUBSTITUTED')
              AND s.status = 'PLANNED'
              AND s.date >= :fromDate
              AND s.date <= :toDate
            ORDER BY s.date ASC, tst.startTime ASC
            """)
    List<TeachingSlot> findByTeacherIdAndDateRange(
            @Param("teacherId") Long teacherId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /**
     * Find teaching slot by session ID with teacher loaded
     * Used to get teacher from session when request.teacher is null
     */
    @Query("""
            SELECT ts FROM TeachingSlot ts
            JOIN FETCH ts.teacher t
            JOIN FETCH t.userAccount ua
            WHERE ts.session.id = :sessionId
              AND ts.status IN ('SCHEDULED', 'SUBSTITUTED')
            """)
    List<TeachingSlot> findBySessionIdWithTeacher(@Param("sessionId") Long sessionId);
}
