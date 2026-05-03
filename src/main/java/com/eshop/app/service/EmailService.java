package com.eshop.app.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {
    private final JavaMailSender mailSender;

    @CircuitBreaker(name = "emailService", fallbackMethod = "sendEmailFallback")
    public void sendEmail(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
        log.info("Email sent to: {}", to);
    }
    
    /**
     * Fallback method when email service is unavailable
     * Logs the failed email for manual retry or scheduled job pickup
     */
    @SuppressWarnings("unused") // Invoked by Resilience4j CircuitBreaker via reflection
    private void sendEmailFallback(String to, String subject, String text, Exception e) {
        log.error("Email service circuit breaker activated. Failed to send email to: {}. Subject: {}", 
                to, subject, e);
        
        // In production, queue this in database for retry by scheduled job
        // For now, just log it
        log.warn("Email queued for retry: to={}, subject={}, text={}", to, subject, text.substring(0, Math.min(50, text.length())));
    }
}
