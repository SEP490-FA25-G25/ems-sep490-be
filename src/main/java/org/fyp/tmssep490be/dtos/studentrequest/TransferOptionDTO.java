package org.fyp.tmssep490be.dtos.studentrequest;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO for transfer class options
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransferOptionDTO {

    private Long classId;
    private String classCode;
    private String className;
    private String courseName;
    private String branchName;
    private String learningMode;
    private String scheduleInfo;
    private String instructorName;
    private int currentEnrollment;
    private int maxCapacity;
    private int availableSlots;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
    private ContentGapAnalysis contentGapAnalysis;
    private boolean canTransfer;
    private Changes changes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContentGapAnalysis {
        private String gapLevel; // NONE, MINOR, MODERATE, MAJOR
        private int missedSessions;
        private int totalSessions;
        private List<GapSessionInfo> gapSessions;
        private List<String> recommendedActions;
        private String impactDescription;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GapSessionInfo {
        private Integer courseSessionNumber;
        private String courseSessionTitle;
        private LocalDate scheduledDate;
    }

    /**
     * Summary of changes between current and target class
     * Used by AA to explain transfer implications to student
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Changes {
        private String branch;      // "Central → North" or "No change"
        private String modality;    // "OFFLINE → ONLINE" or "No change"
        private String schedule;    // "Mon/Wed/Fri 08:00 → Tue/Thu/Sat 18:00" or "No change"
    }
}