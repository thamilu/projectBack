package com.eshop.app.customer.api.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreInterestedUsersResponse {
    private Long userId;
    private String username;
    private String email;
}
