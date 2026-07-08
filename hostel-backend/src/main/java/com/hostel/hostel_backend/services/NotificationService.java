package com.hostel.hostel_backend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@hostel.com}")
    private String fromEmail;

    public void sendAlert(String to, String message) {
        log.info("Preparing to send email alert to {}: {}", to, message);

        if (mailSender == null) {
            log.warn("JavaMailSender bean is not configured in this context. Falling back to log print: {}", message);
            return;
        }

        try {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setFrom(fromEmail);
            mailMessage.setTo(to);
            mailMessage.setSubject("Hostel Management System Alert");
            mailMessage.setText(message);

            mailSender.send(mailMessage);
            log.info("Email alert successfully sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email alert to {}: {}", to, e.getMessage());
        }
    }
}
