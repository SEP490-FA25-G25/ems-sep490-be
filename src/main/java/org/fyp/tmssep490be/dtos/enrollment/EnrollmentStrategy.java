package org.fyp.tmssep490be.dtos.enrollment;

/**
 * Strategy cho enrollment execution
 */
public enum EnrollmentStrategy {
    /**
     * Enroll tất cả (nếu capacity đủ)
     */
    ALL,

    /**
     * Enroll một phần (selectedStudentIds)
     */
    PARTIAL,

    /**
     * Override capacity và enroll tất cả
     */
    OVERRIDE
}
