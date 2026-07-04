package com.example.SimleaBackendTest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "search")
public class SearchProperties {

    private boolean mockEnabled = true;
    private Google google = new Google();
    private Bing bing = new Bing();
    private GitHub github = new GitHub();
    private Hunter hunter = new Hunter();

    public boolean isMockEnabled() { return mockEnabled; }
    public void setMockEnabled(boolean mockEnabled) { this.mockEnabled = mockEnabled; }

    public Google getGoogle() { return google; }
    public void setGoogle(Google google) { this.google = google; }

    public Bing getBing() { return bing; }
    public void setBing(Bing bing) { this.bing = bing; }

    public GitHub getGithub() { return github; }
    public void setGithub(GitHub github) { this.github = github; }

    public Hunter getHunter() { return hunter; }
    public void setHunter(Hunter hunter) { this.hunter = hunter; }

    public static class Google {
        private String apiKey;
        private String cx;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }

        public String getCx() { return cx; }
        public void setCx(String cx) { this.cx = cx; }
    }

    public static class Bing {
        private String apiKey;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class GitHub {
        private String apiKey;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }

    public static class Hunter {
        private String apiKey;

        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    }
}
