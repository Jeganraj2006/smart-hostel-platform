package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.models.Room;
import com.hostel.hostel_backend.models.RoommatePreferences;
import com.hostel.hostel_backend.models.User;
import com.hostel.hostel_backend.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RoomAllocationService {

    @Autowired
    private UserRepository userRepository;

    public static class AllocationSuggestion {
        private String roomId;
        private String roomNumber;
        private String blockName;
        private List<User> suggestedStudents;

        public AllocationSuggestion(String roomId, String roomNumber, String blockName, List<User> suggestedStudents) {
            this.roomId = roomId;
            this.roomNumber = roomNumber;
            this.blockName = blockName;
            this.suggestedStudents = suggestedStudents;
        }

        public String getRoomId() { return roomId; }
        public String getRoomNumber() { return roomNumber; }
        public String getBlockName() { return blockName; }
        public List<User> getSuggestedStudents() { return suggestedStudents; }
    }

    /**
     * Computes roommate compatibility score between student1 and student2 based on RoommatePreferences.
     * Rules:
     * - Same sleepSchedule: +3
     * - Same cleanlinessLevel or difference of 1: +2
     * - Same studyHabit: +2
     * - Same preferredLanguage (case-insensitive): +1
     */
    public int calculateCompatibilityScore(User s1, User s2) {
        RoommatePreferences p1 = getPreferencesOrDefault(s1);
        RoommatePreferences p2 = getPreferencesOrDefault(s2);

        int score = 0;

        // 1. Same sleep schedule
        if (p1.getSleepSchedule() != null && p1.getSleepSchedule().equals(p2.getSleepSchedule())) {
            score += 3;
        }

        // 2. Cleanliness level (same or within 1)
        if (p1.getCleanlinessLevel() != null && p2.getCleanlinessLevel() != null) {
            if (Math.abs(p1.getCleanlinessLevel() - p2.getCleanlinessLevel()) <= 1) {
                score += 2;
            }
        }

        // 3. Compatible study habit (same)
        if (p1.getStudyHabit() != null && p1.getStudyHabit().equals(p2.getStudyHabit())) {
            score += 2;
        }

        // 4. Preferred language (case-insensitive)
        if (p1.getPreferredLanguage() != null && p2.getPreferredLanguage() != null) {
            if (p1.getPreferredLanguage().equalsIgnoreCase(p2.getPreferredLanguage())) {
                score += 1;
            }
        }

        return score;
    }

    private RoommatePreferences getPreferencesOrDefault(User user) {
        if (user.getRoommatePreferences() != null) {
            return user.getRoommatePreferences();
        }
        RoommatePreferences defaults = new RoommatePreferences();
        defaults.setSleepSchedule("EARLY_BIRD");
        defaults.setCleanlinessLevel(3);
        defaults.setStudyHabit("SILENT");
        defaults.setPreferredLanguage("English");
        return defaults;
    }

    /**
     * Greedily suggests room allocations for unassigned students in available vacant spaces.
     * This matching algorithm is inspired by the Gale-Shapley stable matching idea.
     * While Gale-Shapley is designed for one-to-one two-sided matching, we adapt the core
     * concept for group rooms: we iteratively fill available rooms by choosing the unassigned
     * student who maximizes the total group compatibility score with existing/already-proposed
     * occupants of that room.
     */
    public List<AllocationSuggestion> suggestAllocation(List<String> unassignedStudentIds, List<Room> availableRooms) {
        List<User> students = userRepository.findAllById(unassignedStudentIds);
        
        // Hide password data
        students.forEach(s -> s.setPassword(null));

        List<User> unassignedPool = new ArrayList<>(students);
        List<AllocationSuggestion> suggestions = new ArrayList<>();

        for (Room room : availableRooms) {
            if (unassignedPool.isEmpty()) {
                break;
            }

            int remainingCapacity = room.getCapacity() - room.getOccupantIds().size();
            if (remainingCapacity <= 0) {
                continue;
            }

            List<User> proposedOccupants = new ArrayList<>();
            // Load existing occupants if any
            for (String occupantId : room.getOccupantIds()) {
                User occupant = userRepository.findById(occupantId).orElse(null);
                if (occupant != null) {
                    proposedOccupants.add(occupant);
                }
            }

            List<User> newlySuggested = new ArrayList<>();

            for (int i = 0; i < remainingCapacity; i++) {
                if (unassignedPool.isEmpty()) {
                    break;
                }

                User bestMatch = null;
                int highestScore = -1;

                for (User candidate : unassignedPool) {
                    int candidateScore = 0;
                    // Compatibility is the sum of compatibility scores with all members currently in the room
                    for (User existing : proposedOccupants) {
                        candidateScore += calculateCompatibilityScore(candidate, existing);
                    }
                    for (User suggested : newlySuggested) {
                        candidateScore += calculateCompatibilityScore(candidate, suggested);
                    }

                    if (candidateScore > highestScore) {
                        highestScore = candidateScore;
                        bestMatch = candidate;
                    }
                }

                if (bestMatch != null) {
                    newlySuggested.add(bestMatch);
                    unassignedPool.remove(bestMatch);
                }
            }

            if (!newlySuggested.isEmpty()) {
                suggestions.add(new AllocationSuggestion(
                        room.getId(),
                        room.getRoomNumber(),
                        room.getBlockName(),
                        newlySuggested
                ));
            }
        }

        return suggestions;
    }
}
