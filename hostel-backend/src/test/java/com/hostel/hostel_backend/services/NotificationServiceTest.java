package com.hostel.hostel_backend.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private JavaMailSender mailSender;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(notificationService, "fromEmail", "noreply@hostel.com");
    }

    @Test
    public void testSendAlertSuccess() {
        notificationService.sendAlert("student@test.com", "Test alert message");

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }

    @Test
    public void testSendAlertNoMailSenderConfigured() {
        // Force mailSender to null
        ReflectionTestUtils.setField(notificationService, "mailSender", null);

        assertDoesNotThrow(() -> {
            notificationService.sendAlert("student@test.com", "Test alert message");
        });
    }

    @Test
    public void testSendAlertMailSenderExceptionHandled() {
        doThrow(new RuntimeException("SMTP connection timed out"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // The exception should be caught inside sendAlert and logged, not thrown to caller.
        assertDoesNotThrow(() -> {
            notificationService.sendAlert("student@test.com", "Test alert message");
        });

        verify(mailSender, times(1)).send(any(SimpleMailMessage.class));
    }
}
