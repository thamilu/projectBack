<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=false; section>
    <#if section = "header">
        ${msg("termsTitle")}
        <p class="login-subtext">Review our commitment to your security</p>
    <#elseif section = "form">
        <div id="kc-terms-text">
            ${msg("termsText")?no_esc}
        </div>
        <form class="form-actions" action="${url.loginAction}" method="POST">
            <div id="kc-form-buttons" class="pf-c-form__group terms-buttons">
                <button class="pf-c-button pf-m-primary pf-m-block" name="accept" id="kc-accept" type="submit">
                    ${msg("doAccept")}
                </button>
                <button class="pf-c-button pf-m-secondary pf-m-block" name="cancel" id="kc-decline" type="submit">
                    ${msg("doDecline")}
                </button>
            </div>
        </form>
        <div class="clearfix"></div>
    </#if>
</@layout.registrationLayout>
