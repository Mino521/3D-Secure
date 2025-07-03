package com.jamiewang.secure3d.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class BulkImportResponseDTO {

    private int totalProcessed;
    private int successCount;
    private int errorCount;
    private List<String> errors;
    private LocalDateTime processedAt;

}
