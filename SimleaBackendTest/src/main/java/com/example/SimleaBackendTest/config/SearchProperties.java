package com.example.SimleaBackendTest.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "search")
public class SearchProperties {

    private Google google = new Google();
    private Bing bing = new Bing();

    public Google getGoogle() { return google; }
    public void setGoogle(Google google) { this.google = google; }

    public Bing getBing() { return bing; }
    public void setBing(Bing bing) { this.bing = bing; }

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
}
