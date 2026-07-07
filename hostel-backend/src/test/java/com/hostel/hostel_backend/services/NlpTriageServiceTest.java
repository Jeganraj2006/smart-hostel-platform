package com.hostel.hostel_backend.services;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class NlpTriageServiceTest {

    private final NlpTriageService nlpTriageService = new NlpTriageService();

    @Test
    public void testTriageCategoryPlumbing() {
        String desc = "There is a severe water leak from the toilet flush pipe in the common restroom.";
        String category = nlpTriageService.triageCategory(desc);
        assertEquals("PLUMBING", category);
    }

    @Test
    public void testTriageCategoryElectrical() {
        String desc = "The ceiling fan is making sparks and the switch board is warm.";
        String category = nlpTriageService.triageCategory(desc);
        assertEquals("ELECTRICAL", category);
    }

    @Test
    public void testTriageCategoryCleanliness() {
        String desc = "There is cockroach and pest infestation near the garbage bin.";
        String category = nlpTriageService.triageCategory(desc);
        assertEquals("CLEANLINESS", category);
    }

    @Test
    public void testTriageCategoryInternet() {
        String desc = "The wifi network is extremely slow and disconnects frequently.";
        String category = nlpTriageService.triageCategory(desc);
        assertEquals("INTERNET", category);
    }

    @Test
    public void testTriagePriorityCritical() {
        String desc = "Electric wire is sparking causing immediate threat of fire emergency!";
        String priority = nlpTriageService.triagePriority(desc);
        assertEquals("CRITICAL", priority);
    }

    @Test
    public void testTriagePriorityMedium() {
        String desc = "The bathroom tap is dripping and leaking slowly.";
        String priority = nlpTriageService.triagePriority(desc);
        assertEquals("MEDIUM", priority);
    }

    @Test
    public void testTriagePriorityLow() {
        String desc = "There is some dust on the study table.";
        String priority = nlpTriageService.triagePriority(desc);
        assertEquals("LOW", priority);
    }
}
