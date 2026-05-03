package com.eshop.app.dto.request;

import com.eshop.app.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleChangeRequest {

    @NotNull
    private UserRole newRole;
}
