# ADR-004: Standardized API Design and Error Handling

## Date
2026-05-10

## Status
Accepted

## Context
A consistent API experience is vital for frontend integration and external consumer adoption. We need a predictable way to handle responses, pagination, and errors.

## Decision
We adopted the **standardized RESTful API format** with a unified `ApiError` structure.

### Rationale:
1. **Predictability**: Frontend (Next.js) can implement a single error-handling interceptor.
2. **Traceability**: Every error includes a `correlationId` to link logs to specific client failures.
3. **Safety**: Prevents internal stack traces from leaking to the client.
4. **Consistency**: Use standard HTTP status codes (200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 403 Forbidden, 404 Not Found, 500 Internal Error).
5. **400 vs 403**: `400 Bad Request` is for malformed/invalid input (fails validation before any policy is applied). `403 Forbidden` is for a syntactically valid, well-formed request that is refused on authorization or business-policy grounds (e.g. an admin blocked from deactivating their own account by `UserSelfProtectionGuard` — see `CHANGELOG.md`).

## Consequences
- **Development**: Developers must wrap exceptions or use the `@RestControllerAdvice` to transform errors into `ApiError`.
- **Frontend**: Must handle the `ApiError` JSON structure in the service layer.
