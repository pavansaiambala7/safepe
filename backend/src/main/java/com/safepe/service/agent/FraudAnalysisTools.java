package com.safepe.service.agent;

import com.safepe.model.Merchant;
import com.safepe.repository.MerchantRepository;
import com.safepe.repository.TransactionRepository;
import com.safepe.service.rag.VectorSearchService;
import com.safepe.service.rag.VectorSearchService.VectorSearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Fraud Analysis Tools — LangChain4j Agent Tool Providers
 * ==========================================================
 * Provides tool methods that the Agentic AI Engine can invoke
 * during its multi-step reasoning process. Each method is a
 * self-contained analysis unit:
 *
 *   - searchFraudPatterns: RAG vector search against known fraud signatures
 *   - checkMerchantTrustScore: Database lookup for merchant risk assessment
 *   - analyzeTransactionVelocity: Recent transaction frequency analysis
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAnalysisTools {

    private final VectorSearchService vectorSearchService;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;

    /**
     * Tool: Search fraud patterns using RAG vector search.
     * Uses Gemini embeddings + pgvector for semantic similarity.
     *
     * @param message The suspicious message or transaction description
     * @return Formatted string of matched fraud patterns with similarity scores
     */
    public String searchFraudPatterns(String message) {
        log.info("🔍 [Tool] Searching fraud patterns for: '{}'", 
                message.substring(0, Math.min(80, message.length())));

        List<VectorSearchResult> results = vectorSearchService.searchSimilarPatterns(message);

        if (results.isEmpty()) {
            return "No similar fraud patterns found in the knowledge base. This message appears to be unique.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("Found %d matching fraud patterns:\n", results.size()));
        for (int i = 0; i < results.size(); i++) {
            VectorSearchResult r = results.get(i);
            sb.append(String.format("  [%d] Category: %s | Severity: %s | Similarity: %.1f%% | Pattern: %s\n",
                    i + 1, r.getCategory(), r.getSeverity(), r.getSimilarityScore(),
                    r.getPatternDescription().substring(0, Math.min(100, r.getPatternDescription().length()))));
        }
        return sb.toString();
    }

    /**
     * Tool: Check merchant trust score from the database.
     *
     * @param upiId The UPI ID to look up
     * @return Merchant trust assessment string
     */
    public String checkMerchantTrustScore(String upiId) {
        log.info("🏪 [Tool] Checking merchant trust score for UPI: {}", upiId);

        Optional<Merchant> merchantOpt = merchantRepository.findByUpiIdMasked(upiId);
        if (merchantOpt.isPresent()) {
            Merchant m = merchantOpt.get();
            return String.format("Merchant Found: '%s' | Trust Score: %.0f%% | Verified: %s | Flagged: %s | Reports: %d",
                    m.getName(),
                    m.getTrustScore() * 100,
                    m.getIsVerified() ? "YES" : "NO",
                    m.getIsFlagged() ? "⚠️ YES" : "NO",
                    m.getReportCount());
        }
        return "Merchant NOT FOUND in database. This UPI ID is unverified — treat with caution.";
    }

    /**
     * Tool: Analyze transaction velocity for a user.
     * Detects rapid-fire transactions that may indicate account takeover.
     *
     * @param userId The user ID to analyze
     * @return Transaction velocity assessment
     */
    public String analyzeTransactionVelocity(String userId) {
        log.info("⏱️ [Tool] Analyzing transaction velocity for user: {}", userId);

        long recentCount = transactionRepository.findByUserId(userId).size();

        if (recentCount > 20) {
            return String.format("HIGH VELOCITY: User has %d transactions. This is unusual and may indicate automated/fraudulent activity.", recentCount);
        } else if (recentCount > 10) {
            return String.format("MODERATE VELOCITY: User has %d transactions. Within normal range but worth monitoring.", recentCount);
        } else {
            return String.format("NORMAL VELOCITY: User has %d transactions. No velocity anomalies detected.", recentCount);
        }
    }

    /**
     * Returns the raw vector search results (for API response).
     */
    public List<VectorSearchResult> getRawSearchResults(String message) {
        return vectorSearchService.searchSimilarPatterns(message);
    }
}
