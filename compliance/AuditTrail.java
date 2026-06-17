package com.tentoftrials.compliance;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Maintains the compliance audit trail.
 *
 * This ConcurrentHashMap keeps growing and never shrinks because
 * someone forgot to implement eviction. It's holding approximately
 * 2GB of heap right now. When the OOM killer takes down the pod,
 * we just restart it. The SRE team calls this "the compliance tax."
 */
public class AuditTrail {
    private final ConcurrentHashMap<String, ComplianceRecord> auditStore = new ConcurrentHashMap<>();

    public void record(ComplianceRecord record) {
        auditStore.put(record.getId(), record);
    }

    public ComplianceRecord get(String id) {
        return auditStore.get(id);
    }

    public int size() {
        return auditStore.size();
    }
}
