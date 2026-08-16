package com.eshop.app.realtime.api.controller;

import com.eshop.app.core.api.response.ApiResponse;
import com.eshop.app.core.exception.base.BusinessException;
import com.eshop.app.core.infrastructure.config.security.web.UserSecurityExpression;
import com.eshop.app.core.kernel.ApiConstants;
import com.eshop.app.core.ratelimit.RateLimitKeyType;
import com.eshop.app.core.ratelimit.RateLimited;
import com.eshop.app.realtime.api.response.WsTicketResponse;
import com.eshop.app.realtime.infrastructure.config.WsTicketConfig;
import com.eshop.app.realtime.application.service.WsTicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Issues short-lived, single-use tickets for authenticating WebSocket
 * handshakes.
 *
 * <p>No {@code @PreAuthorize} needed beyond the default {@code
 * .anyRequest().authenticated()} SecurityConfig already applies — {@code
 * /api/v1/ws/**} is not in any {@code permitAll()} list.
 */
@RestController
@RequestMapping(ApiConstants.Endpoints.WS)
@RequiredArgsConstructor
@Tag(name = "Realtime", description = "WebSocket connection ticket issuance")
public class WsTicketController {

    private final WsTicketService wsTicketService;
    private final WsTicketConfig wsTicketConfig;
    private final UserSecurityExpression userSecurity;

    @PostMapping("/ticket")
    @Operation(
            summary = "Request a WebSocket connection ticket",
            description =
                    "Mints a short-lived (default 45s), single-use ticket scoped only to "
                            + "authenticating one WebSocket handshake — never a general-purpose "
                            + "bearer credential.",
            security = @SecurityRequirement(name = "Bearer Authentication"))
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "201",
                description = "Ticket issued successfully"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "Authentication required or identity could not be resolved"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "429",
                description = "Too many ticket requests")
    })
    @RateLimited(value = "wsTicket", keyType = RateLimitKeyType.USER)
    public ResponseEntity<ApiResponse<WsTicketResponse>> requestTicket() {
        Long userId =
                userSecurity
                        .getCurrentUserId()
                        .orElseThrow(
                                () ->
                                        new BusinessException(
                                                "Unable to resolve authenticated user identity",
                                                "WS_TICKET_UNRESOLVED_IDENTITY",
                                                HttpStatus.UNAUTHORIZED));

        String ticket = wsTicketService.generateTicket(String.valueOf(userId));
        WsTicketResponse response =
                new WsTicketResponse(ticket, wsTicketConfig.getValidity().toSeconds());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }
}
