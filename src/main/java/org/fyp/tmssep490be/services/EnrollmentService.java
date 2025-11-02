package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.enrollment.ClassEnrollmentImportExecuteRequest;
import org.fyp.tmssep490be.dtos.enrollment.ClassEnrollmentImportPreview;
import org.fyp.tmssep490be.dtos.enrollment.EnrollmentResult;
import org.springframework.web.multipart.MultipartFile;

public interface EnrollmentService {
    /**
     * Preview import Excel cho class enrollment
     * Parse Excel, resolve students, calculate capacity, determine recommendation
     *
     * @param classId Class ID to enroll students into
     * @param file Excel file với student data
     * @param enrolledBy User ID của Academic Affair (for audit)
     * @return Preview với student resolution status và capacity info
     */
    ClassEnrollmentImportPreview previewClassEnrollmentImport(
            Long classId,
            MultipartFile file,
            Long enrolledBy
    );

    /**
     * Execute enrollment sau khi preview và confirm
     * Create enrollments, auto-generate student_sessions, send emails
     *
     * @param request Execute request với strategy và students
     * @param enrolledBy User ID của Academic Affair (for audit)
     * @return Enrollment result với counts
     */
    EnrollmentResult executeClassEnrollmentImport(
            ClassEnrollmentImportExecuteRequest request,
            Long enrolledBy
    );
}
