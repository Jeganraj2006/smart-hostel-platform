package com.hostel.hostel_backend.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "visitors")
public class Visitor {

    @Id
    private String id;

    private String visitorName;
    private String visitorPhone;
    private String purpose;
    private String hostStudentId;   // the student being visited
    private String photoUrl;        // optional — set after Cloudinary upload

    // Track who performed check-in (security guard / warden user ID)
    private String approvedBy;

    private LocalDateTime checkInAt  = LocalDateTime.now();
    private LocalDateTime checkOutAt;                         // null until checked-out
}
