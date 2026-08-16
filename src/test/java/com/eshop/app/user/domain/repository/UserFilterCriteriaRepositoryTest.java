package com.eshop.app.user.domain.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.eshop.app.core.infrastructure.config.security.AbstractIntegrationTest;
import com.eshop.app.user.domain.entity.User;
import com.eshop.app.user.shared.domain.enums.UserRole;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class UserFilterCriteriaRepositoryTest extends AbstractIntegrationTest {

    @Autowired private UserRepository userRepository;

    private User activeUser;
    private User deletedUser;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        activeUser = User.create("kc-active", "active@example.com", UserRole.CUSTOMER);
        deletedUser = User.create("kc-deleted", "deleted@example.com", UserRole.CUSTOMER);
        deletedUser.softDelete("admin");

        userRepository.saveAll(List.of(activeUser, deletedUser));
    }

    @Test
    void testFindAllByDeleted_True() {
        Page<User> deletedUsers = userRepository.findAllByDeleted(true, PageRequest.of(0, 10));
        assertEquals(1, deletedUsers.getTotalElements());
        assertEquals("deleted@example.com", deletedUsers.getContent().get(0).getEmail());
    }

    @Test
    void testFindAllByDeleted_False() {
        Page<User> activeUsers = userRepository.findAllByDeleted(false, PageRequest.of(0, 10));
        assertEquals(1, activeUsers.getTotalElements());
        assertEquals("active@example.com", activeUsers.getContent().get(0).getEmail());
    }

    @Test
    void testFindByRoleAndDeleted() {
        Page<User> activeCustomers =
                userRepository.findByRoleAndDeleted(
                        UserRole.CUSTOMER, false, PageRequest.of(0, 10));
        assertEquals(1, activeCustomers.getTotalElements());
    }
}
