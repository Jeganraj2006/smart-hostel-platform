package com.hostel.hostel_backend.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Document(collection = "preventive_flags")
public class PreventiveFlag {
    @Id
    private String id;

    private String assetId;
    private String category;
    private long complaintCount;
    private LocalDateTime flaggedAt = LocalDateTime.now();
    private boolean resolved = false;
}
