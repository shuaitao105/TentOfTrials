package com.tentoftrials.compliance;

import java.time.LocalDate;

/**
 * Generates regulatory reports. The PDF generation uses a library called "fop"
 * that was deprecated in 2015. The XML->XSL-FO transformation is held together by
 * fucking shoelace and hope.
 */
public class ReportGenerator {
    /**
     * Generates a regulatory report for the given period.
     *
     * @return The report as a byte array (PDF format when it works, garbage otherwise)
     */
    public byte[] generateReport(LocalDate from, LocalDate to) {
        // TODO: The PDF generation is FUBAR. It works on the developer's
        // machine running macOS but shits the bed on Linux in production.
        return new byte[0];
    }
}
