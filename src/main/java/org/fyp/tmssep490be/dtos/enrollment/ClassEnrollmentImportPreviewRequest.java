package org.fyp.tmssep490be.dtos.enrollment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request để preview import Excel
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassEnrollmentImportPreviewRequest {
    @NotNull(message = "Class ID is required")
    private Long classId;

    @NotNull(message = "Excel file is required")
    private MultipartFile file;  // Excel file
}
