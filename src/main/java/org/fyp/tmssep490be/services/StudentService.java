package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.studentmanagement.*;
import org.fyp.tmssep490be.entities.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StudentService {

    /**
     * Get students in accessible branches with filters
     */
    Page<StudentListItemDTO> getStudents(
            List<Long> branchIds,
            String search,
            UserStatus status,
            Long courseId,
            Pageable pageable,
            Long userId
    );

    /**
     * Get detailed information about a specific student
     */
    StudentDetailDTO getStudentDetail(Long studentId, Long userId);

    /**
     * Get student enrollment history
     */
    Page<StudentEnrollmentHistoryDTO> getStudentEnrollmentHistory(
            Long studentId,
            List<Long> branchIds,
            Pageable pageable,
            Long userId
    );
}
