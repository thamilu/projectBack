<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=true; section>
    <#if section = "header">
        ${msg("registerTitle")}
        <p class="login-subtext">Join the E-Shop Ecosystem</p>
    <#elseif section = "form">
        <form id="kc-register-form" action="${url.registrationAction}" method="post">
            <div class="pf-c-form__group">
                <label for="firstName" class="pf-c-form__label">${msg("firstName")}<span class="required">*</span></label>
                <input type="text" id="firstName" class="pf-c-form-control" name="firstName" value="${(register.formData.firstName!'')}" autocomplete="given-name" />
                <#if messagesPerField.existsError('firstName')>
                    <span id="input-error-firstname" class="pf-c-form__helper-text pf-m-error" aria-live="polite">
                        ${kcSanitize(messagesPerField.get('firstName'))?no_esc}
                    </span>
                </#if>
            </div>

            <div class="pf-c-form__group">
                <label for="lastName" class="pf-c-form__label">${msg("lastName")}<span class="required">*</span></label>
                <input type="text" id="lastName" class="pf-c-form-control" name="lastName" value="${(register.formData.lastName!'')}" autocomplete="family-name" />
                <#if messagesPerField.existsError('lastName')>
                    <span id="input-error-lastname" class="pf-c-form__helper-text pf-m-error" aria-live="polite">
                        ${kcSanitize(messagesPerField.get('lastName'))?no_esc}
                    </span>
                </#if>
            </div>

            <div class="pf-c-form__group">
                <label for="email" class="pf-c-form__label">${msg("email")}<span class="required">*</span></label>
                <input type="text" id="email" class="pf-c-form-control" name="email" value="${(register.formData.email!'')}" autocomplete="email" />
                <#if messagesPerField.existsError('email')>
                    <span id="input-error-email" class="pf-c-form__helper-text pf-m-error" aria-live="polite">
                        ${kcSanitize(messagesPerField.get('email'))?no_esc}
                    </span>
                </#if>
            </div>

            <#if !realm.registrationEmailAsUsername>
                <div class="pf-c-form__group">
                    <label for="username" class="pf-c-form__label">${msg("username")}<span class="required">*</span></label>
                    <input type="text" id="username" class="pf-c-form-control" name="username" value="${(register.formData.username!'')}" autocomplete="username" />
                </div>
            </#if>

            <#if passwordRequired??>
                <div class="pf-c-form__group">
                    <label for="password" class="pf-c-form__label">${msg("password")}<span class="required">*</span></label>
                    <div class="password-input-wrapper">
                        <input type="password" id="password" class="pf-c-form-control" name="password" autocomplete="new-password" />
                        <button type="button" class="password-toggle" onclick="togglePassword('password', this)">
                            <i class="fas fa-eye"></i>
                        </button>
                    </div>
                    <#if messagesPerField.existsError('password')>
                        <span id="input-error-password" class="pf-c-form__helper-text pf-m-error" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('password'))?no_esc}
                        </span>
                    </#if>
                </div>

                <div class="pf-c-form__group">
                    <label for="password-confirm" class="pf-c-form__label">${msg("passwordConfirm")}<span class="required">*</span></label>
                    <div class="password-input-wrapper">
                        <input type="password" id="password-confirm" class="pf-c-form-control" name="password-confirm" />
                        <button type="button" class="password-toggle" onclick="togglePassword('password-confirm', this)">
                            <i class="fas fa-eye"></i>
                        </button>
                    </div>
                    <#if messagesPerField.existsError('password-confirm')>
                        <span id="input-error-password-confirm" class="pf-c-form__helper-text pf-m-error" aria-live="polite">
                            ${kcSanitize(messagesPerField.get('password-confirm'))?no_esc}
                        </span>
                    </#if>
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
