package com.hostel.hostel_backend.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    public void sendAlert(String to, String message) {
        log.info("Sending overdue alert email to {}: {}", to, message);
    }
}
