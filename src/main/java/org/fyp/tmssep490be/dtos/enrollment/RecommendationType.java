package org.fyp.tmssep490be.dtos.enrollment;

/**
 * Recommendation type cho Academic Affair
 */
public enum RecommendationType {
    /**
     * Capacity đủ, enroll hết
     */
    OK,

    /**
     * Vượt capacity, suggest enroll một phần
     */
    PARTIAL_SUGGESTED,

    /**
     * Vượt capacity nhưng <= 20%, có thể override
     */
    OVERRIDE_AVAILABLE,

    /**
     * Vượt quá nhiều, không nên enroll
     */
    BLOCKED
}
