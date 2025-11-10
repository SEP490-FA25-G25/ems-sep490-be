package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.teacherrequest.*;
import org.fyp.tmssep490be.dtos.teacherrequest.SwapCandidateDTO;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.TeacherRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for Teacher Request management
 * Supports: SWAP, RESCHEDULE, MODALITY_CHANGE
 */
@RestController
@RequestMapping("/api/v1/teacher-requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Teacher Request", description = "Teacher request management APIs (Swap, Reschedule, Modality Change)")
@SecurityRequirement(name = "Bearer Authentication")
public class TeacherRequestController {

    private final TeacherRequestService teacherRequestService;

    /**
     * Create a new teacher request
     * POST /api/v1/teacher-requests
     */
    @PostMapping
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Create teacher request",
            description = "Create a new request (SWAP, RESCHEDULE, or MODALITY_CHANGE)"
    )
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> createRequest(
            @RequestBody @Valid TeacherRequestCreateDTO createDTO,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Create request type {} for session {} by user {}", 
                createDTO.getRequestType(), createDTO.getSessionId(), currentUser.getId());

        TeacherRequestResponseDTO response = teacherRequestService.createRequest(createDTO, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Request created successfully")
                .data(response)
                .build());
    }

    /**
     * Suggest time slots (RESCHEDULE) for a session on a selected date
     */
    @GetMapping("/{sessionId}/reschedule/slots")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Suggest time slots for reschedule", description = "List time slots without conflicts on the selected date")
    public ResponseEntity<ResponseObject<List<RescheduleSlotSuggestionDTO>>> suggestSlots(
            @PathVariable Long sessionId,
            @RequestParam("date") java.time.LocalDate date,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        List<RescheduleSlotSuggestionDTO> items = teacherRequestService.suggestSlots(sessionId, date, currentUser.getId());
        return ResponseEntity.ok(ResponseObject.<List<RescheduleSlotSuggestionDTO>>builder()
                .success(true)
                .message(items.isEmpty() ? "No suitable time slots for the selected date" : "OK")
                .data(items)
                .build());
    }

    /**
     * Suggest resources (RESCHEDULE) for a session with selected date and time slot
     */
    @GetMapping("/{sessionId}/reschedule/suggestions")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Suggest resources for reschedule", description = "List resources without conflicts for date + time slot")
    public ResponseEntity<ResponseObject<List<RescheduleResourceSuggestionDTO>>> suggestResources(
            @PathVariable Long sessionId,
            @RequestParam("date") java.time.LocalDate date,
            @RequestParam("timeSlotId") Long timeSlotId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        List<RescheduleResourceSuggestionDTO> items = teacherRequestService.suggestResources(sessionId, date, timeSlotId, currentUser.getId());
        return ResponseEntity.ok(ResponseObject.<List<RescheduleResourceSuggestionDTO>>builder()
                .success(true)
                .message(items.isEmpty() ? "No suitable resources for the selected slot" : "OK")
                .data(items)
                .build());
    }

    /**
     * Get all requests for current teacher
     * GET /api/v1/teacher-requests/me
     */
    @GetMapping("/me")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Get my requests",
            description = "Get all requests for the current teacher"
    )
    public ResponseEntity<ResponseObject<List<TeacherRequestListDTO>>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Get requests for user {}", currentUser.getId());

        List<TeacherRequestListDTO> requests = teacherRequestService.getMyRequests(currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<List<TeacherRequestListDTO>>builder()
                .success(true)
                .message("Requests loaded successfully")
                .data(requests)
                .build());
    }

    /**
     * Get teacher's future sessions for request creation
     * GET /api/v1/teacher-requests/my-sessions?date=2025-11-10 (optional)
     * NOTE: Must be placed BEFORE /{id} endpoint to avoid path matching conflict
     */
    @GetMapping("/my-sessions")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Get my future sessions",
            description = "Get list of teacher's future sessions. " +
                    "If date parameter is provided, returns sessions for that specific date. " +
                    "Otherwise, returns sessions for the next 7 days."
    )
    public ResponseEntity<ResponseObject<List<org.fyp.tmssep490be.dtos.teacherrequest.TeacherSessionDTO>>> getMyFutureSessions(
            @RequestParam(value = "date", required = false) java.time.LocalDate date,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Get future sessions for user {}, filter date: {}", currentUser.getId(), date);

        List<org.fyp.tmssep490be.dtos.teacherrequest.TeacherSessionDTO> sessions = 
                teacherRequestService.getMyFutureSessions(currentUser.getId(), date);

        String message = date != null 
                ? String.format("Sessions for %s loaded successfully", date)
                : "Sessions for next 7 days loaded successfully";

        return ResponseEntity.ok(ResponseObject.<List<org.fyp.tmssep490be.dtos.teacherrequest.TeacherSessionDTO>>builder()
                .success(true)
                .message(message)
                .data(sessions)
                .build());
    }

    /**
     * Get teacher request by ID
     * GET /api/v1/teacher-requests/{id}
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('TEACHER') or hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Get request by ID",
            description = "Get request details (Teacher sees own requests, Staff sees all)"
    )
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> getRequestById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Get request {} for user {}", id, currentUser.getId());

        TeacherRequestResponseDTO response = teacherRequestService.getRequestById(id, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Request loaded successfully")
                .data(response)
                .build());
    }

    /**
     * Approve teacher request (Staff only)
     * PATCH /api/v1/teacher-requests/{id}/approve
     */
    @PatchMapping("/{id}/approve")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Approve teacher request",
            description = "Approve a request. Staff can override Teacher's choices."
    )
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> approveRequest(
            @PathVariable Long id,
            @RequestBody @Valid TeacherRequestApproveDTO approveDTO,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Approve request {} by user {}", id, currentUser.getId());

        TeacherRequestResponseDTO response = teacherRequestService.approveRequest(id, approveDTO, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Request approved successfully")
                .data(response)
                .build());
    }

    /**
     * Reject teacher request (Staff only)
     * PATCH /api/v1/teacher-requests/{id}/reject
     */
    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Reject teacher request",
            description = "Reject a request with reason"
    )
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> rejectRequest(
            @PathVariable Long id,
            @RequestBody @Valid TeacherRequestRejectDTO rejectDTO,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Reject request {} by user {}", id, currentUser.getId());

        TeacherRequestResponseDTO response = teacherRequestService.rejectRequest(
                id, rejectDTO.getReason(), currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Request rejected")
                .data(response)
                .build());
    }

    /**
     * Suggest swap candidates (SWAP) for a session
     */
    @GetMapping("/{sessionId}/swap/candidates")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(summary = "Suggest swap candidates", description = "List teachers who can replace a session")
    public ResponseEntity<ResponseObject<List<SwapCandidateDTO>>> suggestSwapCandidates(
            @PathVariable Long sessionId,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        List<SwapCandidateDTO> items = teacherRequestService.suggestSwapCandidates(sessionId, currentUser.getId());
        return ResponseEntity.ok(ResponseObject.<List<SwapCandidateDTO>>builder()
                .success(true)
                .message(items.isEmpty() ? "No suitable teachers found" : "OK")
                .data(items)
                .build());
    }

    /**
     * Confirm swap request (Replacement Teacher)
     * PATCH /api/v1/teacher-requests/{id}/confirm
     */
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Confirm swap request",
            description = "Replacement teacher confirms they will teach the session"
    )
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> confirmSwap(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Confirm swap request {} by user {}", id, currentUser.getId());

        TeacherRequestResponseDTO response = teacherRequestService.confirmSwap(id, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Swap request confirmed successfully")
                .data(response)
                .build());
    }

    /**
     * Decline swap request (Replacement Teacher)
     * PATCH /api/v1/teacher-requests/{id}/decline
     */
    @PatchMapping("/{id}/decline")
    @PreAuthorize("hasRole('TEACHER')")
    @Operation(
            summary = "Decline swap request",
            description = "Replacement teacher declines the swap request"
    )
    public ResponseEntity<ResponseObject<TeacherRequestResponseDTO>> declineSwap(
            @PathVariable Long id,
            @RequestBody(required = false) java.util.Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Decline swap request {} by user {}", id, currentUser.getId());

        String reason = body != null ? body.get("reason") : null;
        TeacherRequestResponseDTO response = teacherRequestService.declineSwap(id, reason, currentUser.getId());

        return ResponseEntity.ok(ResponseObject.<TeacherRequestResponseDTO>builder()
                .success(true)
                .message("Swap request declined")
                .data(response)
                .build());
    }
}



