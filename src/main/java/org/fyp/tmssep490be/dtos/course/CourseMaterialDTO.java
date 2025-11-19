package org.fyp.tmssep490be.dtos.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseMaterialDTO {
    private Long id;
    private String title;
    private String description;
    private String materialType;
    private String fileName;
    private String filePath;
    private String fileUrl;
    private Long fileSize;
    private String level; // COURSE, PHASE, SESSION
    private Long phaseId;
    private Long sessionId;
    private Integer sequenceNo;
    private Boolean isAccessible;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}