package com.hostel.hostel_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String role;
    private String name;
    private String email;
    private String message;
}