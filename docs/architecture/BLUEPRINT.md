# E-Shop Architecture Blueprint

## Modular Monolith Structure
The E-Shop backend follows a strict 4-layer Modular Monolith architecture.

### Layers
1. **API (Controller)**: Handles HTTP requests, validation, and security.
2. **Application (Use Cases & Ports)**: Orchestrates business logic via Use Cases and defines interface ports.
3. **Domain (Entities & Value Objects)**: Rich domain models containing business logic and invariants.
4. **Infrastructure (Adapters & Config)**: Database persistence, external API clients, and module-specific configuration.

## Dependency Rules
- **Modules MUST NOT depend on each other's Infrastructure.**
- **Modules should communicate via Events (Asynchronous) or Application Ports (Synchronous).**
- **Domain must have ZERO dependencies on outer layers (Hexagonal Principle).**
- **No Shared Leakage**: All business-specific logic belongs to its respective module, not `shared`.

## Naming Conventions
- Use Cases: `[Action][Entity]UseCase` (e.g., `CreateOrderUseCase`)
- Ports: `[Entity]Port` or `[Action]Port`
- Entities: Singular nouns (e.g., `Product`, `Order`)
- DTOs: `[Entity][Action][Request/Response]` (e.g., `ProductCreateRequest`)
