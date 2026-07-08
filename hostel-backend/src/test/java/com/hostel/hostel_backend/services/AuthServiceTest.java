package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.dto.RegisterRequest;
import com.hostel.hostel_backend.exceptions.BadRequestException;
import com.hostel.hostel_backend.repositories.UserRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    @Test
    public void testRegisterConsentRequired() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test Student");
        req.setEmail("teststudent@test.com");
        req.setPassword("SecurePass123");
        req.setPhone("1234567890");
        req.setRole("STUDENT");
        req.setConsentGiven(false); // No consent!

        Assertions.assertThrows(BadRequestException.class, () -> {
            authService.register(req);
        });
    }

    @Test
    public void testRegisterConsentAccepted() {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test Student");
        req.setEmail("teststudent@test.com");
        req.setPassword("SecurePass123");
        req.setPhone("1234567890");
        req.setRole("STUDENT");
        req.setConsentGiven(true); // Consent given!

        when(userRepository.existsByEmail("teststudent@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any(CharSequence.class))).thenReturn("encodedPass");

        String res = authService.register(req);
        Assertions.assertTrue(res.contains("Registration request sent"));
    }
}
