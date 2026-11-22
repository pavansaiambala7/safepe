package com.safepe.fraud.service.agent;

import com.safepe.fraud.dto.AgenticFraudResult;
import com.safepe.fraud.service.rag.VectorSearchService.VectorSearchResult;
import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Pins the risk-scoring rules of the fraud-pattern RAG. Gemini and the vector
 * search are mocked, so only the engine's own decisions are under test.
 */
class AgenticFraudEngineTest {

    private final FraudAnalysisTools tools = mock(FraudAnalysisTools.class);
    private final ChatLanguageModel gemini = mock(ChatLanguageModel.class);
    private final AgenticFraudEngine engine = new AgenticFraudEngine(tools, gemini);

    @BeforeEach
    void defaults() {
        when(tools.searchFraudPatterns(anyString())).thenReturn("matches");
        when(gemini.generate(anyString())).thenReturn("PHISHING - asks for credentials");
    }

    private void vectorSearchReturns(double... similarities) {
        when(tools.getRawSearchResults(anyString())).thenReturn(java.util.Arrays.stream(similarities)
                .mapToObj(s -> VectorSearchResult.builder().patternId("p").patternDescription("KYC link")
                        .category("KYC_FRAUD").severity("HIGH").similarityScore(s).build())
                .toList());
    }

    @Test
    void noMatchingPatternIsLowRiskAndAllowed() {
        vectorSearchReturns();

        AgenticFraudResult result = engine.analyzeWithAgents("Your OTP for login is 123456", null, "user_a");

        assertThat(result.getRiskScore()).isEqualTo(15);
        assertThat(result.getTrustScore()).isEqualTo(92);
        assertThat(result.getAction()).isEqualTo("ALLOW");
        assertThat(result.isRagPatternMatched()).isFalse();
    }

    @ParameterizedTest(name = "best match {0}% -> risk {1}, {2}")
    @CsvSource({
            "91.0, 91, BLOCK",
            "85.0, 85, BLOCK",
            "84.6, 85, BLOCK",
            "84.4, 84, FLAG_VERIFICATION",
            "76.0, 76, FLAG_VERIFICATION",
            "62.0, 75, FLAG_VERIFICATION",
            "99.9, 99, BLOCK"
    })
    void matchedPatternSetsRiskFromBestSimilarity(double bestSimilarity, int expectedRisk, String expectedAction) {
        vectorSearchReturns(60.5, bestSimilarity);

        AgenticFraudResult result = engine.analyzeWithAgents("Update KYC now at bit.ly/x", null, "user_a");

        assertThat(result.getRiskScore()).isEqualTo(expectedRisk);
        assertThat(result.getTrustScore()).isEqualTo(Math.max(1, 100 - expectedRisk));
        assertThat(result.getAction()).isEqualTo(expectedAction);
        assertThat(result.getTrustLevel()).isEqualTo("LOW_TRUST");
        assertThat(result.getMatchedPatterns()).hasSize(2);
    }

    @Test
    void returnsThreeReasoningStepsAndUsesGeminiSummary() {
        vectorSearchReturns(90.0);
        when(gemini.generate(anyString())).thenReturn("PHISHING", "Blocked: matches a KYC phishing pattern.\n");

        AgenticFraudResult result = engine.analyzeWithAgents("Update KYC now", null, "user_a");

        assertThat(result.getReasoningSteps()).extracting(AgenticFraudResult.ReasoningStep::getStepNumber)
                .containsExactly(1, 2, 3);
        assertThat(result.getSummary()).isEqualTo("Blocked: matches a KYC phishing pattern.");
    }

    @Test
    void geminiOutageStillProducesAVerdict() {
        vectorSearchReturns(90.0);
        when(gemini.generate(anyString())).thenThrow(new RuntimeException("429 quota exceeded"));

        AgenticFraudResult result = engine.analyzeWithAgents("Update KYC now", null, "user_a");

        assertThat(result.getAction()).isEqualTo("BLOCK");
        assertThat(result.getSummary()).contains("90.0% similarity");
    }

    @Test
    void checksMerchantAndVelocityOnlyWhenIdsAreGiven() {
        vectorSearchReturns();

        engine.analyzeWithAgents("hello", null, null);
        verify(tools, never()).checkMerchantTrustScore(anyString());
        verify(tools, never()).analyzeTransactionVelocity(anyString());

        engine.analyzeWithAgents("hello", "shop@oksbi", "user_a");
        verify(tools).checkMerchantTrustScore("shop@oksbi");
        verify(tools).analyzeTransactionVelocity("user_a");
    }

    @Test
    void vectorSearchFailureIsTreatedAsNoMatch() {
        when(tools.getRawSearchResults(anyString())).thenThrow(new RuntimeException("embedding API down"));

        AgenticFraudResult result = engine.analyzeWithAgents("hello", null, "user_a");

        assertThat(result.getAction()).isEqualTo("ALLOW");
        assertThat(result.getMatchedPatterns()).isEmpty();
    }

    @Test
    void matchedPatternsAreCopiedFromSearchResults() {
        when(tools.getRawSearchResults(anyString())).thenReturn(List.of(VectorSearchResult.builder()
                .patternId("42").patternDescription("Lottery win").category("LOTTERY_SCAM").severity("MEDIUM")
                .similarityScore(88.0).build()));

        AgenticFraudResult result = engine.analyzeWithAgents("You won 25 lakh", null, "user_a");

        assertThat(result.getMatchedPatterns()).singleElement()
                .returns("42", AgenticFraudResult.MatchedPattern::getPatternId)
                .returns("LOTTERY_SCAM", AgenticFraudResult.MatchedPattern::getCategory)
                .returns(88.0, AgenticFraudResult.MatchedPattern::getSimilarityPercent);
    }
}
