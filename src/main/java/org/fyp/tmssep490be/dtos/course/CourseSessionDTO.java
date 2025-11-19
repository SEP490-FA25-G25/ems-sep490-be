package org.fyp.tmssep490be.dtos.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseSessionDTO {
    private Long id;
    private Integer sequenceNo;
    private String topic;
    private String description;
    private String objectives;
    private List<String> skillSets;
    private List<CourseMaterialDTO> materials;
    private Integer totalMaterials;
    private Boolean isCompleted;
}