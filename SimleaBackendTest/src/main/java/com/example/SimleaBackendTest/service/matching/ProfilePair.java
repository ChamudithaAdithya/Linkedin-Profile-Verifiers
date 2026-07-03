package com.example.SimleaBackendTest.service.matching;

import com.example.SimleaBackendTest.entity.SourceProfile;

public record ProfilePair(SourceProfile a, SourceProfile b) {
    public String nameA() { return a.getFullName(); }
    public String nameB() { return b.getFullName(); }
    public String companyA() { return a.getCompany(); }
    public String companyB() { return b.getCompany(); }
    public String locationA() { return a.getLocation(); }
    public String locationB() { return b.getLocation(); }
    public String headlineA() { return a.getHeadline(); }
    public String headlineB() { return b.getHeadline(); }
    public String urlA() { return a.getProfileUrl(); }
    public String urlB() { return b.getProfileUrl(); }
}
