<#ftl output_format="HTML" auto_esc=true>

<#-- Reusable SVG Icon Macros -->

<#macro eyeIcon class="">
    <svg class="kc-icon kc-icon--eye ${class}"
         width="20" height="20" viewBox="0 0 24 24"
         fill="none" stroke="currentColor" stroke-width="2"
         aria-hidden="true" focusable="false" role="img">
        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
        <circle cx="12" cy="12" r="3"/>
    </svg>
</#macro>

<#macro eyeOffIcon class="" hidden=false>
    <svg class="kc-icon kc-icon--eye-off ${class}"
         width="20" height="20" viewBox="0 0 24 24"
         fill="none" stroke="currentColor" stroke-width="2"
         aria-hidden="true" focusable="false" role="img"
         <#if hidden>hidden</#if>>
        <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19"/>
        <line x1="1" y1="1" x2="23" y2="23"/>
    </svg>
</#macro>

<#macro errorIcon ariaHidden=true>
    <svg class="kc-error-icon" width="14" height="14" viewBox="0 0 14 14" <#if ariaHidden>aria-hidden="true"</#if> focusable="false" fill="currentColor" role="img">
        <path d="M7 0C3.13 0 0 3.13 0 7s3.13 7 7 7 7-3.13 7-7-3.13-7-7-7zm.75 10.5h-1.5v-1.5h1.5v1.5zm0-3h-1.5v-4h1.5v4z"/>
    </svg>
</#macro>

<#macro checkIcon ariaHidden=true>
    <svg class="kc-req-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3" <#if ariaHidden>aria-hidden="true"</#if> focusable="false" role="img">
        <polyline points="20 6 9 17 4 12"></polyline>
    </svg>
</#macro>

<#macro crossIcon ariaHidden=true>
    <svg class="kc-req-icon" viewBox="0 0 24 24" width="14" height="14" fill="none" stroke="currentColor" stroke-width="3" <#if ariaHidden>aria-hidden="true"</#if> focusable="false" role="img">
        <line x1="18" y1="6" x2="6" y2="18"></line>
        <line x1="6" y1="6" x2="18" y2="18"></line>
    </svg>
</#macro>

<#macro successIcon>
<svg class="kc-icon kc-icon--success" 
     width="18" height="18" 
     viewBox="0 0 24 24" 
     fill="none" 
     stroke="currentColor" 
     stroke-width="2"
     aria-hidden="true"
     focusable="false">
    <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
    <polyline points="22 4 12 14.01 9 11.01"/>
</svg>
</#macro>

<#macro alertIcon>
<svg class="kc-icon kc-icon--alert" 
     width="18" height="18" 
     viewBox="0 0 24 24" 
     fill="none" 
     stroke="currentColor" 
     stroke-width="2"
     aria-hidden="true"
     focusable="false">
    <circle cx="12" cy="12" r="10"/>
    <line x1="12" y1="8" x2="12" y2="12"/>
    <line x1="12" y1="16" x2="12.01" y2="16"/>
</svg>
</#macro>

<#macro infoIcon>
<svg class="kc-icon kc-icon--info" 
     width="18" height="18" 
     viewBox="0 0 24 24" 
     fill="none" 
     stroke="currentColor" 
     stroke-width="2"
     aria-hidden="true"
     focusable="false">
    <circle cx="12" cy="12" r="10"/>
    <line x1="12" y1="16" x2="12" y2="12"/>
    <line x1="12" y1="8" x2="12.01" y2="8"/>
</svg>
</#macro>

<#-- ── Social Icons ─────────────────────────────────────────── -->
<#macro _googleSvg>
<svg viewBox="0 0 24 24" width="20" height="20"
     class="kc-social-icon" aria-hidden="true" focusable="false">
    <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#4285F4"/>
    <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#34A853"/>
    <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.06H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.94l2.85-2.22.81-.63z" fill="#FBBC05"/>
    <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.06l3.66 2.84c.87-2.6 3.3-4.52 6.16-4.52z" fill="#EA4335"/>
</svg>
</#macro>

<#macro _githubSvg>
<svg viewBox="0 0 24 24" width="20" height="20"
     class="kc-social-icon" aria-hidden="true" focusable="false">
    <path fill-rule="evenodd" clip-rule="evenodd"
          d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.531 1.032 1.531 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482C19.138 20.193 22 16.44 22 12.017 22 6.484 17.522 2 12 2z"
          fill="currentColor"/>
</svg>
</#macro>

<#macro _microsoftSvg>
<svg viewBox="0 0 23 23" width="20" height="20"
     class="kc-social-icon" aria-hidden="true" focusable="false">
    <path fill="#f35325" d="M0 0h10.5v10.5H0z"/>
    <path fill="#81bc06" d="M11.5 0H22v10.5H11.5z"/>
    <path fill="#05a6f0" d="M0 11.5h10.5V22H0z"/>
    <path fill="#ffba08" d="M11.5 11.5H22V22H11.5z"/>
</svg>
</#macro>

<#macro _linkedinSvg>
<svg viewBox="0 0 24 24" width="20" height="20"
     class="kc-social-icon" aria-hidden="true" focusable="false">
    <path d="M19 0h-14c-2.761 0-5 2.239-5 5v14c0 2.761 2.239 5 5 5h14c2.762 0 5-2.239 5-5v-14c0-2.761-2.238-5-5-5zm-11 19h-3v-11h3v11zm-1.5-12.268c-.966 0-1.75-.779-1.75-1.75s.784-1.75 1.75-1.75 1.75.779 1.75 1.75-.784 1.75-1.75 1.75zm13.5 12.268h-3v-5.604c0-3.368-4-3.113-4 0v5.604h-3v-11h3v1.765c1.396-2.586 7-2.777 7 2.476v6.759z"
          fill="currentColor"/>
</svg>
</#macro>

<#macro socialIcon provider displayName="">
    <#assign _allowed = ["google","github","microsoft","linkedin"] />
    <#if _allowed?seq_contains(provider)>
        <#if provider == "google"><@_googleSvg /></#if>
        <#if provider == "github"><@_githubSvg /></#if>
        <#if provider == "microsoft"><@_microsoftSvg /></#if>
        <#if provider == "linkedin"><@_linkedinSvg /></#if>
    <#else>
        <span class="kc-social-button__icon-fallback" aria-hidden="true">
            <#if displayName?has_content && displayName?length gt 0>
                ${displayName?substring(0,1)?upper_case}
            <#else>
                ${provider?substring(0,1)?upper_case}
            </#if>
        </span>
    </#if>
</#macro>

<#macro spinnerIcon initiallyHidden=true>
<svg class="kc-spinner"
     viewBox="0 0 24 24"
     width="18" height="18"
     fill="none"
     aria-hidden="true"
     focusable="false"
     <#if initiallyHidden>hidden</#if>>
    <circle cx="12" cy="12" r="10"
            stroke="currentColor"
            stroke-width="3"
            opacity="0.25"/>
    <circle cx="12" cy="12" r="10"
            stroke="currentColor"
            stroke-width="3"
            stroke-linecap="round"
            stroke-dasharray="62.83"
            stroke-dashoffset="47.12"/>
</svg>
</#macro>


