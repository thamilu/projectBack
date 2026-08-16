package com.eshop.app.user.api.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUrlResponse {
    private String authorizationUrl;
    private String state;
    private String message;
}
