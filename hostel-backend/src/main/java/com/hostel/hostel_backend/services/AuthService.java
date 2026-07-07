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

import java.util.concurrent.TimeUnit;

@Service
public class AuthService {

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

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(password));
        user.setAccountStatus("PENDING"); // Needs warden approval

        userRepository.save(user);
        return "Registration request sent. Awaiting warden approval.";
    }

    // Login → only ACTIVE accounts
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail();
        String lockoutKey = "login:lockout:" + email;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(lockoutKey))) {
            throw new BadRequestException("Account temporarily locked due to too many failed attempts. Please try again later.");
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
        redisTemplate.delete("login:attempts:" + email);
        redisTemplate.delete(lockoutKey);

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
    }
}