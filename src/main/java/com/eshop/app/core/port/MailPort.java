package com.eshop.app.core.port;

import java.util.Map;

/**
 * [HARDEN] Mail Port — outbound port for email delivery.
 *
 * Decouples the application from specific mail providers (SMTP, SendGrid, SES).
 * All modules that send email MUST depend on this port, never the provider directly.
 *
 * Design:
 * - Simple fire-and-forget send operations
 * - Template-based sending for HTML emails
 * - Bulk send support for notifications
 */
public interface MailPort {

    /**
     * Sends a plain text email.
     *
     * @param to      recipient email address
     * @param subject email subject line
     * @param body    plain text body
     */
    void sendText(String to, String subject, String body);

    /**
     * Sends an HTML email using a named template.
     *
     * @param to           recipient email address
     * @param subject      email subject line
     * @param templateName template identifier (e.g., "order-confirmation")
     * @param variables    template variables map
     */
    void sendTemplate(String to, String subject, String templateName, Map<String, Object> variables);

    /**
     * Sends an HTML email with raw HTML body.
     *
     * @param to      recipient email address
     * @param subject email subject line
     * @param html    the raw HTML body
     */
    void sendHtml(String to, String subject, String html);
}
