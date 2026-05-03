package com.eshop.app.repository;

import com.eshop.app.entity.DeliveryAgentProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryAgentProfileRepository extends JpaRepository<DeliveryAgentProfile, Long> {
    // Additional query methods can be added here if needed
}
