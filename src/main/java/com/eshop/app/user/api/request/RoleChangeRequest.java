package com.eshop.app.user.api.request;

import com.eshop.app.user.shared.domain.enums.UserRole;




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
