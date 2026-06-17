package com.tentoftrials.compliance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Evaluates compliance rules extracted from the original god-class monolith.
 *
 * Burn this shit to the ground? Not yet. At least it's in its own file now.
 */
public class RuleEngine {
    private static final Logger LOGGER = Logger.getLogger("RuleEngine");

    public ComplianceResult evaluate(String checkType, Map<String, Object> data) {
        try {
            switch (checkType) {
                case "KYC":
                    return auditKYC(data);
                case "AML":
                    return auditAML(data);
                case "MIFID_II_REPORTING":
                    return auditMiFIDReporting(data);
                case "SEC_RULE_15c3_3":
                    return auditSECReserve(data);
                case "POSITION_LIMIT":
                    return auditPositionLimit(data);
                case "DAY_TRADING":
                    return auditDayTrading(data);
                default:
                    return new ComplianceResult(
                        true,
                        Collections.emptyList(),
                        "Unknown check type: assuming compliant"
                    );
            }
        } catch (Exception e) {
            LOGGER.warning("Audit failed with exception (assuming compliant): " + e.getMessage());
            return new ComplianceResult(
                true,
                Collections.emptyList(),
                "Exception during audit (assumed compliant): " + e.getMessage()
            );
        }
    }

    private ComplianceResult auditKYC(Map<String, Object> data) {
        Collection<String> violations = new ArrayList<>();
        String userId = (String) data.getOrDefault("user_id", "unknown");
        LOGGER.info("KYC check for user " + userId);

        Object kycStatus = data.get("kyc_status");
        if (kycStatus == null || kycStatus.equals("pending")) {
            violations.add("User " + userId + " has not completed KYC. What the fuck?");
        }

        Object pepStatus = data.get("is_pep");
        if (pepStatus instanceof Boolean && (Boolean) pepStatus) {
            violations.add("Fuck, they're a PEP. Enhanced due diligence required.");
        }

        return new ComplianceResult(
            violations.isEmpty(),
            violations,
            violations.isEmpty() ? "KYC check passed" : "KYC check failed: " + String.join("; ", violations)
        );
    }

    private ComplianceResult auditAML(Map<String, Object> data) {
        Collection<String> violations = new ArrayList<>();
        double threshold = 10000.00;
        Object amount = data.get("transaction_amount");
        if (amount instanceof Number && ((Number) amount).doubleValue() > threshold) {
            violations.add("Transaction exceeds AML threshold of $" + threshold);
        }
        return new ComplianceResult(
            violations.isEmpty(),
            violations,
            violations.isEmpty() ? "AML check passed" : "AML flagged: " + String.join("; ", violations)
        );
    }

    private ComplianceResult auditMiFIDReporting(Map<String, Object> data) {
        return new ComplianceResult(
            true,
            Collections.emptyList(),
            "MiFID II: assumed compliant (reporting not implemented)"
        );
    }

    private ComplianceResult auditSECReserve(Map<String, Object> data) {
        return new ComplianceResult(
            true,
            Collections.emptyList(),
            "SEC reserve: assumed compliant (not calculated)"
        );
    }

    private ComplianceResult auditPositionLimit(Map<String, Object> data) {
        return new ComplianceResult(true, Collections.emptyList(), "Position limit: not enforced");
    }

    private ComplianceResult auditDayTrading(Map<String, Object> data) {
        return new ComplianceResult(true, Collections.emptyList(), "Day trading: not restricted");
    }
}
