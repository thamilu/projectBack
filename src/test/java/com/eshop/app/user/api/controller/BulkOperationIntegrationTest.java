package com.eshop.app.user.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eshop.app.core.infrastructure.config.security.AbstractIntegrationTest;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.UserRepository;
import com.eshop.app.user.shared.domain.enums.UserRole;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class BulkOperationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired private UserRepository userRepository;

    private User adminUser;
    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.webAppContextSetup(webApplicationContext)
                        .apply(springSecurity())
                        .build();

        userRepository.deleteAll();

        adminUser = User.create("kc-admin-123", "admin@example.com", UserRole.ADMIN);
        user1 = User.create("kc-user-1", "user1@example.com", UserRole.CUSTOMER);
        user2 = User.create("kc-user-2", "user2@example.com", UserRole.CUSTOMER);

        user1.softDelete("admin");
        user2.softDelete("admin");

        userRepository.saveAll(List.of(adminUser, user1, user2));
    }

    @Test
    void testBulkActivate_SuccessAndIdempotency() throws Exception {
        String requestBody = "{\"userIds\": [" + user1.getId() + ", " + user2.getId() + "]}";
        String idempotencyKey = "test-key-123";

        mockMvc.perform(
                        post("/api/v1/users/bulk/activate")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                                .with(
                                        jwt().jwt(
                                                        j ->
                                                                j.subject("kc-admin-123")
                                                                        .claim(
                                                                                "email",
                                                                                "admin@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(2));

        mockMvc.perform(
                        post("/api/v1/users/bulk/activate")
                                .header("Idempotency-Key", idempotencyKey)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                                .with(
                                        jwt().jwt(
                                                        j ->
                                                                j.subject("kc-admin-123")
                                                                        .claim(
                                                                                "email",
                                                                                "admin@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.successCount").value(2));
    }

    @Test
    void testBulkActivate_ValidationFails_EmptyList() throws Exception {
        String requestBody = "{\"userIds\": []}";

        mockMvc.perform(
                        post("/api/v1/users/bulk/activate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                                .with(
                                        jwt().jwt(
                                                        j ->
                                                                j.subject("kc-admin-123")
                                                                        .claim(
                                                                                "email",
                                                                                "admin@example.com"))))
                .andExpect(status().isBadRequest());
    }
}
