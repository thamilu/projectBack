<#macro registrationLayout displayInfo=false displayMessage=true displayRequiredFields=false>
<!DOCTYPE html>
<html class="${properties.kcHtmlClass!}" lang="${locale.currentLanguageTag}">
<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="robots" content="noindex, nofollow">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">

    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="icon" href="${url.resourcesPath}/img/favicon.ico" />
    <#if properties.stylesCommon?has_content>
        <#list properties.stylesCommon?split(' ') as style>
            <link href="${url.resourcesCommonPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
</head>

<body>
    <div class="premium-wrapper">
        <!-- Brand Side (Visible on Desktop) -->
        <div class="brand-section">
            <div class="brand-content">
                <div class="brand-logo-large"></div>
                <h1 class="brand-title">E-Shop</h1>
                <p class="brand-subtitle">Enterprise Marketplace Ecosystem</p>
                <div class="brand-features">
                    <div class="feature-item">
                        <span>✦</span>
                        <div class="feature-text">
                            <h3>Global Scale Architecture</h3>
                            <p>Distributed low-latency infrastructure</p>
                        </div>
                    </div>
                    <div class="feature-item">
                        <span>✦</span>
                        <div class="feature-text">
                            <h3>Zero-Trust Security</h3>
                            <p>Military-grade identity hardening</p>
                        </div>
                    </div>
                    <div class="feature-item">
                        <span>✦</span>
                        <div class="feature-text">
                            <h3>Ultra-Premium Experience</h3>
                            <p>Precision-engineered glass interfaces</p>
                        </div>
                    </div>
                </div>
            </div>
            <div class="brand-glow-orb"></div>
        </div>

        <!-- Form Side -->
        <div class="form-section">
            <main class="pf-c-login__main">
                <header class="pf-c-login__main-header">
                    <h1 id="kc-page-title"><#nested "header"></h1>
                </header>

                <div id="kc-content">
                    <div id="kc-content-wrapper">
                        <#-- App-level messages -->
                        <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
                            <div class="pf-c-alert pf-m-${(message.type == 'error')?string('danger', message.type)}">
                                <span class="pf-c-alert__icon">
                                    <#if message.type = 'success'><i class="fas fa-check-circle"></i></#if>
                                    <#if message.type = 'warning'><i class="fas fa-exclamation-triangle"></i></#if>
                                    <#if message.type = 'error'><i class="fas fa-exclamation-circle"></i></#if>
                                    <#if message.type = 'info'><i class="fas fa-info-circle"></i></#if>
                                </span>
                                <span class="kc-feedback-text">${kcSanitize(message.summary)?no_esc}</span>
                            </div>
                        </#if>

                        <#nested "form">

                        <#if displayInfo>
                            <div id="kc-info" class="login-info-section">
                                <div id="kc-info-wrapper">
                                    <#nested "info">
                                </div>
                            </div>
                        </#if>
                    </div>
                </div>
            </main>
        </div>
    </div>
    <script>
        function togglePassword(inputId, button) {
            const input = document.getElementById(inputId);
            const icon = button.querySelector('i');
            if (input.type === 'password') {
                input.type = 'text';
                icon.classList.remove('fa-eye');
                icon.classList.add('fa-eye-slash');
            } else {
                input.type = 'password';
                icon.classList.remove('fa-eye-slash');
                icon.classList.add('fa-eye');
            }
        }
    </script>
</body>
</html>
</#macro>
