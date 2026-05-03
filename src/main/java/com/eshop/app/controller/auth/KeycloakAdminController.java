package com.eshop.app.controller.auth;

import com.eshop.app.dto.auth.RegisterRequest;
import com.eshop.app.service.auth.KeycloakAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class KeycloakAdminController {
    
    private final KeycloakAdminService adminService;
    
    /**
     * Create a new user
     */
    @PostMapping("/users")
    public Mono<ResponseEntity<Map<String, String>>> createUser(
            @Valid @RequestBody RegisterRequest request) {
        log.info("Admin creating user: {}", request.getUsername());
        
        return adminService.createUser(request)
                .map(ResponseEntity::ok);
    }
    
    /**
     * Get all users
     */
    @GetMapping("/users")
    public Mono<ResponseEntity<List<Map<String, Object>>>> getAllUsers() {
        log.info("Admin fetching all users");
        
        return adminService.getAllUsers()
                .map(ResponseEntity::ok);
    }
    
    /**
     * Get user by username
     */
    @GetMapping("/users/{username}")
    public Mono<ResponseEntity<Map<String, Object>>> getUserByUsername(
            @PathVariable String username) {
        log.info("Admin fetching user: {}", username);
        
        return adminService.getUserByUsername(username)
                .map(ResponseEntity::ok);
    }
    
    /**
     * Delete user
     */
    @DeleteMapping("/users/{userId}")
    public Mono<ResponseEntity<Map<String, String>>> deleteUser(
            @PathVariable String userId) {
        log.info("Admin deleting user: {}", userId);
        
        return adminService.deleteUser(userId)
                .map(ResponseEntity::ok);
    }
    
    /**
     * Reset user password
     */
    @PutMapping("/users/{userId}/reset-password")
    public Mono<ResponseEntity<Map<String, String>>> resetPassword(
            @PathVariable String userId,
            @RequestBody Map<String, String> request) {
        log.info("Admin resetting password for user: {}", userId);
        
        String newPassword = request.get("password");
        
        return adminService.resetPassword(userId, newPassword)
                .map(ResponseEntity::ok);
    }
    
    /**
     * Update user
     */
    @PutMapping("/users/{userId}")
    public Mono<ResponseEntity<Map<String, String>>> updateUser(
            @PathVariable String userId,
            @RequestBody Map<String, Object> updates) {
        log.info("Admin updating user: {}", userId);
        
        return adminService.updateUser(userId, updates)
                .map(ResponseEntity::ok);
    }
}
