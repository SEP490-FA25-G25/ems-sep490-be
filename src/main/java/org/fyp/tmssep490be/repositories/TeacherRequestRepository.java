package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.TeacherRequest;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.TeacherRequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeacherRequestRepository extends JpaRepository<TeacherRequest, Long> {
    
    /**
     * Find all requests by teacher ID, ordered by submitted date descending
     */
    List<TeacherRequest> findByTeacherIdOrderBySubmittedAtDesc(Long teacherId);
    
    /**
     * Check if a pending request exists for the same session and request type
     * Used to prevent duplicate requests
     */
    boolean existsBySessionIdAndRequestTypeAndStatus(
            Long sessionId, 
            TeacherRequestType requestType, 
            RequestStatus status
    );
    
    /**
     * Find pending request by session ID and request type
     */
    Optional<TeacherRequest> findBySessionIdAndRequestTypeAndStatus(
            Long sessionId,
            TeacherRequestType requestType,
            RequestStatus status
    );
    
    /**
     * Find request by ID with teacher, replacement teacher, and session loaded
     */
    @Query("SELECT tr FROM TeacherRequest tr " +
           "LEFT JOIN FETCH tr.teacher t " +
           "LEFT JOIN FETCH tr.replacementTeacher rt " +
           "LEFT JOIN FETCH tr.session s " +
           "WHERE tr.id = :id")
    Optional<TeacherRequest> findByIdWithTeacherAndSession(@Param("id") Long id);
    
    /**
     * Find requests by teacher ID and status
     */
    List<TeacherRequest> findByTeacherIdAndStatusOrderBySubmittedAtDesc(
            Long teacherId, 
            RequestStatus status
    );

    /**
     * Find requests by session IDs and statuses
     */
    List<TeacherRequest> findBySessionIdInAndStatusIn(
            List<Long> sessionIds,
            List<RequestStatus> statuses
    );

    /**
     * Find requests where teacher is the replacement teacher
     * Used for replacement teacher to see requests waiting for their confirmation
     */
    List<TeacherRequest> findByReplacementTeacherIdOrderBySubmittedAtDesc(Long replacementTeacherId);
}
