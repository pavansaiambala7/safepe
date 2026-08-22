package com.safepe.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Response DTO containing AI-curated FD rate recommendations.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FDRateResponse {

    /** Top recommended FD options, ranked by the AI model. */
    private List<FDRecommendation> recommendations;

    /** Brief market-context commentary (e.g. RBI rate-cycle outlook). */
    private String marketContext;

    /** Risk factors the user should be aware of. */
    private List<String> riskFactors;

    /** Raw analysis text returned by the AI model (for debugging / transparency). */
    private String rawAnalysis;
}
