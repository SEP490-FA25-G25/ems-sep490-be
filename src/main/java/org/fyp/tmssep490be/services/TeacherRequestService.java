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

    /**
     * Suggest teachers who can replace a session (SWAP)
     */
    List<org.fyp.tmssep490be.dtos.teacherrequest.SwapCandidateDTO> suggestSwapCandidates(Long sessionId, Long userId);

    /**
     * Confirm swap request (Replacement Teacher)
     * Updates teaching slots and sets request status to APPROVED
     */
    TeacherRequestResponseDTO confirmSwap(Long requestId, Long userId);

    /**
     * Decline swap request (Replacement Teacher)
     * Resets request to PENDING and clears replacement teacher
     */
    TeacherRequestResponseDTO declineSwap(Long requestId, String reason, Long userId);

    /**
     * Get teacher's future sessions for request creation
     * Returns sessions in the next 7 days with status PLANNED
     * @param userId Current authenticated user ID
     * @param date Optional date filter. If provided, returns sessions for that specific date only.
     *             If null, returns sessions for the next 7 days
     * @return List of teacher's future sessions
     */
    List<org.fyp.tmssep490be.dtos.teacherrequest.TeacherSessionDTO> getMyFutureSessions(Long userId, java.time.LocalDate date);
}
