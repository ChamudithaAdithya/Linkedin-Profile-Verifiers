package com.example.SimleaBackendTest.service.linkedin;

import com.example.SimleaBackendTest.entity.RawSearchResult;
import com.example.SimleaBackendTest.service.discovery.AccountConnector;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LinkedinPlaywrightConnector implements AccountConnector {

    private static final Logger log = LoggerFactory.getLogger(LinkedinPlaywrightConnector.class);
    private static final Pattern LINKEDIN_PROFILE_URL = Pattern.compile(
            "https?://(?:www\\.)?linkedin\\.com/in/[^/\\s\"'>]+");

    private final LinkedinCookieService cookieService;
    private final ObjectMapper objectMapper;

    public LinkedinPlaywrightConnector(LinkedinCookieService cookieService) {
        this.cookieService = cookieService;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String sourceName() {
        return "linkedin-playwright";
    }

    @Override
    public boolean isConnected() {
        return cookieService.hasValidAccount();
    }

    @Override
    public String accountLabel() {
        var status = cookieService.getStatus();
        return status.valid() ? status.profileName() : "No account connected";
    }

    @Override
    public List<RawSearchResult> search(String query) {
        return searchProfiles(query, null, null);
    }

    @Override
    public List<RawSearchResult> searchStructured(String name, String company, String location) {
        return searchProfiles(name, company, location);
    }

    @Override
    public List<RawSearchResult> searchProfiles(String name, String company, String location) {
        if (!isConnected()) {
            log.warn("LinkedIn account not connected. Cannot search.");
            return List.of();
        }

        Optional<String> cookiesOpt = cookieService.getCookiesJson();
        if (cookiesOpt.isEmpty()) return List.of();

        String keywords = buildSearchQuery(name, company, location);
        if (keywords.isBlank()) return List.of();

        log.info("LinkedIn Playwright search: keywords='{}'", keywords);

        try (Playwright pw = Playwright.create();
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             BrowserContext ctx = browser.newContext()) {

            injectCookies(ctx, cookiesOpt.get());

            Page page = ctx.newPage();
            String encoded = URLEncoder.encode(keywords, StandardCharsets.UTF_8);
            String searchUrl = "https://www.linkedin.com/search/results/people/?keywords=" + encoded;

            log.info("Navigating to: {}", searchUrl);
            page.navigate(searchUrl, new Page.NavigateOptions().setTimeout(45000));
            page.waitForLoadState();
            page.waitForTimeout(3000);

            String html = page.content();
            return parseSearchResults(html, keywords);
        }
    }

    private void injectCookies(BrowserContext ctx, String cookiesJson) {
        try {
            JsonNode arr = objectMapper.readTree(cookiesJson);
            for (JsonNode node : arr) {
                String name = node.get("name").asText();
                String value = node.get("value").asText();
                var cookie = new com.microsoft.playwright.options.Cookie(name, value);
                if (node.has("domain")) cookie.domain = node.get("domain").asText();
                if (node.has("path")) cookie.path = node.get("path").asText();
                if (node.has("httpOnly")) cookie.httpOnly = node.get("httpOnly").asBoolean();
                if (node.has("secure")) cookie.secure = node.get("secure").asBoolean();
                if (node.has("sameSite")) {
                    String ss = node.get("sameSite").asText();
                    try {
                        cookie.sameSite = com.microsoft.playwright.options.SameSiteAttribute.valueOf(ss.toUpperCase());
                    } catch (Exception ex) {
                        // ignore invalid sameSite value
                    }
                }
                ctx.addCookies(List.of(cookie));
            }
        } catch (Exception e) {
            log.error("Failed to inject cookies", e);
        }
    }

    List<RawSearchResult> parseSearchResults(String html, String query) {
        List<RawSearchResult> results = new ArrayList<>();

        try (Playwright pw = Playwright.create();
             Browser browser = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
             Page page = browser.newPage()) {

            page.setContent(html);

            var cards = page.querySelectorAll(".reusable-search__result-container");
            log.info("Found {} profile cards", cards.size());

            for (var card : cards) {
                try {
                    RawSearchResult result = new RawSearchResult();
                    result.setQuery(query);
                    result.setSource(sourceName());
                    result.setFetchedAt(Instant.now());

                    var linkEl = card.querySelector("a[href*='/in/']");
                    if (linkEl != null) {
                        String href = linkEl.getAttribute("href");
                        String fullUrl = href != null && href.startsWith("/")
                                ? "https://www.linkedin.com" + href.split("\\?")[0]
                                : href;
                        if (fullUrl != null) {
                            result.setUrl(fullUrl);
                        }
                    }

                    var nameEl = card.querySelector(".actor-name, .profile-card-name, span[aria-hidden='true']");
                    if (nameEl != null) {
                        result.setTitle(nameEl.textContent().trim());
                    }

                    var headlineEl = card.querySelector(".profile-card-headline, .actor-description");
                    if (headlineEl != null) {
                        result.setSnippet(headlineEl.textContent().trim());
                    }

                    if (result.getTitle() != null && !result.getTitle().isBlank()) {
                        results.add(result);
                    }
                } catch (Exception e) {
                    log.debug("Failed to parse card: {}", e.getMessage());
                }
            }
        }

        if (results.isEmpty()) {
            var fallback = fallbackParse(html, query);
            results.addAll(fallback);
        }

        log.info("Parsed {} LinkedIn results from search", results.size());
        return results;
    }

    private List<RawSearchResult> fallbackParse(String html, String query) {
        List<RawSearchResult> results = new ArrayList<>();
        var m = LINKEDIN_PROFILE_URL.matcher(html);
        while (m.find()) {
            String url = m.group();
            if (results.stream().anyMatch(r -> url.equals(r.getUrl()))) continue;

            RawSearchResult result = new RawSearchResult();
            result.setQuery(query);
            result.setSource(sourceName());
            result.setUrl(url);
            result.setFetchedAt(Instant.now());

            int titleStart = html.indexOf("aria-hidden=\"true\"", m.start() - 500);
            if (titleStart > 0 && titleStart < m.start()) {
                int textStart = html.indexOf(">", titleStart) + 1;
                int textEnd = html.indexOf("<", textStart);
                if (textStart > 0 && textEnd > textStart) {
                    result.setTitle(html.substring(textStart, textEnd).trim());
                }
            }

            results.add(result);
        }
        return results;
    }

    private String buildSearchQuery(String name, String company, String location) {
        StringBuilder sb = new StringBuilder();
        if (name != null && !name.isBlank()) sb.append(name);
        if (company != null && !company.isBlank()) sb.append(" ").append(company);
        if (location != null && !location.isBlank()) sb.append(" ").append(location);
        return sb.toString().trim();
    }
}
