package com.safepe.dto;

/**
 * Request DTO for SMS scam-detection analysis.
 *
 * @param content the raw SMS text to analyse
 */
public record ScamSMSRequest(
        String content
) {}
