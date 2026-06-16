package com.aihiringplatform.backend.controller;

import com.aihiringplatform.backend.service.ai.AIGatewayService;
import com.aihiringplatform.backend.service.openrouter.OpenRouterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class OpenRouterDiagnosticController {

    private final OpenRouterService openRouterService;
    private final AIGatewayService gatewayService;

    public OpenRouterDiagnosticController(OpenRouterService openRouterService, AIGatewayService gatewayService) {
        this.openRouterService = openRouterService;
        this.gatewayService = gatewayService;
    }

    @GetMapping("/openrouter")
    public ResponseEntity<?> testOpenRouter() {
        return ResponseEntity.ok(Map.of(
                "providers", gatewayService.getProviderDiagnostics(),
                "openRouter", openRouterService.runConnectivityCheck()
        ));
    }
}
