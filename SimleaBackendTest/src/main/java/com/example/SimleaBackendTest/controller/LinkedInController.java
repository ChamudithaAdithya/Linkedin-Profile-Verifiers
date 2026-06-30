package com.example.SimleaBackendTest.controller;

import com.example.SimleaBackendTest.dto.LinkedInUserInfo;
import com.example.SimleaBackendTest.service.LinkedInService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/linkedin")
public class LinkedInController {

    private static final Logger log = LoggerFactory.getLogger(LinkedInController.class);

    private final LinkedInService linkedInService;

    public LinkedInController(LinkedInService linkedInService) {
        this.linkedInService = linkedInService;
    }

    @GetMapping("/url")
    public ResponseEntity<Map<String, String>> getAuthorizationUrl() {
        String url = linkedInService.generateAuthorizationUrl();
        return ResponseEntity.ok(Map.of("url", url));
    }

    @PostMapping("/callback")
    public ResponseEntity<?> callback(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String state = request.get("state");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Missing authorization code"));
        }
        try {
            LinkedInUserInfo userInfo = linkedInService.authenticate(code, state);
            return ResponseEntity.ok(userInfo);
        } catch (Exception e) {
            log.error("LinkedIn authentication failed", e);
            return ResponseEntity.status(502).body(Map.of("error", e.getMessage()));
        }
    }
}
