package com.eshop.app.repository;

import com.eshop.app.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUserProfile_User_Id(Long userId);
    List<UserAddress> findByUserProfile_User_KeycloakId(String keycloakId);
}
