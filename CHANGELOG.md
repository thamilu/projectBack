# Changelog

Notable changes to the eshop-back API, especially breaking changes to the HTTP contract.
Format loosely follows [Keep a Changelog](https://keepachangelog.com/).

## Unreleased

### Changed (Breaking)

- **User self-protection violations now return `403 Forbidden` instead of `400 Bad Request`.**
  Affects `DELETE /api/v1/users/{id}`, `PUT /api/v1/users/{id}/activate`,
  `PUT /api/v1/users/{id}/deactivate`, `PUT /api/v1/users/{id}/role`,
  `POST /api/v1/users/bulk/activate`, and `POST /api/v1/users/bulk/deactivate` when an admin
  targets their own account (error codes `USER_SELF_DELETE`, `USER_SELF_ACTIVATE`,
  `USER_SELF_DEACTIVATE`, `USER_SELF_ROLE_CHANGE`).
  - **Why:** the request is syntactically valid and well-formed — it is rejected on business
    policy grounds (preventing accidental admin self-lockout), which is what `403 Forbidden`
    means. `400 Bad Request` is reserved for malformed/invalid input.
  - **Client impact:** any client-side logic keyed on `400` for these specific error codes must
    be updated to check for `403`. The response body shape (`errorCode`, `message`) is unchanged.
  - Enforcement was also deduplicated: `UserController` previously duplicated this check inline
    for 4 of the 6 endpoints (with hardcoded strings, inconsistently applied); it is now solely
    enforced by `UserSelfProtectionGuard` for all 6 endpoints.

### Fixed

- `UserSelfProtectionGuard.preventSelfOperation` no longer throws `NullPointerException` when
  `targetId` is null (previously produced a `500` instead of correctly no-op'ing).
- `UserSelfProtectionGuard.preventSelfOperationInBulk` now rejects a target-ID collection
  containing a `null` element with `400 Bad Request` (`INVALID_TARGET_IDS`) instead of silently
  propagating it downstream.
