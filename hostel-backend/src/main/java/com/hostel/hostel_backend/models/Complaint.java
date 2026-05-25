package com.hostel.hostel_backend.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Data
@Document(collection = "complaints")
public class Complaint {

    @Id
    private String id;

    private String studentId;
    private String studentName;
    private String category;
    private String priority; // LOW, MEDIUM, CRITICAL
    private String description;
    private String assetId;

    private String status; // OPEN, IN_PROGRESS, RESOLVED
    private String assignedTo;
    private Integer rating;

    private LocalDateTime raisedAt = LocalDateTime.now();
    private LocalDateTime resolvedAt;
}