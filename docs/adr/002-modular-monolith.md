# ADR-002: Adopting a Modular Monolith Architecture

## Date
2026-05-10

## Status
Accepted

## Context
As the project grows, a "flat" layered monolith (all services in one folder, all entities in another) becomes difficult to maintain. High coupling between unrelated features leads to merge conflicts and "spaghetti" code. However, Microservices would introduce excessive operational complexity (distributed tracing, network latency, infrastructure cost).

## Decision
We chose a **Modular Monolith** approach using **Spring Modulith** and **Domain-Driven Design (DDD)** principles.

### Rationale:
1. **Bounded Contexts**: Logic is grouped by business domain (Product, Order, User), not by technical layer (Service, Controller).
2. **Enforced Boundaries**: Spring Modulith validates that modules only communicate via public APIs or events.
3. **Low Overhead**: Runs in a single JVM, using local memory instead of network calls for inter-module communication.
4. **Future-Proof**: If a module (e.g., Payment) needs to scale independently later, its boundaries are already defined for extraction into a Microservice.

## Consequences
- **Refactoring**: Requires moving files from flat structures into domain-specific packages (`com.eshop.app.modules.*`).
- **Discipline**: Developers must avoid "illegal" cross-module imports (e.g., `OrderService` calling `ProductRepository` directly).
- **Tooling**: Requires `spring-modulith` dependency for runtime/test-time validation.
