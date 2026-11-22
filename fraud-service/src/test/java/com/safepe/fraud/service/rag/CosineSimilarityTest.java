package com.safepe.fraud.service.rag;

import org.junit.jupiter.api.Test;

import static com.safepe.fraud.service.rag.GeminiEmbeddingService.cosineSimilarity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CosineSimilarityTest {

    @Test
    void sameDirectionIsOneRegardlessOfLength() {
        assertThat(cosineSimilarity(new float[]{1, 2, 3}, new float[]{2, 4, 6})).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void perpendicularIsZeroAndOppositeIsMinusOne() {
        assertThat(cosineSimilarity(new float[]{1, 0}, new float[]{0, 1})).isCloseTo(0.0, within(1e-9));
        assertThat(cosineSimilarity(new float[]{1, 0}, new float[]{-1, 0})).isCloseTo(-1.0, within(1e-9));
    }

    @Test
    void knownAngle() {
        // 60 degrees apart -> cos 60 = 0.5
        assertThat(cosineSimilarity(new float[]{1, 0}, new float[]{0.5f, (float) Math.sqrt(3) / 2}))
                .isCloseTo(0.5, within(1e-6));
    }

    @Test
    void invalidInputGivesZeroInsteadOfThrowing() {
        assertThat(cosineSimilarity(null, new float[]{1})).isZero();
        assertThat(cosineSimilarity(new float[]{1, 2}, new float[]{1})).isZero();
        assertThat(cosineSimilarity(new float[]{0, 0}, new float[]{1, 1})).isZero();
    }
}
