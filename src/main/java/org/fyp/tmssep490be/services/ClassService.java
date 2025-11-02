package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.classmanagement.*;
import org.fyp.tmssep490be.entities.enums.Modality;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for class management operations
 * Provides business logic for Academic Affairs staff to view and manage classes
 */
public interface ClassService {

    /**
     * Get list of classes accessible to academic affairs user
     * Filters by user's branch assignments and applies search/filter criteria
     *
     * @param branchIds List of branch IDs user has access to
     * @param courseId Optional course filter
     * @param modality Optional modality filter
     * @param search Optional search term (class code, name, course name, branch name)
     * @param pageable Pagination parameters
     * @param userId Current user ID for access control
     * @return Page of ClassListItemDTO
     */
    Page<ClassListItemDTO> getClasses(
            List<Long> branchIds,
            Long courseId,
            Modality modality,
            String search,
            Pageable pageable,
            Long userId
    );

    /**
     * Get detailed information about a specific class
     * Includes enrollment summary and upcoming sessions
     *
     * @param classId Class ID to retrieve
     * @param userId Current user ID for access control
     * @return ClassDetailDTO with comprehensive class information
     */
    ClassDetailDTO getClassDetail(Long classId, Long userId);

    /**
     * Get list of students currently enrolled in a class
     * Supports search and pagination
     *
     * @param classId Class ID
     * @param search Optional search term (student code, name, email, phone)
     * @param pageable Pagination parameters
     * @param userId Current user ID for access control
     * @return Page of ClassStudentDTO
     */
    Page<ClassStudentDTO> getClassStudents(
            Long classId,
            String search,
            Pageable pageable,
            Long userId
    );

    /**
     * Get quick enrollment summary for a class
     * Lightweight endpoint for capacity checks and list views
     *
     * @param classId Class ID
     * @param userId Current user ID for access control
     * @return ClassEnrollmentSummaryDTO with capacity information
     */
    ClassEnrollmentSummaryDTO getClassEnrollmentSummary(Long classId, Long userId);

    /**
     * Get available students for enrollment in a class
     * Returns students from the same branch who are not yet enrolled
     * Smart sorting based on skill assessment matching:
     * - Priority 1: Students with assessment matching both Subject AND Level
     * - Priority 2: Students with assessment matching Subject only
     * - Priority 3: Students with no assessment or different Subject
     *
     * @param classId Class ID to enroll students into
     * @param search Optional search term (student code, name, email, phone)
     * @param pageable Pagination parameters (supports sorting)
     * @param userId Current user ID for access control
     * @return Page of AvailableStudentDTO with match priority
     */
    Page<AvailableStudentDTO> getAvailableStudentsForClass(
            Long classId,
            String search,
            Pageable pageable,
            Long userId
    );
}
