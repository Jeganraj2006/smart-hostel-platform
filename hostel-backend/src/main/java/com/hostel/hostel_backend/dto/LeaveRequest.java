package com.hostel.hostel_backend.dto;

import lombok.Data;

@Data
public class LeaveRequest {
    private String leaveType;
    private String fromDate;
    private String toDate;
    private String returnTime;
    private String reason;
    private String destination;
}