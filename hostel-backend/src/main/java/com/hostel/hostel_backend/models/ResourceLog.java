package com.hostel.hostel_backend.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "resource_logs")
public class ResourceLog {

    @Id
    private String id;

    private String date; // YYYY-MM-DD
    private String resourceType; // ELECTRICITY, WATER, MESS_WASTAGE
    private String blockName;
    private Double quantity;
    private String unit;
    private String recordedBy;
}
