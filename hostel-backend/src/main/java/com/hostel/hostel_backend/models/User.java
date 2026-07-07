package com.hostel.hostel_backend.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "users")
public class User {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    private String password;
    private String name;
    private String phone;
    private String role;

    // PENDING = waiting warden approval
    // ACTIVE  = can login
    // REJECTED = denied by warden
    private String accountStatus = "PENDING";

    private boolean isActive = true;
    private String rejectionReason;
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime approvedAt;
    private RoommatePreferences roommatePreferences;
}