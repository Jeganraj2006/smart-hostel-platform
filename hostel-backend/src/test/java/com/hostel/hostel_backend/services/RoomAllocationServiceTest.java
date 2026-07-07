package com.hostel.hostel_backend.services;

import com.hostel.hostel_backend.models.RoommatePreferences;
import com.hostel.hostel_backend.models.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class RoomAllocationServiceTest {

    @InjectMocks
    private RoomAllocationService roomAllocationService;

    @Test
    public void testScoringLogicAndOrdering() {
        User baseline = new User();
        RoommatePreferences prefBase = new RoommatePreferences();
        prefBase.setSleepSchedule("EARLY_BIRD");
        prefBase.setCleanlinessLevel(4);
        prefBase.setStudyHabit("SILENT");
        prefBase.setPreferredLanguage("English");
        baseline.setRoommatePreferences(prefBase);

        // Scenario 1: Perfect Match (same sleep, clean=4, study=SILENT, lang=English)
        // Expected score: Sleep(3) + Clean(2) + Study(2) + Lang(1) = 8
        User perfectMatch = new User();
        RoommatePreferences prefPerfect = new RoommatePreferences();
        prefPerfect.setSleepSchedule("EARLY_BIRD");
        prefPerfect.setCleanlinessLevel(4);
        prefPerfect.setStudyHabit("SILENT");
        prefPerfect.setPreferredLanguage("English");
        perfectMatch.setRoommatePreferences(prefPerfect);

        // Scenario 2: Partial Match (same sleep, clean=3 (diff is 1), study=MUSIC_OK, lang=Spanish)
        // Expected score: Sleep(3) + Clean(2) + Study(0) + Lang(0) = 5
        User partialMatch = new User();
        RoommatePreferences prefPartial = new RoommatePreferences();
        prefPartial.setSleepSchedule("EARLY_BIRD");
        prefPartial.setCleanlinessLevel(3);
        prefPartial.setStudyHabit("MUSIC_OK");
        prefPartial.setPreferredLanguage("Spanish");
        partialMatch.setRoommatePreferences(prefPartial);

        // Scenario 3: Low Match (sleep=NIGHT_OWL, clean=1 (diff is 3), study=MUSIC_OK, lang=Spanish)
        // Expected score: Sleep(0) + Clean(0) + Study(0) + Lang(0) = 0
        User lowMatch = new User();
        RoommatePreferences prefLow = new RoommatePreferences();
        prefLow.setSleepSchedule("NIGHT_OWL");
        prefLow.setCleanlinessLevel(1);
        prefLow.setStudyHabit("MUSIC_OK");
        prefLow.setPreferredLanguage("Spanish");
        lowMatch.setRoommatePreferences(prefLow);

        int scorePerfect = roomAllocationService.calculateCompatibilityScore(baseline, perfectMatch);
        int scorePartial = roomAllocationService.calculateCompatibilityScore(baseline, partialMatch);
        int scoreLow = roomAllocationService.calculateCompatibilityScore(baseline, lowMatch);

        // Assert exact scores
        Assertions.assertEquals(8, scorePerfect);
        Assertions.assertEquals(5, scorePartial);
        Assertions.assertEquals(0, scoreLow);

        // Assert expected ordering: Perfect > Partial > Low
        Assertions.assertTrue(scorePerfect > scorePartial, "Perfect match score should be higher than partial match score");
        Assertions.assertTrue(scorePartial > scoreLow, "Partial match score should be higher than low match score");
    }
}
