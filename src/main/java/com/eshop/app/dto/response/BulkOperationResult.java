package com.eshop.app.dto.response;

import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkOperationResult {
    private int totalProcessed;
    private int successCount;
    private int failedCount;
    private List<Long> failedIds;
}
