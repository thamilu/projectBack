# Architectural Decision Records (ADR)

## ADR 001: Use Case Architecture (Hexagonal)
**Date**: 2026-05-16
**Status**: Accepted
**Context**: Monolithic services (e.g., `DefaultOrderService`) were becoming too complex and violating SRP.
**Decision**: Adopt `UseCase` interfaces in `application/port/in` and separate implementations in `application/usecase`.
**Consequences**: Improved testability, clear business boundaries, and reduced orchestration logic in single classes.

## ADR 002: Module-Level Config Ownership
**Date**: 2026-05-16
**Status**: Accepted
**Context**: Global `config/` directory was a monolith, making it hard to identify module-specific settings.
**Decision**: Move configurations to `module/infrastructure/config`.
**Consequences**: Better encapsulation, easier module extraction in the future.

## ADR 003: Removal of Shared Business Leakage
**Date**: 2026-05-16
**Status**: Accepted
**Context**: `shared/` was being used as a dumping ground for DTOs and entities.
**Decision**: Move all domain-specific objects to their respective modules.
**Consequences**: Strict bounded context enforcement.
