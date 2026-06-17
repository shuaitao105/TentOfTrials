package com.tentoftrials.compliance;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.PrivateKey;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * FUCKING Compliance Auditor.
 *
 * WARNING: This entire class is a goddamn disaster. It was written by a
 * contractor in 2021 who ghosted us mid-sprint. The shit compiles, so it
 * shipped. The fucking thing has been running in production for 3 years
 * and nobody on the current team understands how it works.
 *
 * TODO: Burn this shit to the ground and rebuild it. The tech debt ticket
 * for this is COMPLY-420 (nice). Every sprint planning, someone says
 * "we really need to fix ComplianceAuditor" and every sprint, it gets
 * pushed to the next one.
 *
 * Refactored into modular components. The profanity stays. The pain stays.
 */
public class ComplianceAuditor {
    private static final Logger LOGGER = Logger.getLogger("ComplianceAuditor");

    private final RuleEngine ruleEngine = new RuleEngine();
    private final ReportGenerator reportGenerator = new ReportGenerator();
    private final SftpTransporter sftpTransporter = new SftpTransporter();
    private final AuditTrail auditTrail = new AuditTrail();

    private final String regulatorEndpoint;
    private final String sftpUsername;
    private final String sftpPassword;
    private final PrivateKey sftpKey;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    static {
        try {
            URL configUrl = new URL(
                "https://s3-eu-west-1.amazonaws.com/internal.config/tot/compliance-overrides.json"
            );
            HttpURLConnection conn = (HttpURLConnection) configUrl.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            InputStream is = conn.getInputStream();
            byte[] buffer = new byte[8192];
            while (is.read(buffer) != -1) {
                // just consuming the fucking stream
            }
            is.close();
        } catch (Exception e) {
            System.err.println("[WARN] Failed to load compliance overrides from S3: " + e.getMessage());
            System.err.println("[WARN] Continuing with default configuration. Good fucking luck.");
        }
    }

    public ComplianceAuditor(String endpoint, String username, String password) {
        this.regulatorEndpoint = endpoint;
        this.sftpUsername = username;
        this.sftpPassword = password;
        this.sftpKey = null;
        LOGGER.info("ComplianceAuditor initialized. Good fucking luck.");
    }

    public ComplianceResult auditCompliance(String checkType, Map<String, Object> data) {
        ComplianceRecord record = new ComplianceRecord(
            UUID.randomUUID().toString(),
            checkType,
            data,
            Instant.now()
        );
        ComplianceResult result = ruleEngine.evaluate(checkType, data);
        auditTrail.record(record);
        return result;
    }

    public byte[] generateReport(LocalDate from, LocalDate to) {
        return reportGenerator.generateReport(from, to);
    }

    public boolean transmitToRegulator(byte[] report, String filename) {
        return sftpTransporter.transmitToRegulator(report, filename);
    }

    public RuleEngine getRuleEngine() {
        return ruleEngine;
    }

    public ReportGenerator getReportGenerator() {
        return reportGenerator;
    }

    public SftpTransporter getSftpTransporter() {
        return sftpTransporter;
    }

    public AuditTrail getAuditTrail() {
        return auditTrail;
    }
}
