package com.aihiringplatform.backend;

import com.aihiringplatform.backend.service.openrouter.OpenRouterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OpenRouterDiagnosticControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OpenRouterService openRouterService;

    @Test
    void testOpenRouterDiagnosticEndpoint() throws Exception {
        when(openRouterService.runConnectivityCheck()).thenReturn(
                new OpenRouterService.OpenRouterConnectivityResult(
                        true,
                        true,
                        "https://openrouter.ai",
                        "http://localhost:8080",
                        "SmartATS",
                        List.of("deepseek/deepseek-chat-v3-0324:free", "qwen/qwen-2.5-72b-instruct:free"),
                        List.of(new OpenRouterService.OpenRouterAttemptResult(
                                "deepseek/deepseek-v4-flash:free",
                                200,
                                true,
                                "Success",
                                "{\"status\":\"ok\"}"
                        )),
                        true,
                        "OpenRouter connectivity check succeeded.",
                        "{\"status\":\"ok\"}"
                )
        );

        mockMvc.perform(get("/api/test/openrouter"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.openRouter.success").value(true))
                .andExpect(jsonPath("$.data.openRouter.httpReferer").value("http://localhost:8080"))
                .andExpect(jsonPath("$.data.openRouter.attempts[0].model").value("deepseek/deepseek-v4-flash:free"))
                .andExpect(jsonPath("$.data.providers").isArray());
    }
}
