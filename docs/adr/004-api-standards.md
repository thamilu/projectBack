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
4. **Consistency**: Use standard HTTP status codes (200 OK, 201 Created, 400 Bad Request, 401 Unauthorized, 404 Not Found, 500 Internal Error).

## Consequences
- **Development**: Developers must wrap exceptions or use the `@RestControllerAdvice` to transform errors into `ApiError`.
- **Frontend**: Must handle the `ApiError` JSON structure in the service layer.
