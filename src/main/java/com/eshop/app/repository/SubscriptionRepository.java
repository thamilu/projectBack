package com.eshop.app.repository;

import com.eshop.app.entity.Subscription;
import com.eshop.app.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    List<Subscription> findByCustomer(User customer);
    List<Subscription> findByStatus(Subscription.SubscriptionStatus status);
    
    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' " +
           "AND s.nextBillingDate <= :date")
    List<Subscription> findDueForBilling(LocalDateTime date);
    
    List<Subscription> findByCustomerAndStatus(User customer, Subscription.SubscriptionStatus status);
}
