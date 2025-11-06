package org.fyp.tmssep490be.services;

/**
 * Service for generating Excel templates for student enrollment
 * Simplified 7-column format: full_name, email, phone, facebook_url, address, gender, dob
 */
public interface EnrollmentTemplateService {

    /**
     * Generate a generic Excel template for student enrollment
     * @return byte array representing the Excel file
     */
    byte[] generateExcelTemplate();

    /**
     * Generate a class-specific Excel template with class information
     * @param classId the ID of the class
     * @return byte array representing the Excel file
     */
    byte[] generateExcelTemplateWithClassInfo(Long classId);
}