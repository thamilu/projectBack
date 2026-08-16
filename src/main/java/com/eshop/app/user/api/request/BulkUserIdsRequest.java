package com.eshop.app.user.api.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload containing list of user IDs for bulk actions. Enforces deep list element
 * constraints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUserIdsRequest {

    @NotEmpty(message = "At least one user ID is required")
    @Size(max = 100, message = "Maximum 100 user IDs per request")
    private List<@Positive(message = "User ID must be positive") Long> userIds;
}
