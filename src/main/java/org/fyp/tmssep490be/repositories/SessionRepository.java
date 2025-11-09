package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.Session;
import org.fyp.tmssep490be.entities.enums.SessionStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {
    /**
     * Tìm tất cả future sessions của class (date >= today, status = PLANNED)
     * Dùng để auto-generate student_session khi enroll
     */
    List<Session> findByClassEntityIdAndDateGreaterThanEqualAndStatusOrderByDateAsc(
            Long classId,
            LocalDate date,
            SessionStatus status
    );

    /**
     * Get upcoming sessions for a class (next sessions from today)
     */
    @Query("SELECT s FROM Session s WHERE s.classEntity.id = :classId " +
           "AND s.date >= CURRENT_DATE AND s.status = 'PLANNED' " +
           "ORDER BY s.date ASC")
    List<Session> findUpcomingSessions(@Param("classId") Long classId, Pageable pageable);

    /**
     * Find sessions for a specific student on a given date
     */
    @Query("SELECT s FROM Session s " +
           "JOIN s.classEntity c " +
           "JOIN Enrollment e ON e.classId = c.id " +
           "WHERE e.studentId = :studentId " +
           "AND s.date = :date " +
           "AND s.status = 'PLANNED' " +
           "AND e.status = 'ENROLLED'")
    List<Session> findSessionsForStudentByDate(@Param("studentId") Long studentId, @Param("date") LocalDate date);

    /**
     * Find session by date and class
     */
    List<Session> findByClassEntityIdAndDate(Long classId, LocalDate date);
}
