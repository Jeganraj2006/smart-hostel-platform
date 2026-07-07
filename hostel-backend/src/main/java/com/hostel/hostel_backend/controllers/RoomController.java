package com.hostel.hostel_backend.controllers;

import com.hostel.hostel_backend.models.Room;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.RoomRepository;
import com.hostel.hostel_backend.repositories.UserRepository;
import com.hostel.hostel_backend.exceptions.BadRequestException;
import com.hostel.hostel_backend.exceptions.ResourceNotFoundException;
import com.hostel.hostel_backend.services.AuditService;
import com.hostel.hostel_backend.services.RoomAllocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*")
public class RoomController {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private RoomAllocationService roomAllocationService;

    public static class ApplyAllocationRequest {
        private String roomId;
        private List<String> studentIds;

        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }
        public List<String> getStudentIds() { return studentIds; }
        public void setStudentIds(List<String> studentIds) { this.studentIds = studentIds; }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Room> createRoom(@RequestBody Room room) {
        if (room.getOccupantIds() == null) {
            room.setOccupantIds(new java.util.ArrayList<>());
        }
        if (room.getStatus() == null) {
            room.setStatus("AVAILABLE");
        }
        Room saved = roomRepository.save(room);

        // Audit Log
        User actor = getCurrentUser();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("roomNumber", saved.getRoomNumber());
        metadata.put("blockName", saved.getBlockName());
        auditService.log(
            actor.getId(),
            actor.getRole(),
            "CREATE",
            "ROOM",
            saved.getId(),
            metadata
        );

        return ResponseEntity.ok(saved);
    }

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        return ResponseEntity.ok(roomRepository.findAll());
    }

    @GetMapping("/available")
    public ResponseEntity<List<Room>> getAvailableRooms() {
        return ResponseEntity.ok(roomRepository.findByStatus("AVAILABLE"));
    }

    @GetMapping("/allocation-suggestions")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<?> getAllocationSuggestions() {
        List<Room> allRooms = roomRepository.findAll();
        Set<String> assignedStudentIds = allRooms.stream()
                .flatMap(r -> r.getOccupantIds().stream())
                .collect(Collectors.toSet());

        List<User> activeStudents = userRepository.findByRole("STUDENT");
        List<String> unassignedStudentIds = activeStudents.stream()
                .filter(u -> "ACTIVE".equals(u.getAccountStatus()))
                .map(User::getId)
                .filter(id -> !assignedStudentIds.contains(id))
                .collect(Collectors.toList());

        List<Room> availableRooms = allRooms.stream()
                .filter(r -> !"MAINTENANCE".equals(r.getStatus()))
                .filter(r -> r.getOccupantIds().size() < r.getCapacity())
                .collect(Collectors.toList());

        List<RoomAllocationService.AllocationSuggestion> suggestions =
                roomAllocationService.suggestAllocation(unassignedStudentIds, availableRooms);

        return ResponseEntity.ok(suggestions);
    }

    @PostMapping("/allocation-suggestions/apply")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<?> applyAllocationSuggestions(@RequestBody List<ApplyAllocationRequest> requests) {
        User actor = getCurrentUser();

        for (ApplyAllocationRequest req : requests) {
            Room room = roomRepository.findById(req.getRoomId())
                    .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + req.getRoomId()));

            for (String studentId : req.getStudentIds()) {
                // Verify student
                User student = userRepository.findById(studentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

                if (room.getOccupantIds().contains(studentId)) {
                    continue;
                }

                if (room.getOccupantIds().size() >= room.getCapacity()) {
                    throw new BadRequestException("Room capacity exceeded for room: " + room.getRoomNumber());
                }

                room.getOccupantIds().add(studentId);

                // Audit Log
                Map<String, String> metadata = new HashMap<>();
                metadata.put("studentId", studentId);
                metadata.put("studentName", student.getName());
                auditService.log(
                    actor.getId(),
                    actor.getRole(),
                    "ASSIGN_STUDENT",
                    "ROOM",
                    room.getId(),
                    metadata
                );
            }

            if (room.getOccupantIds().size() >= room.getCapacity()) {
                room.setStatus("FULL");
            } else {
                room.setStatus("AVAILABLE");
            }

            roomRepository.save(room);
        }

        return ResponseEntity.ok(Map.of("message", "Allocation suggestions applied successfully"));
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<Room> assignStudent(@PathVariable String id,
                                              @RequestBody Map<String, String> body) {
        String studentId = body.get("studentId");
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new BadRequestException("studentId is required");
        }

        // Verify student exists
        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found with id: " + studentId));

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));

        if (!"AVAILABLE".equals(room.getStatus())) {
            throw new BadRequestException("Room is not available for assignment. Status: " + room.getStatus());
        }

        if (room.getOccupantIds().contains(studentId)) {
            throw new BadRequestException("Student is already assigned to this room");
        }

        if (room.getOccupantIds().size() >= room.getCapacity()) {
            throw new BadRequestException("Room capacity exceeded");
        }

        room.getOccupantIds().add(studentId);
        if (room.getOccupantIds().size() >= room.getCapacity()) {
            room.setStatus("FULL");
        }

        Room saved = roomRepository.save(room);

        // Audit Log
        User actor = getCurrentUser();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("studentId", studentId);
        metadata.put("studentName", student.getName());
        auditService.log(
            actor.getId(),
            actor.getRole(),
            "ASSIGN_STUDENT",
            "ROOM",
            id,
            metadata
        );

        return ResponseEntity.ok(saved);
    }

    @PutMapping("/{id}/unassign")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<Room> unassignStudent(@PathVariable String id,
                                                @RequestBody Map<String, String> body) {
        String studentId = body.get("studentId");
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new BadRequestException("studentId is required");
        }

        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found with id: " + id));

        if (!room.getOccupantIds().contains(studentId)) {
            throw new BadRequestException("Student is not assigned to this room");
        }

        room.getOccupantIds().remove(studentId);
        if (room.getOccupantIds().size() < room.getCapacity()) {
            room.setStatus("AVAILABLE");
        }

        Room saved = roomRepository.save(room);

        // Audit Log
        User actor = getCurrentUser();
        Map<String, String> metadata = new HashMap<>();
        metadata.put("studentId", studentId);
        auditService.log(
            actor.getId(),
            actor.getRole(),
            "UNASSIGN_STUDENT",
            "ROOM",
            id,
            metadata
        );

        return ResponseEntity.ok(saved);
    }
}
