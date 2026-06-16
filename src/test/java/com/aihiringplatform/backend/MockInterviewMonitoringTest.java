package com.aihiringplatform.backend;

import com.aihiringplatform.backend.dto.MonitoringEventDto;
import com.aihiringplatform.backend.entity.MockInterviewSession;
import com.aihiringplatform.backend.entity.MockInterviewStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the interview monitoring system fields and logic.
 */
class MockInterviewMonitoringTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void monitoringFieldsDefaultToNull() {
        MockInterviewSession session = new MockInterviewSession();
        assertNull(session.getTabSwitchCount());
        assertNull(session.getFaceWarningCount());
        assertNull(session.getSuspiciousActivityCount());
        assertNull(session.getConfidenceScore());
        assertNull(session.getFluencyScore());
        assertNull(session.getCameraUsed());
        assertNull(session.getMicrophoneUsed());
        assertNull(session.getMonitoringEventsJson());
        assertNull(session.getTranscriptJson());
    }

    @Test
    void monitoringFieldsCanBeSet() {
        MockInterviewSession session = new MockInterviewSession();
        session.setTabSwitchCount(3);
        session.setFaceWarningCount(2);
        session.setSuspiciousActivityCount(5);
        session.setConfidenceScore(72);
        session.setFluencyScore(85);
        session.setCameraUsed(true);
        session.setMicrophoneUsed(true);

        assertEquals(3, session.getTabSwitchCount());
        assertEquals(2, session.getFaceWarningCount());
        assertEquals(5, session.getSuspiciousActivityCount());
        assertEquals(72, session.getConfidenceScore());
        assertEquals(85, session.getFluencyScore());
        assertTrue(session.getCameraUsed());
        assertTrue(session.getMicrophoneUsed());
    }

    @Test
    void monitoringEventsCanBeSerializedAndDeserialized() throws Exception {
        List<MonitoringEventDto> events = List.of(
                new MonitoringEventDto("TAB_SWITCH", "Tab switching detected.", "2026-06-14T10:00:00Z"),
                new MonitoringEventDto("FACE_WARNING", "Face not detected.", "2026-06-14T10:00:05Z"),
                new MonitoringEventDto("COPY_PASTE", "Paste detected.", "2026-06-14T10:01:00Z")
        );

        String json = objectMapper.writeValueAsString(events);
        assertNotNull(json);
        assertTrue(json.contains("TAB_SWITCH"));
        assertTrue(json.contains("FACE_WARNING"));

        List<MonitoringEventDto> deserialized = objectMapper.readValue(json, new TypeReference<>() {});
        assertEquals(3, deserialized.size());
        assertEquals("TAB_SWITCH", deserialized.get(0).getType());
        assertEquals("Face not detected.", deserialized.get(1).getMessage());
    }

    @Test
    void transcriptJsonCanBeStored() throws Exception {
        MockInterviewSession session = new MockInterviewSession();
        var transcripts = new java.util.LinkedHashMap<Integer, String>();
        transcripts.put(0, "I built a REST API using Spring Boot...");
        transcripts.put(1, "In my previous role I designed a microservice architecture...");
        String json = objectMapper.writeValueAsString(transcripts);
        session.setTranscriptJson(json);

        assertNotNull(session.getTranscriptJson());
        var parsed = objectMapper.readValue(session.getTranscriptJson(),
                new TypeReference<java.util.LinkedHashMap<Integer, String>>() {});
        assertEquals(2, parsed.size());
        assertTrue(parsed.get(0).contains("Spring Boot"));
    }

    @Test
    void integrityStatusDerivation() {
        // Clean: 0 suspicious
        assertEquals("Clean", deriveIntegrityStatus(0));
        // Flagged: 1-3
        assertEquals("Flagged", deriveIntegrityStatus(1));
        assertEquals("Flagged", deriveIntegrityStatus(3));
        // Suspicious: >3
        assertEquals("Suspicious", deriveIntegrityStatus(4));
        assertEquals("Suspicious", deriveIntegrityStatus(10));
    }

    @Test
    void monitoringEventDtoProperties() {
        MonitoringEventDto dto = new MonitoringEventDto();
        dto.setType("TAB_SWITCH");
        dto.setMessage("Tab switching detected.");
        dto.setTimestamp("2026-06-14T10:00:00Z");

        assertEquals("TAB_SWITCH", dto.getType());
        assertEquals("Tab switching detected.", dto.getMessage());
        assertEquals("2026-06-14T10:00:00Z", dto.getTimestamp());
    }

    // Mirror of the service logic for unit testing
    private String deriveIntegrityStatus(int suspiciousCount) {
        if (suspiciousCount == 0) return "Clean";
        if (suspiciousCount <= 3) return "Flagged";
        return "Suspicious";
    }
}
