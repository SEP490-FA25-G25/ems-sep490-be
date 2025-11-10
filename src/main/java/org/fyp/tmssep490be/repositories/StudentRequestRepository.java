package org.fyp.tmssep490be.repositories;

import org.fyp.tmssep490be.entities.StudentRequest;
import org.fyp.tmssep490be.entities.enums.RequestStatus;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface StudentRequestRepository extends JpaRepository<StudentRequest, Long> {

    // Find requests by student
    Page<StudentRequest> findByStudentIdAndStatusIn(Long studentId, List<RequestStatus> statuses, Pageable pageable);

    // Check for duplicate requests
    boolean existsByStudentIdAndTargetSessionIdAndRequestTypeAndStatusIn(
            Long studentId, Long sessionId, StudentRequestType requestType, List<RequestStatus> statuses);

    // Find pending requests for AA review - simplified query
    @Query("SELECT sr FROM StudentRequest sr WHERE sr.status = :status ORDER BY sr.submittedAt ASC")
    Page<StudentRequest> findPendingRequestsForAA(@Param("status") RequestStatus status, Pageable pageable);

    // Find all requests by status
    Page<StudentRequest> findByStatus(RequestStatus status, Pageable pageable);

    // Find all requests by status with sort only (for in-memory filtering)
    List<StudentRequest> findByStatus(RequestStatus status, org.springframework.data.domain.Sort sort);

    // Find request by student and ID (for student access control)
    boolean existsByIdAndStudentId(Long requestId, Long studentId);

    // Count requests by status for summary
    long countByStatus(RequestStatus status);

    // Count requests by type and status for summary
    long countByRequestTypeAndStatus(StudentRequestType requestType, RequestStatus status);

    // Find all requests by student (for previous requests calculation)
    List<StudentRequest> findByStudentId(Long studentId);

    /**
     * Check if student has existing makeup request for a target session
     * (to prevent duplicate makeup requests for same session)
     */
    boolean existsByStudentIdAndTargetSessionIdAndRequestTypeAndMakeupSessionIdAndStatusIn(
        Long studentId,
        Long targetSessionId,
        StudentRequestType requestType,
        Long makeupSessionId,
        List<RequestStatus> statuses
    );

    /**
     * Check if makeup session already has a pending/approved request
     * (to prevent conflicts when multiple students request same makeup session)
     */
    @Query("SELECT COUNT(sr) > 0 FROM StudentRequest sr " +
           "WHERE sr.makeupSession.id = :makeupSessionId " +
           "AND sr.status IN :statuses")
    boolean existsPendingMakeupForSession(
        @Param("makeupSessionId") Long makeupSessionId,
        @Param("statuses") List<RequestStatus> statuses
    );
}
