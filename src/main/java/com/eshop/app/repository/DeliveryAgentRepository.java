package com.eshop.app.repository;

import com.eshop.app.entity.DeliveryAgentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgentProfile, Long> {
    Optional<DeliveryAgentProfile> findByUser_Id(Long userId);
    boolean existsByUser_Id(Long userId);
}
