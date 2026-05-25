package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.dto.AuthResponse;
import com.hostel.hostel_backend.dto.LoginRequest;
import com.hostel.hostel_backend.dto.RegisterRequest;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtTokenProvider jwtTokenProvider;

    // Registration → status = PENDING (warden must approve)
    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());
        user.setRole(request.getRole().toUpperCase());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAccountStatus("PENDING"); // Needs warden approval

        userRepository.save(user);
        return "Registration request sent. Awaiting warden approval.";
    }

    // Login → only ACTIVE accounts
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if ("PENDING".equals(user.getAccountStatus())) {
            throw new RuntimeException(
                    "Your account is pending approval. Please wait for warden verification."
            );
        }

        if ("REJECTED".equals(user.getAccountStatus())) {
            throw new RuntimeException(
                    "Your registration was rejected. Reason: " +
                            (user.getRejectionReason() != null ? user.getRejectionReason() : "Contact warden")
            );
        }

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
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
}