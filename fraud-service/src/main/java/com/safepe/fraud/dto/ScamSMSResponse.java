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
public class ScamSMSResponse {
    private String classification;
    private Double confidence;
    private List<String> redFlags;
    private String explanation;
    private String advice;
}
