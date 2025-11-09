package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentrequest.*;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.springframework.data.domain.Page;

import java.time.LocalDate;
import java.util.List;

public interface StudentRequestService {

    // Student operations
    Page<StudentRequestResponseDTO> getMyRequests(Long studentId, RequestFilterDTO filter);
    StudentRequestDetailDTO getRequestById(Long requestId, Long studentId);
    StudentRequestResponseDTO submitAbsenceRequest(Long studentId, AbsenceRequestDTO dto);
    StudentRequestResponseDTO cancelRequest(Long requestId, Long studentId);
    List<SessionAvailabilityDTO> getAvailableSessionsForDate(Long studentId, LocalDate date, StudentRequestType requestType);

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
