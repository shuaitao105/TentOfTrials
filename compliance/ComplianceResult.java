package com.tentoftrials.compliance;

import java.util.Collection;

public class ComplianceResult {
    private final boolean compliant;
    private final Collection<String> violations;
    private final String summary;

    public ComplianceResult(boolean compliant, Collection<String> violations, String summary) {
        this.compliant = compliant;
        this.violations = violations;
        this.summary = summary;
    }

    public boolean isCompliant() {
        return compliant;
    }

    public Collection<String> getViolations() {
        return violations;
    }

    public String getSummary() {
        return summary;
    }
}
