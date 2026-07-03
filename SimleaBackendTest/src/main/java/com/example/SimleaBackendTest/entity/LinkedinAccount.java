package com.example.SimleaBackendTest.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "linkedin_account")
public class LinkedinAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cookies_json", columnDefinition = "TEXT")
    private String cookiesJson;

    @Column(name = "is_valid")
    private boolean valid;

    @Column(name = "profile_name")
    private String profileName;

    @Column(name = "profile_photo")
    private String profilePhoto;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "last_validated_at")
    private Instant lastValidatedAt;

    public LinkedinAccount() {
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCookiesJson() { return cookiesJson; }
    public void setCookiesJson(String cookiesJson) { this.cookiesJson = cookiesJson; }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public String getProfileName() { return profileName; }
    public void setProfileName(String profileName) { this.profileName = profileName; }

    public String getProfilePhoto() { return profilePhoto; }
    public void setProfilePhoto(String profilePhoto) { this.profilePhoto = profilePhoto; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getLastValidatedAt() { return lastValidatedAt; }
    public void setLastValidatedAt(Instant lastValidatedAt) { this.lastValidatedAt = lastValidatedAt; }
}
