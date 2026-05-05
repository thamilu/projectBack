<#import "template.ftl" as layout>
<@layout.registrationLayout displayInfo=true displayMessage=!messagesPerField.existsError('username'); section>
    <#if section = "header">
        ${msg("emailForgotTitle")}
    <#elseif section = "form">
        <form id="kc-reset-password-form" action="${url.loginAction}" method="post">
            <div class="pf-c-form__group">
                <label for="username" class="pf-c-form__label"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>
                <input type="text" id="username" name="username" class="pf-c-form-control" autofocus value="${(auth.attemptedUsername!'')}" aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"/>
            </div>

            <div class="pf-c-form__group">
                <div id="kc-form-buttons">
                    <button class="pf-c-button pf-m-primary pf-m-block" type="submit">${msg("doSubmit")}</button>
                </div>
                <div id="kc-form-options" style="margin-top: 1.5rem; justify-content: center;">
                    <span><a href="${url.loginUrl}">${msg("backToLogin")?no_esc}</a></span>
                </div>
            </div>
        </form>
    <#elseif section = "info" >
        <p style="text-align: center; color: var(--text-muted); font-size: 0.875rem;">
            ${msg("emailInstruction")}
        </p>
    </#if>
</@layout.registrationLayout>
