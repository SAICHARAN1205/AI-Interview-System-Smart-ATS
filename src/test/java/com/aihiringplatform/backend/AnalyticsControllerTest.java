package com.aihiringplatform.backend;

import com.aihiringplatform.backend.dto.AnalyticsMetric;
import com.aihiringplatform.backend.dto.AnalyticsPoint;
import com.aihiringplatform.backend.dto.CandidateAnalyticsResponse;
import com.aihiringplatform.backend.dto.RecruiterAnalyticsResponse;
import com.aihiringplatform.backend.service.AnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AnalyticsService analyticsService;

    @Test
    @WithMockUser(username = "recruiter@example.com", roles = {"RECRUITER"})
    void recruiterAnalyticsReturnsStructuredPayload() throws Exception {
        RecruiterAnalyticsResponse response = new RecruiterAnalyticsResponse();
        AnalyticsMetric metric = new AnalyticsMetric();
        metric.setLabel("Total Jobs");
        metric.setValue("4");
        response.setOverview(List.of(metric));

        AnalyticsPoint point = new AnalyticsPoint();
        point.setLabel("May 5");
        point.setValue(9);
        response.setApplicationsOverTime(List.of(point));

        when(analyticsService.getRecruiterAnalytics("recruiter@example.com", false, null, null, null, null, null)).thenReturn(response);

        // GlobalResponseHandler wraps response in ApiResponse{data:...}
        mockMvc.perform(get("/api/analytics/recruiter"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=60")))
                .andExpect(jsonPath("$.data.overview[0].label").value("Total Jobs"))
                .andExpect(jsonPath("$.data.applicationsOverTime[0].value").value(9));
    }

    @Test
    @WithMockUser(username = "candidate@example.com", roles = {"CANDIDATE"})
    void candidateAnalyticsReturnsStructuredPayload() throws Exception {
        CandidateAnalyticsResponse response = new CandidateAnalyticsResponse();
        AnalyticsMetric metric = new AnalyticsMetric();
        metric.setLabel("Average ATS Score");
        metric.setValue("82%");
        response.setOverview(List.of(metric));

        AnalyticsPoint radarPoint = new AnalyticsPoint();
        radarPoint.setLabel("Communication");
        radarPoint.setValue(78);
        response.setRadarMetrics(List.of(radarPoint));

        when(analyticsService.getCandidateAnalytics("candidate@example.com")).thenReturn(response);

        // GlobalResponseHandler wraps response in ApiResponse{data:...}
        mockMvc.perform(get("/api/analytics/candidate"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("max-age=60")))
                .andExpect(jsonPath("$.data.overview[0].label").value("Average ATS Score"))
                .andExpect(jsonPath("$.data.radarMetrics[0].value").value(78));
    }
}
