package com.hostel.hostel_backend.models;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Data
@Document(collection = "rooms")
public class Room {

    @Id
    private String id;

    private String blockName;
    private Integer floor;
    private String roomNumber;
    private Integer capacity;
    private List<String> occupantIds = new ArrayList<>();
    private String roomType; // SINGLE, SHARED
    private String status = "AVAILABLE"; // AVAILABLE, FULL, MAINTENANCE
}
