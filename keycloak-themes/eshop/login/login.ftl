<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!(messagesPerField.existsError('username') || messagesPerField.existsError('password')) displayInfo=realm.password && realm.registrationAllowed && !registrationDisabled??; section>
    <#if section = "header">
        ${msg("loginAccountTitle")}
        <p class="login-subtext">Secure Enterprise Access</p>
    <#elseif section = "form">
        <div id="kc-form">
          <div id="kc-form-wrapper">
            <#if realm.password>
                <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}" method="post">
                    <div class="pf-c-form__group">
                        <label for="username" class="pf-c-form__label"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if><span class="required">*</span></label>
                        <input tabindex="1" id="username" class="pf-c-form-control" name="username" value="${(login.username!'')}" type="text" autofocus autocomplete="username" spellcheck="false"
                               aria-required="true"
                               aria-invalid="<#if messagesPerField.existsError('username')>true</#if>" />
                    </div>

                    <div class="pf-c-form__group">
                        <label for="password" class="pf-c-form__label">${msg("password")}<span class="required">*</span></label>
                        <div class="password-input-wrapper">
                            <input tabindex="2" id="password" class="pf-c-form-control" name="password" type="password" autocomplete="current-password" spellcheck="false"
                                   aria-required="true"
                                   aria-invalid="<#if messagesPerField.existsError('password')>true</#if>" />
                            <button type="button" class="password-toggle" onclick="togglePassword('password', this)">
                                <i class="fas fa-eye"></i>
                            </button>
                        </div>
                    </div>

                    <div class="pf-c-form__group login-pf-settings">
                        <div id="kc-form-options">
                            <#if realm.rememberMe && !usernameHidden??>
                                <div class="checkbox">
                                    <label>
                                        <#if login.rememberMe??>
                                            <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox" checked> ${msg("rememberMe")}
                                        <#else>
                                            <input tabindex="3" id="rememberMe" name="rememberMe" type="checkbox"> ${msg("rememberMe")}
                                        </#if>
                                    </label>
                                </div>
                            </#if>
                            <#if realm.resetPasswordAllowed>
                                <span><a tabindex="5" href="${url.loginResetCredentialsUrl}">${msg("doForgotPassword")}</a></span>
                            </#if>
                        </div>
                    </div>

                    <div id="kc-form-buttons" class="pf-c-form__group">
                        <input type="hidden" id="id-hidden-input" name="credentialId" <#if auth.selectedCredential?has_content>value="${auth.selectedCredential}"</#if>/>
                        <button tabindex="4" class="pf-c-button pf-m-primary pf-m-block" name="login" id="kc-login" type="submit">${msg("doLogIn")}</button>
                    </div>
                </form>
            </#if>
            </div>
            
            <#if realm.password && social.providers??>
                <div id="kc-social-providers" class="pf-c-login__main-footer-links" style="margin-top: 2.5rem !important;">
                    <span>${msg("identity-provider-login-label")}</span>
                </div>
                <ul class="pf-c-login__main-footer-links-list" style="list-style: none; padding: 0; display: flex; flex-direction: column; gap: 1rem; margin-bottom: 3rem !important;">
                    <#list social.providers as p>
                        <li>
                            <a id="social-${p.alias}" class="pf-c-button pf-m-secondary pf-m-block" type="button" href="${p.loginUrl}">
                                <#if p.iconClasses?has_content>
                                    <i class="${properties.kcCommonLogoIdP!} ${p.iconClasses!}" aria-hidden="true"></i>
                                </#if>
                                <span class="${properties.kcFormSocialAccountNameClass!} kc-social-icon-text">${p.displayName!}</span>
                            </a>
                        </li>
                    </#list>
                </ul>
            </#if>
        </div>
    <#elseif section = "info" >
        <#if realm.password && realm.registrationAllowed && !registrationDisabled??>
            <div id="kc-registration-container">
                <div id="kc-registration">
                    <a tabindex="6" href="${url.registrationUrl}">${msg("noAccount")} ${msg("doRegister")}</a>
                </div>
            </div>
        </#if>
    </#if>
</@layout.registrationLayout>
