package com.example.SimleaBackendTest.service.linkedin;

import com.example.SimleaBackendTest.entity.LinkedinAccount;
import com.example.SimleaBackendTest.repository.LinkedinAccountRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LinkedinCookieService {

    private static final Logger log = LoggerFactory.getLogger(LinkedinCookieService.class);

    private final LinkedinAccountRepository repository;
    private final ObjectMapper objectMapper;

    public LinkedinCookieService(LinkedinAccountRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }

    public boolean hasValidAccount() {
        return repository.findTopByValidTrueOrderByLastValidatedAtDesc().isPresent();
    }

    public Optional<String> getCookiesJson() {
        return repository.findTopByValidTrueOrderByLastValidatedAtDesc()
                .map(LinkedinAccount::getCookiesJson);
    }

    public LinkedinAccountStatus saveAndValidate(String cookiesJson) {
        try {
            boolean valid = validateCookies(cookiesJson);
            LinkedinAccount account = new LinkedinAccount();
            account.setCookiesJson(cookiesJson);
            account.setValid(valid);
            account.setLastValidatedAt(Instant.now());

            if (valid) {
                String name = extractProfileName(cookiesJson);
                account.setProfileName(name != null ? name : "LinkedIn User");
            }

            repository.save(account);

            return new LinkedinAccountStatus(valid, account.getProfileName());
        } catch (Exception e) {
            log.error("Failed to validate LinkedIn cookies", e);
            return new LinkedinAccountStatus(false, null);
        }
    }

    public LinkedinAccountStatus getStatus() {
        var opt = repository.findTopByValidTrueOrderByLastValidatedAtDesc();
        if (opt.isPresent()) {
            var acc = opt.get();
            return new LinkedinAccountStatus(true, acc.getProfileName());
        }
        return new LinkedinAccountStatus(false, null);
    }

    boolean validateCookies(String cookiesJson) {
        try {
            var cookiesArray = objectMapper.readTree(cookiesJson);
            if (!cookiesArray.isArray() || cookiesArray.isEmpty()) return false;

            try (Playwright pw = Playwright.create();
                 Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
                 BrowserContext ctx = browser.newContext()) {

                for (JsonNode c : cookiesArray) {
                    ctx.addCookies(List.of(toPlaywrightCookie(c)));
                }

                Page page = ctx.newPage();
                page.navigate("https://www.linkedin.com/feed/", new Page.NavigateOptions()
                        .setTimeout(30000));
                page.waitForLoadState();

                String url = page.url();
                boolean valid = url.contains("linkedin.com/feed") || url.contains("linkedin.com/mynetwork");
                log.info("LinkedIn cookie validation: url={}, valid={}", url, valid);
                return valid;
            }
        } catch (Exception e) {
            log.warn("LinkedIn cookie validation failed: {}", e.getMessage());
            return false;
        }
    }

    private com.microsoft.playwright.options.Cookie toPlaywrightCookie(JsonNode node) {
        String name = node.get("name").asText();
        String value = node.get("value").asText();
        var cookie = new com.microsoft.playwright.options.Cookie(name, value);
        if (node.has("domain")) cookie.domain = node.get("domain").asText();
        if (node.has("path")) cookie.path = node.get("path").asText();
        if (node.has("httpOnly")) cookie.httpOnly = node.get("httpOnly").asBoolean();
        if (node.has("secure")) cookie.secure = node.get("secure").asBoolean();
        if (node.has("sameSite")) cookie.sameSite = com.microsoft.playwright.options.SameSiteAttribute.valueOf(node.get("sameSite").asText());
        return cookie;
    }

    private String extractProfileName(String cookiesJson) {
        try {
            JsonNode arr = objectMapper.readTree(cookiesJson);
            for (JsonNode c : arr) {
                if ("li_user".equals(c.get("name").asText()) || "li_company".equals(c.get("name").asText())) {
                    return c.get("value").asText();
                }
            }
        } catch (Exception e) {
            log.debug("Could not extract profile name from cookies");
        }
        return null;
    }

    public record LinkedinAccountStatus(boolean valid, String profileName) {}
}
