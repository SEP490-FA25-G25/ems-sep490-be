package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.studentrequest.*;
import org.fyp.tmssep490be.entities.enums.StudentRequestType;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.StudentRequestService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/students/me")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Student Request Management", description = "APIs for students to manage their absence/makeup/transfer requests")
@SecurityRequirement(name = "Bearer Authentication")
public class StudentRequestController {

    private final StudentRequestService studentRequestService;

    @GetMapping("/requests")
    @Operation(summary = "Get student's requests", description = "Retrieve all requests submitted by the current student with pagination and filtering")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<Page<StudentRequestResponseDTO>>> getMyRequests(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Filter by request type: ABSENCE, MAKEUP, TRANSFER")
            @RequestParam(required = false) String requestType,
            @Parameter(description = "Filter by status: PENDING, APPROVED, REJECTED, CANCELLED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Page number (0-based)")
            @RequestParam(defaultValue = "0") Integer page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "10") Integer size,
            @Parameter(description = "Sort criteria: field,direction (e.g., submittedAt,desc)")
            @RequestParam(defaultValue = "submittedAt,desc") String sort) {

        // Get student ID from user principal
        Long studentId = currentUser.getId();

        RequestFilterDTO filter = RequestFilterDTO.builder()
                .requestType(requestType)
                .status(status)
                .page(page)
                .size(size)
                .sort(sort)
                .build();

        Page<StudentRequestResponseDTO> requests = studentRequestService.getMyRequests(studentId, filter);

        return ResponseEntity.ok(ResponseObject.success("Retrieved student requests successfully", requests));
    }

    @GetMapping("/requests/{requestId}")
    @Operation(summary = "Get request details", description = "Retrieve detailed information about a specific request")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentRequestDetailDTO>> getRequestById(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Request ID") @PathVariable Long requestId) {

        Long studentId = currentUser.getId();
        StudentRequestDetailDTO request = studentRequestService.getRequestById(requestId, studentId);

        return ResponseEntity.ok(ResponseObject.success("Retrieved request details successfully", request));
    }

    @PostMapping("/requests/{requestId}/cancel")
    @Operation(summary = "Cancel request", description = "Cancel a pending request submitted by the student")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<StudentRequestResponseDTO>> cancelRequest(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Request ID") @PathVariable Long requestId) {

        Long studentId = currentUser.getId();
        StudentRequestResponseDTO request = studentRequestService.cancelRequest(requestId, studentId);

        return ResponseEntity.ok(ResponseObject.success("Request cancelled successfully", request));
    }

    @GetMapping("/classes/sessions")
    @Operation(summary = "Get available sessions for date", description = "Retrieve all class sessions for a specific date (used in Step 2 of absence request flow)")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ResponseObject<List<SessionAvailabilityDTO>>> getAvailableSessionsForDate(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "Date in YYYY-MM-DD format", required = true)
            @RequestParam String date,
            @Parameter(description = "Request type: ABSENCE, MAKEUP, TRANSFER", required = true)
            @RequestParam String requestType) {

        Long studentId = currentUser.getId();
        LocalDate localDate = LocalDate.parse(date);
        StudentRequestType requestTypeEnum = StudentRequestType.valueOf(requestType);

        List<SessionAvailabilityDTO> sessions = studentRequestService.getAvailableSessionsForDate(
                studentId, localDate, requestTypeEnum);

        return ResponseEntity.ok(ResponseObject.success("Retrieved available sessions successfully", sessions));
    }
}