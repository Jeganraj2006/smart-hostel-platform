package com.hostel.hostel_backend.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String password;
}