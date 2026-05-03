package com.eshop.app.repository;

import com.eshop.app.entity.Subscription;
import com.eshop.app.entity.SubscriptionTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionTransactionRepository extends JpaRepository<SubscriptionTransaction, Long> {
    List<SubscriptionTransaction> findBySubscription(Subscription subscription);
    List<SubscriptionTransaction> findBySubscriptionOrderByAttemptedAtDesc(Subscription subscription);
    List<SubscriptionTransaction> findByStatus(SubscriptionTransaction.TransactionStatus status);
}
