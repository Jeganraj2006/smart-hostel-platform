package com.hostel.hostel_backend.services;

import opennlp.tools.tokenize.SimpleTokenizer;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NlpTriageService {

    // Maps categories to a list of keywords
    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new HashMap<>();
    // Maps priorities to a list of keywords
    private static final Map<String, List<String>> PRIORITY_KEYWORDS = new HashMap<>();

    static {
        // Category Keywords
        CATEGORY_KEYWORDS.put("ELECTRICAL", Arrays.asList(
            "shock", "shocks", "wire", "wires", "power", "fan", "fans", "light", "lights", 
            "bulb", "bulbs", "switch", "switches", "geyser", "fuse", "plug", "plugs", 
            "electricity", "spark", "sparks", "current", "ac"
        ));
        CATEGORY_KEYWORDS.put("PLUMBING", Arrays.asList(
            "leak", "leaks", "leaking", "water", "tap", "taps", "clog", "clogs", "clogged", "clogging", 
            "pipe", "pipes", "drain", "drains", "shower", "sink", "flush", "toilet", "basin", 
            "drip", "dripping", "sewage", "overflow"
        ));
        CATEGORY_KEYWORDS.put("CLEANLINESS", Arrays.asList(
            "dirt", "dirty", "clean", "garbage", "waste", "dust", "trash", "sweep", "sweeping", 
            "smell", "smells", "smelly", "odor", "pest", "pests", "bug", "bugs", "cockroach", "cockroaches", 
            "mosquito", "mosquitos", "rodent", "rodents", "stain", "stains", "litter"
        ));
        CATEGORY_KEYWORDS.put("FURNITURE", Arrays.asList(
            "chair", "chairs", "table", "tables", "desk", "bed", "cot", "cupboard", "wardrobe", 
            "mirror", "door", "doors", "window", "windows", "lock", "locks", "key", "keys", 
            "handle", "drawer", "shelf", "shelves"
        ));
        CATEGORY_KEYWORDS.put("INTERNET", Arrays.asList(
            "wifi", "internet", "router", "routers", "lan", "network", "speed", 
            "disconnect", "disconnects", "slow", "slowly", "connection", "connections", 
            "port", "ethernet"
        ));

        // Priority Keywords
        PRIORITY_KEYWORDS.put("CRITICAL", Arrays.asList(
            "urgent", "urgently", "fire", "danger", "dangerous", "shock", "shocks", "flood", 
            "spark", "sparks", "emergency", "bleeding", "immediate", "immediately", "broken", 
            "hazard", "blast", "smoke", "burst"
        ));
        PRIORITY_KEYWORDS.put("MEDIUM", Arrays.asList(
            "clog", "clogged", "clogging", "slow", "slowly", "leak", "leaking", "leaks", 
            "smell", "smells", "smelly", "pest", "pests", "dirty", "wifi", "fan", "light", 
            "drip", "dripping", "router", "plug", "lock", "key", "door"
        ));
        PRIORITY_KEYWORDS.put("LOW", Arrays.asList(
            "dust", "chair", "table", "mirror", "drawer", "shelf", "stain", "paint"
        ));
    }

    /**
     * Rule-augmented triage approach using OpenNLP's SimpleTokenizer.
     * Tokenizes description and assigns category based on highest keyword matching scores.
     * Upgradeable to trained OpenNLP DocumentCategorizer when labeled data becomes available.
     */
    public String triageCategory(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "OTHER";
        }

        // Tokenize using OpenNLP SimpleTokenizer
        String[] tokens = SimpleTokenizer.INSTANCE.tokenize(description.toLowerCase());
        Map<String, Integer> categoryScores = new HashMap<>();

        for (String cat : CATEGORY_KEYWORDS.keySet()) {
            categoryScores.put(cat, 0);
        }

        for (String token : tokens) {
            for (Map.Entry<String, List<String>> entry : CATEGORY_KEYWORDS.entrySet()) {
                if (entry.getValue().contains(token)) {
                    categoryScores.put(entry.getKey(), categoryScores.get(entry.getKey()) + 1);
                }
            }
        }

        // Find category with highest score > 0
        String bestCategory = "OTHER";
        int maxScore = 0;
        for (Map.Entry<String, Integer> scoreEntry : categoryScores.entrySet()) {
            if (scoreEntry.getValue() > maxScore) {
                maxScore = scoreEntry.getValue();
                bestCategory = scoreEntry.getKey();
            }
        }

        return bestCategory;
    }

    /**
     * Rule-augmented triage approach using OpenNLP's SimpleTokenizer.
     * Tokenizes description and assigns priority based on highest keyword matching scores.
     * Upgradeable to trained OpenNLP DocumentCategorizer when labeled data becomes available.
     */
    public String triagePriority(String description) {
        if (description == null || description.trim().isEmpty()) {
            return "LOW";
        }

        // Tokenize using OpenNLP SimpleTokenizer
        String[] tokens = SimpleTokenizer.INSTANCE.tokenize(description.toLowerCase());
        Map<String, Integer> priorityScores = new HashMap<>();

        for (String pri : PRIORITY_KEYWORDS.keySet()) {
            priorityScores.put(pri, 0);
        }

        for (String token : tokens) {
            for (Map.Entry<String, List<String>> entry : PRIORITY_KEYWORDS.entrySet()) {
                if (entry.getValue().contains(token)) {
                    priorityScores.put(entry.getKey(), priorityScores.get(entry.getKey()) + 1);
                }
            }
        }

        // Determine priority based on scores. If we have any CRITICAL matching, bias towards CRITICAL.
        if (priorityScores.get("CRITICAL") > 0) {
            return "CRITICAL";
        } else if (priorityScores.get("MEDIUM") > 0) {
            return "MEDIUM";
        } else if (priorityScores.get("LOW") > 0) {
            return "LOW";
        }

        // Default fallback if no keywords matched
        return "LOW";
    }
}
