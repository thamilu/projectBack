package com.eshop.app.media.api.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for setting a media asset as the primary product image. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetPrimaryImageRequest {

    /** The media asset ID to set as primary. */
    @NotNull(message = "Media asset ID is required")
    private UUID mediaAssetId;
}
