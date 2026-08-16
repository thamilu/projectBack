package com.eshop.app.user.api.request;

import com.eshop.app.user.shared.domain.enums.ExportFormat;
import com.eshop.app.user.shared.domain.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportRequest {
    private ExportFormat format;
    private UserRole role;
    private Boolean active;
}
