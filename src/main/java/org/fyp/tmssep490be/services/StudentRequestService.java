package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.*;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface StudentRequestService {

    // Student operations (userId is user_account.id, will be mapped to student.id internally)
    Page<StudentRequestResponseDTO> getMyRequests(Long userId, RequestFilterDTO filter);
    StudentRequestDetailDTO getRequestById(Long requestId, Long userId);
    StudentRequestResponseDTO submitAbsenceRequest(Long userId, AbsenceRequestDTO dto);
    StudentRequestResponseDTO cancelRequest(Long requestId, Long userId);
    List<SessionAvailabilityDTO> getAvailableSessionsForDate(Long userId, LocalDate date, StudentRequestType requestType);

    // Academic Affairs operations
    Page<AARequestResponseDTO> getPendingRequests(AARequestFilterDTO filter);
    Page<AARequestResponseDTO> getAllRequests(AARequestFilterDTO filter);
    StudentRequestDetailDTO getRequestDetailsForAA(Long requestId);
    StudentRequestResponseDTO approveRequest(Long requestId, Long decidedById, ApprovalDTO dto);
    StudentRequestResponseDTO rejectRequest(Long requestId, Long decidedById, RejectionDTO dto);
    RequestSummaryDTO getRequestSummary(AARequestFilterDTO filter);

    // Support methods
    boolean hasDuplicateRequest(Long studentId, Long sessionId, StudentRequestType requestType);
    double calculateAbsenceRate(Long studentId, Long classId);
}
