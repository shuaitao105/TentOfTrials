package com.tentoftrials.compliance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RuleEngineTest {
    private RuleEngine ruleEngine;

    @BeforeEach
    void setUp() {
        ruleEngine = new RuleEngine();
    }

    @Test
    void kycPassesWhenStatusIsComplete() {
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", "user-1");
        data.put("kyc_status", "complete");

        ComplianceResult result = ruleEngine.evaluate("KYC", data);

        assertTrue(result.isCompliant());
    }

    @Test
    void kycFailsWhenStatusIsPending() {
        Map<String, Object> data = new HashMap<>();
        data.put("user_id", "user-2");
        data.put("kyc_status", "pending");

        ComplianceResult result = ruleEngine.evaluate("KYC", data);

        assertFalse(result.isCompliant());
    }

    @Test
    void amlFlagsLargeTransactions() {
        Map<String, Object> data = new HashMap<>();
        data.put("transaction_amount", 25000.0);

        ComplianceResult result = ruleEngine.evaluate("AML", data);

        assertFalse(result.isCompliant());
    }
}
