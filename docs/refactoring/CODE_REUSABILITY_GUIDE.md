# Code Reusability Guide

> **Last Updated:** 2026-02-22  
> **Module:** Product Service  
> **Type:** Code Quality / DRY Principle Enforcement

---

## Overview

This document describes the code reusability improvements made to [`ProductServiceImpl.java`](../../src/main/java/com/eshop/app/service/impl/ProductServiceImpl.java).

The project uses a `ProductServiceHelper` component to isolate reusable business logic. However, `ProductServiceImpl` had several places where the same logic was duplicated instead of delegated — violating the **DRY (Don't Repeat Yourself)** principle.

---

## Reusability Violations Found & Fixed

### 1. Tag Resolution in `updateProduct()` — N+1 Loop

**Problem:** `updateProduct()` was resolving product tags with an inline N+1 loop:

```java
// ❌ BEFORE — N+1 loop, logic duplicated from createProduct area
if (request.getTags() != null) {
    Set<Tag> tags = new HashSet<>();
    for (String tagName : request.getTags()) {
        Tag tag = tagRepository.findByName(tagName)
                .orElseGet(() -> tagRepository.save(Tag.builder().name(tagName).build()));
        tags.add(tag);
    }
    product.setTags(tags);
}
```

Meanwhile, `createProduct()` already used the optimized helper:

```java
// ✅ createProduct already used this (N+1 → 2 queries)
Set<Tag> tags = helper.resolveOrCreateTags(request.getTags());
```

**Fix:** Delegate to the same helper in `updateProduct()`:

```java
// ✅ AFTER — consistent, optimized, no duplication
if (request.getTags() != null) {
    product.setTags(helper.resolveOrCreateTags(request.getTags()));
}
```

**`resolveOrCreateTags()` algorithm:**
1. Normalize all tag names (trim, lowercase)
2. Fetch **all matching** tags in one `findByNameIn()` query
3. Identify missing tags
4. Batch-insert missing tags with `saveAll()`
5. Return combined set — always **2 queries max** regardless of tag count

---

### 2. `generateFriendlyUrl()` — Duplicated Logic

**Problem:** The exact same slug-generation regex was copy-pasted into `ProductServiceImpl` as a private method, even though `ProductServiceHelper` already had it.

```java
// ❌ BEFORE in ProductServiceImpl (duplicate)
private String generateFriendlyUrl(String name) {
    return name.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
}
```

```java
// ✅ AFTER — delegate to single source of truth
private String generateFriendlyUrl(String name) {
    return helper.generateFriendlyUrl(name);
}
```

---

### 3. `ensureUniqueFriendlyUrl()` — Duplicated and Unsafe

**Problem:** `ProductServiceImpl` had its own version of the uniqueness-check loop that was **unbounded** (infinite loop risk in theory):

```java
// ❌ BEFORE — no upper bound, could loop forever if DB is corrupted
private String ensureUniqueFriendlyUrl(String baseUrl) {
    String friendlyUrl = baseUrl;
    int counter = 1;
    while (productRepository.existsByFriendlyUrl(friendlyUrl)) {
        friendlyUrl = baseUrl + "-" + counter;
        counter++;  // no max limit!
    }
    return friendlyUrl;
}
```

`ProductServiceHelper` has the **safe version** with a circuit breaker and UUID fallback:

```java
// ✅ ProductServiceHelper — safe version with max attempts guard
public String ensureUniqueFriendlyUrl(String baseUrl) {
    if (!productRepository.existsByFriendlyUrl(baseUrl)) return baseUrl;

    int maxAttempts = productProperties.getMaxFriendlyUrlAttempts(); // configurable
    for (int counter = 1; counter <= maxAttempts; counter++) {
        String url = String.format("%s-%d", baseUrl, counter);
        if (!productRepository.existsByFriendlyUrl(url)) return url;
    }

    // UUID fallback — virtually impossible to collide
    String uuidUrl = baseUrl + "-" + UUID.randomUUID().toString().substring(0, 8);
    if (productRepository.existsByFriendlyUrl(uuidUrl)) {
        throw new FriendlyUrlGenerationException("Failed to generate unique URL for: " + baseUrl);
    }
    return uuidUrl;
}
```

**Fix:** Delegate to the safe version:

```java
// ✅ AFTER
private String ensureUniqueFriendlyUrl(String baseUrl) {
    return helper.ensureUniqueFriendlyUrl(baseUrl);
}
```

---

### 4. `getPrimaryImageUrl()` — Duplicated from ProductMapper

**Problem:** `ProductServiceImpl` had a private `getPrimaryImageUrl(Product)` method that duplicated the exact same null-safe, `Hibernate.isInitialized()` protected logic already in `ProductMapper`:

```java
// ❌ BEFORE — same code in two places
private String getPrimaryImageUrl(Product product) {
    if (product == null) return null;
    if (product.getPrimaryImage() != null) return product.getPrimaryImage().getUrl();
    try {
        if (product.getImages() != null && Hibernate.isInitialized(product.getImages()) ...
    } ...
}
```

**Fix:** Delegate to the mapper, the single source of truth for product → DTO mapping logic:

```java
// ✅ AFTER
private String getPrimaryImageUrl(Product product) {
    return productMapper.getPrimaryImageUrl(product);
}
```

---

## Key Components and Their Responsibilities

| Component | Responsibility |
|-----------|---------------|
| `ProductServiceImpl` | Orchestrates business flows, applies security, transactions |
| `ProductServiceHelper` | Reusable entity-level helpers (URL generation, tag resolution, entity building) |
| `ProductMapper` | Response mapping from entities to DTOs |

**Rule:** If the same logic is needed in two different methods, it lives in the helper — not in both methods.

---

## Build Verification

After all changes, the project compiled cleanly:

```
BUILD SUCCESSFUL in 31s
```

---

## See Also

- [`PERFORMANCE_OPTIMIZATION_GUIDE.md`](./PERFORMANCE_OPTIMIZATION_GUIDE.md) — N+1 query fixes in Cart and Order
- [`ProductServiceHelper.java`](../../src/main/java/com/eshop/app/service/impl/ProductServiceHelper.java)
- [`ProductMapper.java`](../../src/main/java/com/eshop/app/mapper/ProductMapper.java)
