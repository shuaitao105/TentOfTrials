package com.tentoftrials.compliance;

import java.time.Instant;
import java.util.Map;

public class ComplianceRecord {
    private final String id;
    private final String checkType;
    private final Map<String, Object> data;
    private final Instant timestamp;

    public ComplianceRecord(String id, String checkType, Map<String, Object> data, Instant timestamp) {
        this.id = id;
        this.checkType = checkType;
        this.data = data;
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public String getCheckType() {
        return checkType;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Instant getTimestamp() {
        return timestamp;
    }
}
