# System Architecture & Modules Diagram

This document details the modular layout and data flow design of the Smart Hostel Platform.

---

## Module Connections Layout

```
                  ┌──────────────────────────────┐
                  │                              │
                  │   Frontend: React / Vite     │
                  │                              │
                  └──────────────┬───────────────┘
                                 │
                                 │ HTTP REST (JSON)
                                 ▼
                  ┌──────────────────────────────┐
                  │                              │
                  │    Backend: Spring Boot      │
                  │                              │
                  └──────────────┬───────────────┘
                                 │
         ┌───────────────────────┼───────────────────────┐
         ▼                       ▼                       ▼
┌──────────────────┐    ┌──────────────────┐    ┌──────────────────┐
│   Controllers    │    │     Services     │    │   Repositories   │
├──────────────────┤    ├──────────────────┤    ├──────────────────┤
│ Auth & Setup     │    │ Room Allocator   │    │ User (Mongo)     │
│ Student / Parent │    │ NLP Triage       │    │ Leave (Mongo)    │
│ WARDEN / HOD     │    │ Fee Risk Score   │    │ Complaint (Mongo)│
│ Resources / Gate │    │ SMTP Mail Alert  │    │ Resource (Mongo) │
└──────────────────┘    └──────────────────┘    └──────────────────┘
                                 │
                        ┌────────┴────────┐
                        ▼                 ▼
             ┌────────────────────┐    ┌────────────────────┐
             │  MongoDB Database  │    │   Redis Caching    │
             │   (Data Store)     │    │  (Rate Limiting)   │
             └────────────────────┘    └────────────────────┘
```

---

## Core Components Workflow

1. **Gatepass Authentication & Exit Logs**:
   * Student requests leave -> `LeaveService` creates approval steps and stores them in MongoDB.
   * On final approval, a QR code base64 payload is computed.
   * `Security Guard` scans code -> `AttendanceController` logs entry/exit records in MongoDB.
2. **Preventive Maintenance**:
   * Student raises complaint -> `NlpTriageService` updates category.
   * `PreventiveMaintenanceService` checks matching counts for room asset. If count matches 3 inside 60 days, a maintenance flag is logged.
3. **Parent Trust Timeline**:
   * Parent requests chronological timeline -> `ParentController` performs database query joins across Leaves, Fees, Complaints, and Attendance collections.
   * Sorts the unified list in descending chronological order and returns it.
