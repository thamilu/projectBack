package com.eshop.app.user.domain.repository;

import com.eshop.app.user.domain.entity.DeliveryAgentProfile;




import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeliveryAgentProfileRepository extends JpaRepository<DeliveryAgentProfile, Long> {
    java.util.Optional<DeliveryAgentProfile> findByUser_Id(Long userId);
    boolean existsByUser_Id(Long userId);
}
