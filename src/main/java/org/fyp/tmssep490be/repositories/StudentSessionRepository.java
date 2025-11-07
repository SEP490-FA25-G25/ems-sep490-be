package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface StudentSessionRepository extends JpaRepository<StudentSession, StudentSession.StudentSessionId> {

    /**
     * Find weekly schedule for a student with all related data
     * Uses JOIN FETCH to prevent N+1 queries
     */
    @Query("SELECT ss FROM StudentSession ss " +
           "JOIN FETCH ss.session s " +
           "JOIN FETCH s.timeSlotTemplate tst " +
           "JOIN FETCH s.classEntity c " +
           "JOIN FETCH c.course course " +
           "JOIN FETCH c.branch branch " +
           "JOIN FETCH s.courseSession cs " +
           "LEFT JOIN FETCH cs.courseMaterials " +
           "LEFT JOIN FETCH s.sessionResources sr " +
           "LEFT JOIN FETCH sr.resource " +
           "WHERE ss.student.id = :studentId " +
           "AND s.date BETWEEN :startDate AND :endDate " +
           "ORDER BY s.date ASC, tst.startTime ASC")
    List<StudentSession> findWeeklyScheduleByStudentId(
            @Param("studentId") Long studentId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * Find specific session for a student with authorization check
     * Ensures student can only access their own sessions
     */
    @Query("SELECT ss FROM StudentSession ss " +
           "JOIN FETCH ss.session s " +
           "JOIN FETCH s.timeSlotTemplate tst " +
           "JOIN FETCH s.classEntity c " +
           "JOIN FETCH c.course course " +
           "JOIN FETCH c.branch branch " +
           "JOIN FETCH s.courseSession cs " +
           "LEFT JOIN FETCH cs.courseMaterials " +
           "LEFT JOIN FETCH s.sessionResources sr " +
           "LEFT JOIN FETCH sr.resource " +
           "LEFT JOIN FETCH s.teachingSlots ts " +
           "LEFT JOIN FETCH ts.teacher " +
           "WHERE ss.student.id = :studentId " +
           "AND ss.session.id = :sessionId")
    Optional<StudentSession> findByStudentIdAndSessionId(
            @Param("studentId") Long studentId,
            @Param("sessionId") Long sessionId
    );

    /**
     * Check if student is enrolled in a specific session
     */
    boolean existsByStudentIdAndSessionId(Long studentId, Long sessionId);
}
