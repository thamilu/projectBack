<#ftl output_format="HTML" auto_esc=true>
<#import "svg-icons.ftl" as icons>

<#--
  @macro passwordField
  ──────────────────────────────────────────────────────────────────────────────
  Renders a fully accessible, CSP-compliant password input field with optional
  strength meter and confirmation-match error region.

  @param {string} id               - REQUIRED. Unique HTML element ID. All
                                     child IDs are derived as "${id}-*" to
                                     prevent collisions when the macro is
                                     instantiated multiple times on the same page.
  @param {string} name             - REQUIRED. Form field name attribute.
  @param {string} label            - REQUIRED. Visible field label text.
  @param {string} autocomplete     - Autocomplete hint. Use "new-password" for
                                     creation, "current-password" for login.
                                     Defaults to "new-password".
  @param {boolean} hasError        - Whether server validation produced an error.
  @param {string} errorId          - ID of the server-rendered error element.
  @param {string} errorMsg         - Server-provided error message text (sanitized
                                     externally before being passed here).
  @param {boolean} showStrengthMeter - Render the strength progressbar and
                                       requirements checklist beneath this field.
  @param {boolean} showConfirmError  - Render the client-side confirm-match error
                                       region beneath this field.
  ──────────────────────────────────────────────────────────────────────────────
-->
<#macro passwordField
    id
    name
    label
    autocomplete     = "new-password"
    hasError         = false
    errorId          = ""
    errorMsg         = ""
    showStrengthMeter = false
    showConfirmError = false>

<#-- ─── Parameter Validation (fail-fast guards) ────────────────── -->
<#if !id?has_content>
    <#stop "passwordField macro: 'id' parameter is required and must not be empty.">
</#if>
<#if !name?has_content>
    <#stop "passwordField macro: 'name' parameter is required and must not be empty.">
</#if>
<#if !label?has_content>
    <#stop "passwordField macro: 'label' parameter is required and must not be empty.">
</#if>

<#-- ─── Derived, collision-free child IDs ────────────────────────
     All child element IDs are prefixed with ${id}- so that two instances
     of this macro (e.g. "password" and "password-confirm") never collide.
────────────────────────────────────────────────────────────────── -->
<#assign strengthDescId    = id + "-strength-desc"  />
<#assign strengthFillId    = id + "-strength-fill"  />
<#assign strengthLabelId   = id + "-strength-label" />
<#assign matchErrorId      = id + "-match-error"    />
<#assign reqLengthId       = id + "-req-length"     />
<#assign reqUpperId        = id + "-req-upper"      />
<#assign reqLowercaseId    = id + "-req-lowercase"  />
<#assign reqNumberId       = id + "-req-number"     />
<#assign reqSpecialId      = id + "-req-special"    />

<#-- ─── aria-describedby composition ───────────────────────────── -->
<#assign describedBy = "" />
<#if showStrengthMeter>
    <#assign describedBy = describedBy + strengthDescId + " " />
</#if>
<#if showConfirmError>
    <#assign describedBy = describedBy + matchErrorId + " " />
</#if>
<#assign describedBy = describedBy + id + "-error" />
<#if hasError && errorId?has_content>
    <#assign describedBy = describedBy + " " + errorId />
