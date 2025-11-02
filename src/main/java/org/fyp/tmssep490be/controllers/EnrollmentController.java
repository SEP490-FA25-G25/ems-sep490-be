package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.enrollment.ClassEnrollmentImportExecuteRequest;
import org.fyp.tmssep490be.dtos.enrollment.ClassEnrollmentImportPreview;
import org.fyp.tmssep490be.dtos.enrollment.EnrollmentResult;
import org.fyp.tmssep490be.exceptions.CustomException;
import org.fyp.tmssep490be.exceptions.ErrorCode;
import org.fyp.tmssep490be.security.UserPrincipal;
import org.fyp.tmssep490be.services.EnrollmentService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller cho enrollment management
 * Primary workflow: Class-specific student enrollment via Excel import
 */
@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Enrollment", description = "Enrollment management APIs")
@SecurityRequirement(name = "Bearer Authentication")
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    private static final String XLSX_CONTENT_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    /**
     * Preview import Excel cho class enrollment
     * POST /api/v1/enrollments/classes/{classId}/import/preview
     *
     * Workflow:
     * 1. Parse Excel file
     * 2. Resolve students (FOUND/CREATE/ERROR)
     * 3. Calculate capacity
     * 4. Return preview với recommendation
     */
    @PostMapping(value = "/classes/{classId}/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Preview Excel import for class enrollment",
            description = "Parse Excel file, resolve students, calculate capacity, and provide recommendations"
    )
    public ResponseEntity<ResponseObject> previewImport(
            @PathVariable Long classId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Preview import request for class {} by user {}", classId, currentUser.getId());

        // Validate file type
        if (file.isEmpty()) {
            throw new CustomException(ErrorCode.EXCEL_FILE_EMPTY);
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals(XLSX_CONTENT_TYPE)) {
            throw new CustomException(ErrorCode.INVALID_FILE_TYPE_XLSX);
        }

        ClassEnrollmentImportPreview preview = enrollmentService.previewClassEnrollmentImport(
                classId, file, currentUser.getId()
        );

        log.info("Preview completed for class {}. Valid students: {}, Errors: {}",
                classId, preview.getTotalValid(), preview.getErrorCount());

        return ResponseEntity.ok(ResponseObject.builder()
                .success(true)
                .message("Import preview ready")
                .data(preview)
                .build());
    }

    /**
     * Execute import sau khi preview và confirm
     * POST /api/v1/enrollments/classes/{classId}/import/execute
     *
     * Workflow:
     * 1. Lock class (pessimistic)
     * 2. Filter students theo strategy (ALL/PARTIAL/OVERRIDE)
     * 3. Create new students if needed
     * 4. Create enrollment records
     * 5. Auto-generate student_session records
     * 6. Send welcome emails (async) - commented out
     */
    @PostMapping("/classes/{classId}/import/execute")
    @PreAuthorize("hasRole('ACADEMIC_AFFAIR')")
    @Operation(
            summary = "Execute enrollment import",
            description = "Create enrollments and auto-generate student sessions based on strategy"
    )
    public ResponseEntity<ResponseObject> executeImport(
            @PathVariable Long classId,
            @RequestBody @Valid ClassEnrollmentImportExecuteRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser
    ) {
        log.info("Execute import request for class {} with strategy {} by user {}",
                classId, request.getStrategy(), currentUser.getId());

        // Validate classId match
        if (!classId.equals(request.getClassId())) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        EnrollmentResult result = enrollmentService.executeClassEnrollmentImport(
                request,
                currentUser.getId()
        );

        log.info("Enrollment completed for class {}. Enrolled: {}, Created: {}, Total sessions: {}",
                classId, result.getEnrolledCount(), result.getStudentsCreated(),
                result.getTotalStudentSessionsCreated());

        return ResponseEntity.ok(ResponseObject.builder()
                .success(true)
                .message(String.format("Successfully enrolled %d students", result.getEnrolledCount()))
                .data(result)
                .build());
    }
}
