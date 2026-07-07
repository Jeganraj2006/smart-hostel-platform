package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.exceptions.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/setup")
@CrossOrigin(origins = "*")
public class SetupController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // Direct user creation — for initial setup only
    // Creates ACTIVE accounts without warden approval
    @PostMapping("/create-user")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already exists");
        }

        String role = body.get("role") != null ? body.get("role").toUpperCase() : "";
        if (!role.equals("STUDENT") && !role.equals("WARDEN") && !role.equals("ADMIN") &&
            !role.equals("HOD") && !role.equals("PARENT") && !role.equals("SECURITY_GUARD")) {
            throw new BadRequestException("Invalid role. Allowed roles are: STUDENT, WARDEN, ADMIN, HOD, PARENT, SECURITY_GUARD");
        }

        User user = new User();
        user.setName(body.get("name"));
        user.setEmail(email);
        user.setPhone(body.get("phone"));
        user.setRole(role);
        user.setPassword(passwordEncoder.encode(body.get("password")));
        user.setAccountStatus("ACTIVE"); // Directly active — no approval needed

        userRepository.save(user);
        return ResponseEntity.ok("User created: " + user.getName() + " (" + user.getRole() + ")");
    }
}