# E-Shop Keycloak Theme Documentation

This documentation defines the technical specifications, component macros, styling constraints, security principles, and build workflows for the custom E-Shop Keycloak theme.

---

## 1. Architecture Overview

### Theme Directory Structure
```text
keycloak-themes/eshop/
├── THEME-README.md         # Developer architecture & standards guide
├── theme.properties        # Theme properties, CSS/JS configurations, and asset SRI hashes
├── scripts/
│   └── build.js            # Build script for minifying assets and generating dynamic SRI hashes
└── login/
    ├── template.ftl        # Master layout macro with global landmarks (main, noscript, skip link)
    ├── login.ftl           # Secure authentication template
    ├── register.ftl        # Secure registration template
    ├── login-reset-password.ftl # Secure forgotten password reset template
    ├── terms.ftl           # Localized terms of service consent template
    ├── macros/
    │   ├── svg-icons.ftl   # Reusable SVG icon components (social, status, spinner)
    │   └── form-field.ftl  # Unified form labels, error handlers, and input generators
    ├── messages/
    │   ├── messages.properties    # Default translation strings (English)
    │   └── messages_ar.properties # Localized translation strings (Arabic)
    └── resources/
        ├── css/            # Modular stylesheets (base, layout, components, themes)
        └── js/             # Interactive client-side component code
```

---

## 2. Shared Component Macros

### SVG Icon Library (`login/macros/svg-icons.ftl`)
Contains centralized, compile-time SVG templates that prevent code replication.
- `<@icons.socialIcon provider="[alias]" displayName="[name]" />`: Renders custom, validated social network icons. Whitelisted internally to prevent arbitrary provider XSS injection.
- `<@icons.spinnerIcon initiallyHidden=true />`: Renders the standardized loading spinner with configurable visibility.
- `<@icons.errorIcon />`, `<@icons.successIcon />`, `<@icons.infoIcon />`: Renders uniform semantic alerts.

### Form Fields (`login/macros/form-field.ftl`)
Abstracts mobile-friendly username labels, autocompletes, types, and error layouts.
- `<@formField.usernameLabel />`: Multi-way check of realm settings to output "Username", "Email", or "Username or Email".
- `${formField.usernameInputType?trim}`: Evaluates to `email` if input is strictly email-only; otherwise `text`.
- `${formField.usernameAutocomplete?trim}` / `${formField.usernameInputmode?trim}`: Injects corresponding autocomplete strings and mobile keyboards.

---

## 3. Styling Guidelines

### CSS Namespace & Naming Convention
- All custom classes **must** use the `.kc-` prefix (e.g., `.kc-login-subtitle`, `.kc-form-group`).
- Global base elements are isolated under `@layer` blocks (base, layout, components, utilities).

### Overrides & Specificity
- The use of `!important` is **strictly forbidden** on component styles. Overrides must be handled using CSS specificity cascade mappings:
  ```css
  /* Correct override specificity */
  .kc-page-header .kc-login-subtitle {
      color: var(--kc-color-text-secondary);
  }
  ```

---

## 4. Security Standards

### Cross-Site Scripting (XSS) Prevention
- Direct variable interpolation containing SVG markup or custom variables using `?no_esc` is forbidden.
- Use macros (`<@icons.socialIcon />`) or wrap variables inside the `${kcSanitize(...) ?no_esc}` function.

### Strict CSP Compliance
- Every inline JavaScript `<script>` block **must** register a secure nonce matching the Keycloak server token:
  ```html
  <script nonce="${cspNonce!''}">
      /* Self-contained script logic */
  </script>
  ```

### CSRF Protection
- Forms must submit security context parameters:
  ```html
  <input type="hidden" name="session_code" value="${session.code!''}"/>
  <input type="hidden" name="execution"    value="${execution!''}"/>
  <input type="hidden" name="client_id"    value="${client.clientId!''}"/>
  <input type="hidden" name="tab_id"       value="${tabId!''}"/>
  ```

---

## 5. Accessibility (A11y)

### Landmark Hierarchy
- The master layout `template.ftl` defines a single `<main id="kc-main-content" role="main">` landmark. Component templates must not contain local `<main>` blocks.
- The `<noscript>` message is registered in the layout `<body>` with a `role="note"` attribute.

### Labeling & Semantics
- Visual asterisks indicating required fields must declare an screen reader description:
  ```html
  <span class="kc-required" aria-label="${msg('requiredField')}">*</span>
  ```
- All interactive links (`<a>` elements with an `href`) must not define `role="button"`. Let screen readers announce them natively as links.

---

## 6. i18n & Localization

- Subtitles and titles must be stored in translation bundles, never hardcoded in `.ftl` templates:
  ```ftl
  <p class="kc-login-subtitle">${msg("termsSubtitle")}</p>
  ```
- Arabic bundles (`messages_ar.properties`) must encode Arabic characters properly or utilize unicode escape notation:
  ```properties
  termsSubtitle=\u0631\u0627\u062c\u0639 \u0627\u0644\u062a\u0632\u0627\u0645\u0646\u0627 \u0628\u062e\u0635\u0648\u0635\u064a\u062a\u0643
  ```

---

## 7. Asset Compilation & Building

### Running the Build Process
Compile modular scripts, minify CSS, and generate dynamic Subresource Integrity (SRI) hashes:
```bash
# Navigate to the theme directory and compile assets
npm run build
```

The build task generates output files in `resources/` and writes computed hashes directly into `theme.properties` under variables prefix `hash_css_...` or `hash_js_...`.
