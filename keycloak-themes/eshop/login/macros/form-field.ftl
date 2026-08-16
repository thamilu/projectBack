<#ftl output_format="HTML" auto_esc=true>
<#import "svg-icons.ftl" as icons>

<#macro formField
    id
    name
    label
    type      = "text"
    value     = ""
    required  = false
    autoComplete = ""
    inputMode = ""
    extraAttrs = ""
    hasError  = false
    errorId   = ""
    errorMsg  = "">

    <div class="pf-c-form__group <#if hasError>pf-m-error</#if>">
        <label for="${id}" class="pf-c-form__label">
            ${label}
            <#if required>
                <span class="pf-c-form__label-required" aria-label="${msg('requiredField')}">*</span>
            </#if>
        </label>

        <input type="${type}"
               id="${id}"
               class="pf-c-form-control <#if hasError>pf-m-error</#if>"
               name="${name}"
               value="${value}"
               <#if required>required aria-required="true"</#if>
               <#if autoComplete?has_content>autocomplete="${autoComplete}"</#if>
               <#if inputMode?has_content>inputmode="${inputMode}"</#if>
               aria-invalid="${hasError?string('true', 'false')}"
               aria-describedby="${id}-error"
               ${extraAttrs} />

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

/**
 * Resolve the correct username/email label key based on realm configuration.
 *
 * @param realm The Keycloak realm configuration object
 * @returns Translatable label string
 */
<#macro usernameLabel>
    <#if !realm.loginWithEmailAllowed>
        ${msg("username")}
    <#elseif !realm.registrationEmailAsUsername>
        ${msg("usernameOrEmail")}
    <#else>
        ${msg("email")}
    </#if>
</#macro>

/**
 * Determine the correct HTML input type for the username field.
 * Returns 'email' only if registrationEmailAsUsername is true, otherwise 'text'.
 */
<#assign usernameInputType = realm.registrationEmailAsUsername?then('email', 'text') />

/**
 * Determine the autocomplete attribute value for the username field.
 * Returns 'username' if email login is disabled, otherwise 'email'.
 */
<#assign usernameAutocomplete = (!realm.loginWithEmailAllowed)?then('username', 'email') />

/**
 * Determine the inputmode attribute value for the username field on mobile devices.
 * Returns 'email' only if registrationEmailAsUsername is true, otherwise 'text'.
 */
<#assign usernameInputmode = realm.registrationEmailAsUsername?then('email', 'text') />
