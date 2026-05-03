package com.eshop.app.dto.request;

import com.eshop.app.validation.NoHtml;
import com.eshop.app.validation.SafeText;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@Schema(description = "Request payload for creating or updating a category")
public class CategoryRequest {

    public static final int NAME_MIN = 2;
    public static final int NAME_MAX = 100;
    public static final int DESCRIPTION_MAX = 500;
    public static final int REASON_MIN = 10;
    public static final int REASON_MAX = 255;

    // Backwards-compatible fields used by existing services/controllers
    // Prefer `categoryName` in new code; `name` is provided for compatibility.
    private String name;
    private String imageUrl;

    @Schema(description = "Name of the category", example = "Electronics", requiredMode = Schema.RequiredMode.REQUIRED,
            minLength = NAME_MIN, maxLength = NAME_MAX)
    @JsonProperty("category_name")
    @NotBlank(message = "Category name is required")
    @Size(min = NAME_MIN, max = NAME_MAX, message = "Category name must be between {min} and {max} characters")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9\\s\\-_&']+$",
             message = "Category name must start with a letter and contain only letters, numbers, spaces, hyphens, underscores, ampersands and apostrophes")
    @NoHtml
    private String categoryName;

    @Schema(description = "Detailed description of the category", example = "Electronic devices and accessories",
            maxLength = DESCRIPTION_MAX)
    @JsonProperty("description")
    @Size(max = DESCRIPTION_MAX, message = "Description cannot exceed {max} characters")
    @SafeText
    @Builder.Default
    private String description = "";

    @Schema(description = "Business justification for creating this category", example = "High demand from customers for electronic products",
            requiredMode = Schema.RequiredMode.REQUIRED, minLength = REASON_MIN, maxLength = REASON_MAX)
    @JsonProperty("reason")
    @NotBlank(message = "Please provide reason for this category")
    @Size(min = REASON_MIN, max = REASON_MAX, message = "Reason must be between {min} and {max} characters")
    @SafeText
    private String reason;

    @Schema(description = "ID of the parent category if this is a subcategory", example = "1")
    @JsonProperty("parent_id")
    private Long parentId;

    // Compatibility getters/setters
    public String getName() {
        return name != null ? name : categoryName;
    }

    public void setName(String name) {
        this.name = name;
        if (this.categoryName == null) {
            this.categoryName = name;
        }
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDescriptionOrEmpty() {
        return description == null ? "" : description;
    }

    public CategoryRequest trimFields() {
        if (categoryName != null) categoryName = categoryName.trim();
        if (description != null) description = description.trim();
        if (reason != null) reason = reason.trim();
        return this;
    }
}

