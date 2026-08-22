package com.safepe.dto;

/**
 * A single fixed-deposit recommendation within an {@link FDRateResponse}.
 *
 * @param bankName     name of the recommending bank/NBFC
 * @param rate         annual interest rate (e.g. 7.25)
 * @param tenureMonths recommended tenure in months
 * @param reasoning    why this option was selected
 */
public record FDRecommendation(
        String bankName,
        Double rate,
        Integer tenureMonths,
        String reasoning
) {}
