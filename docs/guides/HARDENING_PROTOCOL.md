# [HARDEN] The Hardening Protocol & Enterprise Keywords

This document serves as the **Master Manifesto** for the E-Shop ecosystem. It defines the high-level engineering standards and the specific keywords used to trigger enterprise-grade workflows.

---

## 🛡️ The [HARDEN] Directive

The `[HARDEN]` keyword is a project-wide command that elevates any task from "Minimum Viable Product" to "Enterprise Production Standard." 

### When you say [HARDEN], the system enforces:
1.  **Strict Parity**: Code and Documentation must match 1:1. Any drift is treated as a critical bug.
2.  **Debt Purge**: Obsolete files, logs, and "dead" logic are automatically identified and deleted.
3.  **Security First**: Zero-Trust principles, sanitized inputs, and hardened auth headers are applied by default.
4.  **Staff/Principal Quality**: Code must follow the highest architectural patterns (DDD, Strategy Pattern, Resilience4j).

---

## 🧠 The Persistent Knowledge Base (KIs)

These keywords are not just labels; they are **active triggers** for the Knowledge Items (KIs) stored in the project's long-term memory at:
`C:\Users\Thamilu selvan N\.gemini\antigravity\knowledge`

When a keyword is used, the system performs a deep lookup of the associated KI to retrieve the specific **Staff/Principal-level** specifications we have saved.

---

## 🔑 High-Precision Keywords & Execution Logic

Use these keywords to trigger specialized Knowledge Items (KIs). When these keywords are used, the following **automated actions** will be performed:

### 📱 Frontend & UX
- **`frontend standards`**
    - **Action**: Refactors CSS/SCSS to a **Mobile-First** architecture.
    - **Action**: Injects standard breakpoints: Mobile (<768px), Tablet (768px-1024px), Desktop (>1024px).
    - **Action**: Converts fixed `px` widths to **fluid units** (`%`, `rem`, `vw`).
    - **Action**: Expands all touch targets to a minimum of **44x44px**.

- **`responsive design`**
    - **Action**: Audits layout for horizontal scrolling (Critical Bug).
    - **Action**: Implements **Flexbox/Grid** containerization.
    - **Action**: Adds `max-width` safety bounds for ultra-wide monitors.

- **`accessibility checklist`**
    - **Action**: Injects **ARIA** roles and semantic tags (`<nav>`, `<main>`).
    - **Action**: Validates color contrast against **WCAG AA** standards.
    - **Action**: Ensures keyboard focus states are highly visible.

### 🎨 Premium Aesthetics
- **`premium ui` / `glassmorphism`**
    - **Action**: Applies **24px background-blur** and semi-transparent `rgba` backgrounds.
    - **Action**: Injects **Animated Mesh Orbs** into the page background.
    - **Action**: Adds **Elastic/Staggered animations** for component entry.
    - **Action**: Implements high-contrast "glass" borders (`rgba(255,255,255,0.1)`).

### 📚 Global Protocol
- **`harden`**
    - **Action**: **Purge**: Deletes obsolete code, fragments, and redundant logs.
    - **Action**: **Sync**: Audits code vs. documentation; updates `API_REFERENCE.md` to match actual DTOs.
    - **Action**: **Security**: Injects input sanitization, security headers, and JWT validation logic.
    - **Action**: **Resilience**: Adds Resilience4j Circuit Breakers and Retry patterns to external service calls.
    - **Action**: **Robustness**: Injects comprehensive error handling and edge-case validation.
    - **Action**: **DRY (Don't Repeat Yourself)**: Consolidates redundant logic into reusable modules.
    - **Action**: **Optimization**: Audits for N+1 queries, memory leaks, and missing cache layers.
    - **Action**: **Scalability**: Verifies statelessness and ensures database connection pools are tuned.
    - **Action**: **Observability**: Injects Micrometer metrics, structured logging, and distributed tracing.
    - **Action**: **Maintainability**: Enforces SOLID principles, clean naming, and modular folder structures.
    - **Action**: **Security Audit**: Scans for hardcoded secrets and validates CORS/CSP policies.

---

## 🏗️ Architectural Pillars

### 1. Mobile-First Engineering
We do not "make it work on mobile later." We design for the **smallest device first** and scale upwards. Base styles are mobile; media queries are for enhancement.

### 2. Zero-Drift Documentation
Documentation is not an afterthought. It is a **Source of Truth**. If a DTO changes in the Java code, the `API_REFERENCE.md` must be updated in the same commit.

### 3. Glassmorphism Design System
All high-end portals must use the **E-Shop Glass System**:
- **Background**: Animated Mesh Orbs.
- **Foreground**: `rgba` glass panes with `backdrop-filter`.
- **Motion**: Elastic transitions and staggered entry animations.

---

## 📝 Developer Oath
> *"I will not ship code that is merely 'working.' I will only ship code that is **Hardened**, **Documented**, and **Premium**."*
