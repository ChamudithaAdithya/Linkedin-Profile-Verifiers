package com.example.SimleaBackendTest.controller;

import com.example.SimleaBackendTest.dto.CookieSubmission;
import com.example.SimleaBackendTest.service.linkedin.LinkedinCookieService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/linkedin/account")
public class LinkedinAccountController {

    private static final Logger log = LoggerFactory.getLogger(LinkedinAccountController.class);

    private final LinkedinCookieService cookieService;

    public LinkedinAccountController(LinkedinCookieService cookieService) {
        this.cookieService = cookieService;
    }

    @PostMapping("/cookies")
    public ResponseEntity<?> submitCookies(@RequestBody CookieSubmission submission) {
        if (submission.getCookies() == null || submission.getCookies().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Cookies JSON is required"));
        }

        var status = cookieService.saveAndValidate(submission.getCookies());
        if (status.valid()) {
            return ResponseEntity.ok(Map.of(
                    "valid", true,
                    "profileName", status.profileName() != null ? status.profileName() : "LinkedIn User"
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "error", "Cookies are invalid or expired. Export fresh cookies from linkedin.com."
            ));
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        var status = cookieService.getStatus();
        return ResponseEntity.ok(Map.of(
                "valid", status.valid(),
                "profileName", status.profileName() != null ? status.profileName() : null
        ));
    }
}
