package com.eshop.app.user.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Return response holding information about the submitted export job. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportJobResponse {
    private String jobId;
    private String statusUrl;
}
