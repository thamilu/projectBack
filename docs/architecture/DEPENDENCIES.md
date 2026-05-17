# Dependency Governance

## Dependency Matrix
| Module | Allowed Dependencies | Forbidden Dependencies |
| :--- | :--- | :--- |
| **Catalog** | Core, Shared | Order, Payment, Shipping |
| **Order** | Catalog, User, Core, Shared | Payment (use Events) |
| **Payment** | Order (Port only), Core, Shared | Catalog |
| **Shipping** | Order (Port only), Core, Shared | Payment, Catalog |

## ArchUnit Enforcement
Architecture constraints are enforced via ArchUnit tests in `com.eshop.app.core.architecture`.

### Key Rules:
1. **Domain Isolation**: No domain classes should depend on `infrastructure` or `api`.
2. **Module Encapsulation**: Classes in `module.internal` should not be accessed from outside the module.
3. **No Circular Dependencies**: Circular dependencies between modules are strictly forbidden.
