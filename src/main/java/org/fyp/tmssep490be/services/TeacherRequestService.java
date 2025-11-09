package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.teacherrequest.*;

import java.util.List;

public interface TeacherRequestService {
    
    /**
     * Create a new teacher request
     * @param createDTO Request data
     * @param userId Current authenticated user ID
     * @return Created teacher request
     */
    TeacherRequestResponseDTO createRequest(TeacherRequestCreateDTO createDTO, Long userId);
    
    /**
     * Get all requests for the current teacher
     * @param userId Current authenticated user ID
     * @return List of teacher requests
     */
    List<TeacherRequestListDTO> getMyRequests(Long userId);
    
    /**
     * Get teacher request by ID (for detail view)
     * @param requestId Request ID
     * @param userId Current authenticated user ID (for authorization check)
     * @return Teacher request details
     */
    TeacherRequestResponseDTO getRequestById(Long requestId, Long userId);
    
    /**
     * Approve a teacher request (Staff only)
     * For MODALITY_CHANGE: updates session_resource and class.modality
     * @param requestId Request ID
     * @param approveDTO Approval data (can override Teacher's choices)
     * @param userId Current authenticated staff user ID
     * @return Approved teacher request
     */
    TeacherRequestResponseDTO approveRequest(Long requestId, TeacherRequestApproveDTO approveDTO, Long userId);
    
    /**
     * Reject a teacher request (Staff only)
     * @param requestId Request ID
     * @param reason Rejection reason
     * @param userId Current authenticated staff user ID
     * @return Rejected teacher request
     */
    TeacherRequestResponseDTO rejectRequest(Long requestId, String reason, Long userId);

    /**
     * Suggest valid time slots for rescheduling a session on a given date
     */
    List<RescheduleSlotSuggestionDTO> suggestSlots(Long sessionId, java.time.LocalDate date, Long userId);

    /**
     * Suggest valid resources for rescheduling with given date and time slot
     */
    List<RescheduleResourceSuggestionDTO> suggestResources(Long sessionId, java.time.LocalDate date, Long timeSlotId, Long userId);
}
