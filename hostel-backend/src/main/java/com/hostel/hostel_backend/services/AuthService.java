package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.dto.AuthResponse;
import com.hostel.hostel_backend.dto.LoginRequest;
import com.hostel.hostel_backend.dto.RegisterRequest;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.security.JwtTokenProvider;
import com.hostel.hostel_backend.exceptions.BadRequestException;
import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private StringRedisTemplate redisTemplate;

    // Registration → status = PENDING (warden must approve)
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        String role = request.getRole() != null ? request.getRole().toUpperCase() : "";
        if (!role.equals("STUDENT") && !role.equals("WARDEN") && !role.equals("ADMIN") &&
            !role.equals("HOD") && !role.equals("PARENT") && !role.equals("SECURITY_GUARD")) {
            throw new BadRequestException("Invalid role. Allowed roles are: STUDENT, WARDEN, ADMIN, HOD, PARENT, SECURITY_GUARD");
        }

        String password = request.getPassword();
        if (password == null || !password.matches("^(?=.*[a-zA-Z])(?=.*\\d).{8,}$")) {
            throw new BadRequestException("Password must be at least 8 characters long and contain both letters and numbers.");
        }

        // India's Digital Personal Data Protection (DPDP) Act 2023 compliance:
        // Section 6 mandates that personal data must be processed only for a specified purpose 
        // for which the Data Principal has given, or is deemed to have given, their consent.
        // Thus, we enforce explicit consent collection and log the timestamp.
        if (request.getConsentGiven() == null || !request.getConsentGiven()) {
            throw new BadRequestException("Explicit consent to collect and process personal data is required under the DPDP Act 2023.");
        }

        User user = new User();
        user.setConsentGivenAt(java.time.LocalDateTime.now());
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(password));
        user.setAccountStatus("PENDING"); // Needs warden approval

        if ("PARENT".equals(role)) {
            String claim = request.getChildEmailOrId();
            if (claim == null || claim.trim().isEmpty()) {
                throw new BadRequestException("Child's email or student ID is required for PARENT registration");
            }
            user.setChildEmailOrId(claim.trim());
            
            // Try resolving the student ID/Email to a real user
            java.util.Optional<User> studentOpt = userRepository.findById(claim.trim());
            if (studentOpt.isEmpty()) {
                studentOpt = userRepository.findByEmail(claim.trim());
            }
            if (studentOpt.isPresent() && "STUDENT".equals(studentOpt.get().getRole())) {
                user.setLinkedStudentId(studentOpt.get().getId());
            } else {
                user.setLinkedStudentId(claim.trim());
            }
        }

        userRepository.save(user);
        return "Registration request sent. Awaiting warden approval.";
    }
    // Login → only ACTIVE accounts
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail();
        String lockoutKey = "login:lockout:" + email;
        try {
            if (Boolean.TRUE.equals(redisTemplate.hasKey(lockoutKey))) {
                throw new BadRequestException("Account temporarily locked due to too many failed attempts. Please try again later.");
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis unavailable, skipping lockout check");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if ("PENDING".equals(user.getAccountStatus())) {
            throw new BadRequestException(
                    "Your account is pending approval. Please wait for warden verification."
            );
        }

        if ("REJECTED".equals(user.getAccountStatus())) {
            throw new BadRequestException(
                    "Your registration was rejected. Reason: " +
                            (user.getRejectionReason() != null ? user.getRejectionReason() : "Contact warden")
            );
        }

        if (!user.isActive()) {
            throw new BadRequestException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            incrementFailedAttempts(email);
            throw new BadRequestException("Invalid password");
        }

        // Login success: reset attempts
        try {
            redisTemplate.delete("login:attempts:" + email);
            redisTemplate.delete(lockoutKey);
        } catch (Exception e) {
            log.warn("Redis unavailable, failed to reset login attempts");
        }

        String token = jwtTokenProvider.generateAccessToken(
                user.getEmail(), user.getRole()
        );

        return new AuthResponse(
                token, user.getRole(),
                user.getName(), user.getEmail(),
                "Login successful"
        );
    }

    private void incrementFailedAttempts(String email) {
        String attemptsKey = "login:attempts:" + email;
        String lockoutKey = "login:lockout:" + email;

        try {
            String currentVal = redisTemplate.opsForValue().get(attemptsKey);
            int attempts = 0;
            if (currentVal != null) {
                try {
                    attempts = Integer.parseInt(currentVal);
                } catch (NumberFormatException e) {
                    // ignore
                }
            }

            attempts++;
            if (attempts >= 5) {
                // Lockout for 15 minutes
                redisTemplate.opsForValue().set(lockoutKey, "locked", 15, TimeUnit.MINUTES);
                redisTemplate.delete(attemptsKey);
                throw new BadRequestException("Account temporarily locked due to too many failed attempts. Please try again later.");
            } else {
                // Save attempt count with 15 minutes TTL
                redisTemplate.opsForValue().set(attemptsKey, String.valueOf(attempts), 15, TimeUnit.MINUTES);
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis unavailable, failed to increment login attempts");
        }
    }
}