</#if>
<#assign describedBy = describedBy?trim />

    <div class="pf-c-form__group<#if hasError> pf-m-error</#if>">
        <label for="${id}" class="pf-c-form__label">
            ${label}
            <span class="pf-c-form__label-required" aria-label="${msg('requiredField')}">*</span>
        </label>

        <div class="kc-password-wrapper">
            <input type="password"
                   id="${id}"
                   class="pf-c-form-control<#if hasError> pf-m-error</#if>"
                   name="${name}"
                   autocomplete="${autocomplete}"
                   required
                   aria-required="true"
                   aria-invalid="${hasError?string('true', 'false')}"
                   <#if describedBy?has_content>aria-describedby="${describedBy}"</#if> />

            <button type="button"
                    class="kc-password-toggle"
                    data-action="toggle-password"
                    data-target="${id}"
                    aria-label="${msg('showPassword')}"
                    aria-pressed="false"
                    aria-controls="${id}">
                <@icons.eyeIcon />
                <@icons.eyeOffIcon hidden=true />
            </button>
        </div>

        <#-- ── Confirm-match Error Region ───────────────────────────
             Uses standard HTML `hidden` attribute (no inline style).
             Toggled by JS via element.hidden = true/false.
             ID is field-specific to prevent collision.
        ──────────────────────────────────────────────────────────── -->
        <#if showConfirmError>
            <span id="${matchErrorId}"
                  class="pf-c-form__helper-text pf-m-error"
                  role="alert"
                  aria-live="assertive"
                  aria-atomic="true"
                  hidden>
                <@icons.errorIcon />
                <span class="kc-error-message-text"></span>
            </span>
        </#if>

        <#-- ── Password Strength Meter ─────────────────────────────
             IDs are field-specific. aria-valuenow updated by JS.
             Requirements list uses role="list" + role="listitem".
             NO aria-checked on <li> — that is invalid ARIA usage.
             Requirement state is communicated via:
               • Visual class (.req--met / .req--unmet)
               • aria-label on each <li> updated by JS
               • aria-live on the parent region
        ──────────────────────────────────────────────────────────── -->
        <#if showStrengthMeter>
            <div class="kc-strength-meter"
                 id="${strengthDescId}"
                 aria-live="polite"
                 aria-label="${msg('passwordStrength')}">

                <div class="kc-strength-track">
                    <div class="kc-strength-fill"
                         id="${strengthFillId}"
                         role="progressbar"
                         aria-label="${msg('passwordStrength')}"
                         aria-valuemin="0"
                         aria-valuemax="100"
                         aria-valuenow="0">
                    </div>
                </div>

                <div class="kc-strength-label-container">
                    <span class="kc-strength-label-text">${msg("passwordStrength")}:</span>
                    <span class="kc-strength-label" id="${strengthLabelId}" aria-live="polite"></span>
                </div>

                <ul class="kc-requirements"
                    aria-label="${msg('passwordRequirements')}"
                    role="list">
                    <li class="kc-req req--initial"
                        id="${reqLengthId}"
                        role="listitem"
                        aria-label="${msg('passwordReqLength')}">
                        <@icons.crossIcon />
                        <span>${msg("passwordReqLength")}</span>
                    </li>
                    <li class="kc-req req--initial"
                        id="${reqUpperId}"
                        role="listitem"
                        aria-label="${msg('passwordReqUppercase')}">
                        <@icons.crossIcon />
                        <span>${msg("passwordReqUppercase")}</span>
                    </li>
                    <li class="kc-req req--initial"
                        id="${reqLowercaseId}"
                        role="listitem"
                        aria-label="${msg('passwordReqLowercase')}">
                        <@icons.crossIcon />
                        <span>${msg("passwordReqLowercase")}</span>
                    </li>
                    <li class="kc-req req--initial"
                        id="${reqNumberId}"
                        role="listitem"
                        aria-label="${msg('passwordReqNumber')}">
                        <@icons.crossIcon />
                        <span>${msg("passwordReqNumber")}</span>
                    </li>
                    <li class="kc-req req--initial"
                        id="${reqSpecialId}"
                        role="listitem"
                        aria-label="${msg('passwordReqSpecial')}">
                        <@icons.crossIcon />
                        <span>${msg("passwordReqSpecial")}</span>
                    </li>
                </ul>
            </div>
        </#if>

        <#-- ── Validation Error Span (Always Rendered for Client/Server side) ── -->
        <span id="${id}-error"
              class="pf-c-form__helper-text pf-m-error"
              role="alert"
              aria-live="assertive"
              aria-atomic="true"
              <#if !hasError>hidden</#if>>
            <@icons.errorIcon />
            <span class="kc-error-message__text"><#if hasError && errorMsg?has_content>${kcSanitize(errorMsg)?no_esc}</#if></span>
        </span>
    </div>
</#macro>
