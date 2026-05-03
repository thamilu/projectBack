package com.eshop.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

@Configuration
public class MailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        // Provide a default JavaMailSenderImpl so application can start in dev without
        // requiring external SMTP configuration. Actual sending will only occur if
        // properties are configured; this bean prevents startup failure when EmailService
        // is present but no mail host is configured.
        return new JavaMailSenderImpl();
    }
}
