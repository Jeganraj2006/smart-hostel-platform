package com.hostel.hostel_backend.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private String phone;
    private String role;
    private String childEmailOrId;
    private Boolean consentGiven;
}