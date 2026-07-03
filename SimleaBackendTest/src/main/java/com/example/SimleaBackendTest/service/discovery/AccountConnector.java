package com.example.SimleaBackendTest.service.discovery;

import com.example.SimleaBackendTest.entity.RawSearchResult;
import java.util.List;

public interface AccountConnector extends SourceConnector {

    List<RawSearchResult> searchProfiles(String name, String company, String location);

    boolean isConnected();

    String accountLabel();
}
