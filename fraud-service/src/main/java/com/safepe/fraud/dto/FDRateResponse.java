package com.safepe.fraud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FDRateResponse {
    private List<FDRecommendation> recommendations;
    private String marketContext;
    private List<String> riskFactors;
    private String rawAnalysis;
}
