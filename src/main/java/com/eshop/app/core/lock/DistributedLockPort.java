package com.eshop.app.core.lock;

import java.time.Duration;
import java.util.concurrent.Callable;

/**
 * [HARDEN] Distributed Lock Port — outbound port for distributed locking.
 *
 * Critical for heavy-traffic correctness in:
 * - Inventory reservation (prevent overselling)
 * - Payment processing (prevent double charge)
 * - Coupon usage (prevent race conditions on redemption)
 * - Order processing (prevent duplicate order creation)
 *
 * Backed by Redis (Redisson) in production.
 * Stub/test implementations can use in-memory ConcurrentHashMap.
 *
 * Rules:
 * - Lock keys MUST be namespaced: "lock:inventory:{productId}"
 * - Always use tryLock with timeout — never block indefinitely
 * - Always release in finally block
 */
public interface DistributedLockPort {

    /**
     * Executes the given task under a distributed lock.
     * Acquires lock, executes task, and releases lock in finally.
     *
     * @param lockKey    the unique lock identifier
     * @param waitTime   max time to wait for lock acquisition
     * @param leaseTime  max time the lock can be held before auto-release
     * @param task       the work to execute under the lock
     * @param <T>        the return type of the task
     * @return the result of the task
     * @throws LockAcquisitionException if lock could not be acquired within waitTime
     */
    <T> T executeWithLock(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> task);

    /**
     * Attempts to acquire the lock and execute the task without throwing on failure.
     * Returns empty Optional if the lock cannot be acquired.
     *
     * @param lockKey   the unique lock identifier
     * @param waitTime  max time to wait for lock acquisition
     * @param leaseTime max time the lock can be held
     * @param task      the work to execute
     * @param <T>       the return type
     * @return result of the task, or null if lock not acquired
     */
    <T> T tryExecuteWithLock(String lockKey, Duration waitTime, Duration leaseTime, Callable<T> task);
}
