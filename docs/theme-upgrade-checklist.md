# Keycloak Theme Upgrade Checklist

Use this checklist when upgrading Keycloak (e.g., v26 to v27) to prevent breaking changes in templates, stylesheets, and message keys.

## Pre-Upgrade Steps

1. **Check Release Notes**: Identify any template changes, removed files, or modified message keys in the new Keycloak version.
2. **Audit Base Templates**: Keycloak templates inherit from the `keycloak` base theme. Check if Keycloak's own `login.ftl` or `template.ftl` changed.
3. **Verify Dependencies**: Confirm that resources loaded via `import=common/keycloak` in `theme.properties` are still present.

## Testing & Compilation

1. **Verify Asset Bundle**: Run the bundler script to ensure compiled styles are current:
   ```bash
   npm run build
   ```
2. **Local Integration Check**: Start the Keycloak dev server locally and verify page loading:
   * Confirm there are no FreeMarker runtime parsing errors.
   * Verify console error reports.
3. **Responsive Spacing Verification**: Test visual alignment at various viewport resolutions (320px, 768px, 1440px).
4. **Subresource Integrity Validation**: Ensure SRI hashes are successfully generated and loaded by checking stylesheet/script elements in developer tools.
