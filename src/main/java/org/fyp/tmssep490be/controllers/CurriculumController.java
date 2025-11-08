package org.fyp.tmssep490be.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.fyp.tmssep490be.dtos.common.ResponseObject;
import org.fyp.tmssep490be.dtos.curriculum.SubjectWithLevelsDTO;
import org.fyp.tmssep490be.services.CurriculumService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller for curriculum-related operations
 * Provides subject and level information for dropdown/select components
 */
@RestController
@RequestMapping("/api/v1/curriculum")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Curriculum Management", description = "Curriculum APIs for subjects and levels")
@SecurityRequirement(name = "bearerAuth")
public class CurriculumController {

    private final CurriculumService curriculumService;

    /**
     * Get all subjects with their levels
     * Used for dropdown/select components when creating student skill assessments
     */
    @GetMapping("/subjects-with-levels")
    @Operation(
            summary = "Get all subjects with their levels",
            description = "Retrieve list of subjects with their levels. Used for selecting levels in student skill assessments."
    )
    @PreAuthorize("hasAnyRole('ACADEMIC_AFFAIR', 'CENTER_HEAD', 'MANAGER')")
    public ResponseEntity<ResponseObject<List<SubjectWithLevelsDTO>>> getAllSubjectsWithLevels() {
        log.info("Fetching all subjects with their levels");

        List<SubjectWithLevelsDTO> subjects = curriculumService.getAllSubjectsWithLevels();

        log.info("Successfully retrieved {} subjects with levels", subjects.size());

        return ResponseEntity.ok(ResponseObject.<List<SubjectWithLevelsDTO>>builder()
                .success(true)
                .message("Subjects with levels retrieved successfully")
                .data(subjects)
                .build());
    }
}