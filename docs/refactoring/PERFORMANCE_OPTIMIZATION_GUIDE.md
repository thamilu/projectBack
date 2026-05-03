# Performance Optimization Guide

> **Last Updated:** 2026-02-22  
> **Module:** Core Domain (Cart, Order, Product)  
> **Type:** Database Query & Write Optimization

---

## Overview

This document describes the performance optimizations applied to the E-shop backend. The project was already built with a strong enterprise-level foundation (Java 21 Virtual Threads, HikariCP tuning, multi-level caching, etc.), so this audit focused on identifying and resolving **N+1 query problems** — the most common but also most impactful database performance bottleneck in JPA/Hibernate applications.

---

## What is the N+1 Query Problem?

When you load a list of entities that have lazy-loaded collections, Hibernate fires:  
- **1** query to load the parent list  
- **N** additional queries (one per parent entity) to load each child collection  

This compounds badly at scale — loading 100 orders becomes 101 queries instead of 1.

---

## Fix 1 — Cart N+1 Read Queries

**File:** [`CartRepository.java`](../../src/main/java/com/eshop/app/repository/CartRepository.java)

### Problem

`Cart` has a lazy `@OneToMany` relationship to `CartItem`, and each `CartItem` has a lazy `@ManyToOne` to `Product`.  
When `CartService` loaded a cart and iterated its items to calculate totals, Hibernate fired:
- 1 query to load `Cart`
- N queries to load each `CartItem`
- N queries to load each `Product` (for price/stock)

For a cart with 10 items = **21 queries**.

### Fix Applied

```java
// CartRepository.java

@EntityGraph(attributePaths = {"items", "items.product"})
Optional<Cart> findByUserId(Long userId);

@EntityGraph(attributePaths = {"items", "items.product"})
Optional<Cart> findByCartCode(String cartCode);
```

**`@EntityGraph`** instructs Hibernate to perform a single `LEFT JOIN FETCH` covering `cart → items → product` in **one SQL query**.

### Impact

| Before | After |
|--------|-------|
| 1 + 2N queries | 1 query |
| 21 queries for 10-item cart | 1 query |

---

## Fix 2 — Order N+1 Read Queries

**File:** [`OrderRepository.java`](../../src/main/java/com/eshop/app/repository/OrderRepository.java)

### Problem

All list-returning `Order` query methods returned `Page<Order>` with lazily-loaded `OrderItem` collections. Mapping each page of 20 orders to a response DTO triggered 20 additional queries to fetch order items.

Affected methods:
- `findByCustomerId`
- `findByOrderStatus`
- `findByPaymentStatus`
- `findByDeliveryAgentId`
- `findByStoreSellerId`
- `findRecentOrdersBySellerId`

### Fix Applied

```java
// OrderRepository.java

@EntityGraph(attributePaths = {"items"})
Page<Order> findByCustomerId(Long customerId, Pageable pageable);

@EntityGraph(attributePaths = {"items"})
Page<Order> findByOrderStatus(Order.OrderStatus orderStatus, Pageable pageable);

// ... same pattern for other affected methods
```

### Impact

| Before | After |
|--------|-------|
| 1 + N queries per page | 1 query per page |
| 21 queries for 20-order page | 1 query |

---

## Fix 3 — Order N+1 Write Operations

**File:** [`OrderServiceImpl.java`](../../src/main/java/com/eshop/app/service/impl/OrderServiceImpl.java)

### Problem

In `createOrder()` and `processCheckout()`, product stock was decremented inside a `for` loop, with `productRepository.save(product)` called on each iteration:

```java
// ❌ BEFORE — N separate write transactions
for (CartItem item : cartItems) {
    Product product = item.getProduct();
    product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
    productRepository.save(product); // N database round-trips
}
```

For an order with 5 products = **5 separate UPDATE queries**.

### Fix Applied

```java
// ✅ AFTER — single batched write
List<Product> productsToUpdate = new ArrayList<>();
for (CartItem item : cartItems) {
    Product product = item.getProduct();
    product.setStockQuantity(product.getStockQuantity() - item.getQuantity());
    productsToUpdate.add(product);
}

// Single batched save — leverages spring.jpa.properties.hibernate.jdbc.batch_size
if (!productsToUpdate.isEmpty()) {
    productRepository.saveAll(productsToUpdate);
}
```

Works in concert with the Hibernate JDBC batch settings in `application.properties`:
```properties
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
```

### Impact

| Before | After |
|--------|-------|
| N separate UPDATEs | 1 batch UPDATE |
| 5 round-trips for 5-product order | 1 round-trip |

---

## Summary Table

| Area | Issue Type | Before | After |
|------|-----------|--------|-------|
| `CartRepository` | N+1 Read | 1 + 2N queries | 1 query |
| `OrderRepository` | N+1 Read | 1 + N queries | 1 query |
| `OrderServiceImpl.createOrder` | N+1 Write | N saves in loop | `saveAll()` once |
| `OrderServiceImpl.processCheckout` | N+1 Write | N saves in loop | `saveAll()` once |

---

## Related Configuration

These optimizations work best with the following properties already configured in `application.properties`:

```properties
# HikariCP Connection Pool
spring.datasource.hikari.maximum-pool-size=20
spring.datasource.hikari.minimum-idle=5
spring.datasource.hikari.connection-timeout=30000

# Hibernate Batch Processing
spring.jpa.properties.hibernate.jdbc.batch_size=50
spring.jpa.properties.hibernate.order_inserts=true
spring.jpa.properties.hibernate.order_updates=true
spring.jpa.properties.hibernate.batch_versioned_data=true

# Second-level cache (Caffeine/Redis)
spring.cache.type=caffeine
```

---

## See Also

- [`CODE_REUSABILITY_GUIDE.md`](./CODE_REUSABILITY_GUIDE.md) — Reusability refactoring applied to ProductServiceImpl
- [`../architecture/`](../architecture/) — System architecture overview
