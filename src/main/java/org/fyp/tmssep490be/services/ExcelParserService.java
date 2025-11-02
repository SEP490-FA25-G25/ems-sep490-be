package org.fyp.tmssep490be.services;

import org.fyp.tmssep490be.dtos.enrollment.StudentEnrollmentData;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Service để parse Excel file thành student enrollment data
 */
public interface ExcelParserService {
    /**
     * Parse Excel file thành list StudentEnrollmentData
     *
     * @param file Excel file (.xlsx)
     * @return List of student enrollment data parsed from Excel
     * @throws org.fyp.tmssep490be.exceptions.CustomException if file format is invalid
     */
    List<StudentEnrollmentData> parseStudentEnrollment(MultipartFile file);
}
