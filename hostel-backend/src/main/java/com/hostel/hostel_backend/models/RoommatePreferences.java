package com.hostel.hostel_backend.models;

import lombok.Data;

@Data
public class RoommatePreferences {
    private String sleepSchedule; // "EARLY_BIRD" / "NIGHT_OWL"
    private Integer cleanlinessLevel; // 1-5
    private String studyHabit; // "SILENT" / "MUSIC_OK" / "GROUP_STUDY"
    private String preferredLanguage;
}
