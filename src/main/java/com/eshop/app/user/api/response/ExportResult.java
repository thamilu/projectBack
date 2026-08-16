package com.eshop.app.user.api.response;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportResult {
    private String exportId;
    private String downloadUrl;
    private long recordCount;
    private LocalDateTime expiresAt;
}
