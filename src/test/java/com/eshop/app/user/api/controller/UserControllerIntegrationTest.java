package com.eshop.app.user.api.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eshop.app.core.infrastructure.config.security.AbstractIntegrationTest;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.domain.repository.UserRepository;
import com.eshop.app.user.shared.domain.enums.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

class UserControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired private UserRepository userRepository;

    private User adminUser;
    private User normalUser;

    @BeforeEach
    void setUp() {
        mockMvc =
                MockMvcBuilders.webAppContextSetup(webApplicationContext)
                        .apply(springSecurity())
                        .build();

        userRepository.deleteAll();

        adminUser = User.create("kc-admin-123", "admin@example.com", UserRole.ADMIN);
        normalUser = User.create("kc-normal-123", "normal@example.com", UserRole.CUSTOMER);

        userRepository.save(adminUser);
        userRepository.save(normalUser);
    }

    @Test
    void testGetCurrentUser_Success() throws Exception {
        mockMvc.perform(
                        get("/api/v1/users/me")
                                .with(
                                        jwt().jwt(
                                                        j ->
                                                                j.subject("kc-normal-123")
                                                                        .claim(
                                                                                "email",
                                                                                "normal@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("normal@example.com"));
    }

    @Test
    void testGetUserById_ETagSupport() throws Exception {
        String etag =
                mockMvc.perform(
                                get("/api/v1/users/" + normalUser.getId())
                                        .with(
                                                jwt().jwt(
                                                                j ->
                                                                        j.subject("kc-admin-123")
                                                                                .claim(
                                                                                        "email",
                                                                                        "admin@example.com"))))
                        .andExpect(status().isOk())
                        .andExpect(header().exists("ETag"))
                        .andReturn()
                        .getResponse()
                        .getHeader("ETag");

        mockMvc.perform(
                        get("/api/v1/users/" + normalUser.getId())
                                .header("If-None-Match", etag)
                                .with(
                                        jwt().jwt(
                                                        j ->
                                                                j.subject("kc-admin-123")
                                                                        .claim(
                                                                                "email",
                                                                                "admin@example.com"))))
                .andExpect(status().isNotModified());
    }
}
