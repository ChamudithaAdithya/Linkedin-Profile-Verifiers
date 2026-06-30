package com.example.SimleaBackendTest.service;

import com.example.SimleaBackendTest.config.LinkedInProperties;
import com.example.SimleaBackendTest.dto.LinkedInTokenResponse;
import com.example.SimleaBackendTest.dto.LinkedInUserInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LinkedInService {

    private static final Logger log = LoggerFactory.getLogger(LinkedInService.class);

    private static final String AUTH_URL = "https://www.linkedin.com/oauth/v2/authorization";
    private static final String TOKEN_URL = "https://www.linkedin.com/oauth/v2/accessToken";
    private static final String USERINFO_URL = "https://api.linkedin.com/v2/userinfo";

    private final LinkedInProperties props;
    private final ObjectMapper objectMapper;
    private final Map<String, String> stateStore = new ConcurrentHashMap<>();

    public LinkedInService(LinkedInProperties props) {
        this.props = props;
        this.objectMapper = new ObjectMapper();
        log.info("LinkedInService initialized with client_id={}, redirect_uri={}",
                props.getClientId(), props.getRedirectUri());
    }

    public String generateAuthorizationUrl() {
        String state = UUID.randomUUID().toString();
        stateStore.put(state, "used");

        return AUTH_URL + "?response_type=code"
                + "&client_id=" + props.getClientId()
                + "&redirect_uri=" + props.getRedirectUri()
                + "&scope=" + props.getScope().replace(" ", "%20")
                + "&state=" + state;
    }

    public LinkedInUserInfo authenticate(String code, String state) throws Exception {
        if (state == null || stateStore.remove(state) == null) {
            throw new RuntimeException("Invalid state parameter");
        }
        String accessToken = exchangeCodeForToken(code);
        return fetchUserInfo(accessToken);
    }

    private String exchangeCodeForToken(String code) throws Exception {
        String body = "grant_type=" + urlEncode("authorization_code")
                + "&code=" + urlEncode(code)
                + "&client_id=" + urlEncode(props.getClientId())
                + "&client_secret=" + urlEncode(props.getClientSecret())
                + "&redirect_uri=" + urlEncode(props.getRedirectUri());

        log.info("Token exchange request: client_id={}, redirect_uri={}",
                props.getClientId(), props.getRedirectUri());

        HttpURLConnection conn = (HttpURLConnection) URI.create(TOKEN_URL).toURL().openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        log.info("LinkedIn response status: {}", status);

        if (status >= 200 && status < 300) {
            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            LinkedInTokenResponse tr = objectMapper.readValue(json, LinkedInTokenResponse.class);
            if (tr.getAccessToken() == null) {
                throw new RuntimeException("No access_token in response: " + json);
            }
            return tr.getAccessToken();
        } else {
            String errBody = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            log.error("LinkedIn token exchange error: status={}, body={}", status, errBody);
            throw new RuntimeException("LinkedIn token exchange failed: " + status + " - " + errBody);
        }
    }

    private LinkedInUserInfo fetchUserInfo(String accessToken) throws Exception {
        HttpURLConnection conn = (HttpURLConnection) URI.create(USERINFO_URL).toURL().openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);

        int status = conn.getResponseCode();
        log.info("Userinfo response status: {}", status);

        if (status >= 200 && status < 300) {
            String json = new String(conn.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            log.info("Userinfo response: {}", json);
            return objectMapper.readValue(json, LinkedInUserInfo.class);
        } else {
            String err = new String(conn.getErrorStream().readAllBytes(), StandardCharsets.UTF_8);
            log.error("Userinfo error: status={}, body={}", status, err);
            throw new RuntimeException("Userinfo fetch failed: " + status);
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
