package org.fyp.tmssep490be.dtos.enrollment;

/**
 * Status của mỗi student sau khi resolve từ Excel data
 */
public enum StudentResolutionStatus {
    /**
     * Student đã tồn tại trong DB → sẽ enroll
     */
    FOUND,

    /**
     * Student mới → sẽ tạo mới rồi enroll
     */
    CREATE,

    /**
     * Trùng trong file Excel (error)
     */
    DUPLICATE,

    /**
     * Validation lỗi (email invalid, missing fields...)
     */
    ERROR
}
