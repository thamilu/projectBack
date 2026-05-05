<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('firstName','lastName','email','username','password','password-confirm'); section>
    <#if section = "header">
        ${msg("registerTitle")}
        <p class="login-subtext">Join the E-Shop Ecosystem</p>
    <#elseif section = "form">
        <form id="kc-register-form" action="${url.registrationAction}" method="post">
            <div class="pf-c-form__group">
                <label for="firstName" class="pf-c-form__label">${msg("firstName")}</label>
                <input type="text" id="firstName" class="pf-c-form-control" name="firstName" value="${(register.formData.firstName!'')}" autocomplete="given-name" />
            </div>

            <div class="pf-c-form__group">
                <label for="lastName" class="pf-c-form__label">${msg("lastName")}</label>
                <input type="text" id="lastName" class="pf-c-form-control" name="lastName" value="${(register.formData.lastName!'')}" autocomplete="family-name" />
            </div>

            <div class="pf-c-form__group">
                <label for="email" class="pf-c-form__label">${msg("email")}</label>
                <input type="text" id="email" class="pf-c-form-control" name="email" value="${(register.formData.email!'')}" autocomplete="email" />
            </div>

            <#if !realm.registrationEmailAsUsername>
                <div class="pf-c-form__group">
                    <label for="username" class="pf-c-form__label">${msg("username")}</label>
                    <input type="text" id="username" class="pf-c-form-control" name="username" value="${(register.formData.username!'')}" autocomplete="username" />
                </div>
            </#if>

            <#if passwordRequired??>
                <div class="pf-c-form__group">
                    <label for="password" class="pf-c-form__label">${msg("password")}</label>
                    <input type="password" id="password" class="pf-c-form-control" name="password" autocomplete="new-password" />
            </div>

                <div class="pf-c-form__group">
                    <label for="password-confirm" class="pf-c-form__label">${msg("passwordConfirm")}</label>
                    <input type="password" id="password-confirm" class="pf-c-form-control" name="password-confirm" />
            </div>
            </#if>

            <div class="pf-c-form__group">
                <div id="kc-form-buttons">
                    <button class="pf-c-button pf-m-primary pf-m-block" type="submit">${msg("doRegister")}</button>
                </div>
                <div id="kc-form-options" style="margin-top: 2rem;">
                    <a href="${url.loginUrl}" class="back-to-login-link">Already have an account? ${msg("backToLogin")?no_esc}</a>
                </div>
            </div>
        </form>
    </#if>
</@layout.registrationLayout>
