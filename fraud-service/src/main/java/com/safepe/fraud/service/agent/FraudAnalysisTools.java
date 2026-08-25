package com.safepe.fraud.service.agent;

import com.safepe.fraud.model.Merchant;
import com.safepe.fraud.repository.MerchantRepository;
import com.safepe.fraud.repository.TransactionRepository;
import com.safepe.fraud.service.rag.VectorSearchService;
import com.safepe.fraud.service.rag.VectorSearchService.VectorSearchResult;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAnalysisTools {

    private final VectorSearchService vectorSearchService;
    private final MerchantRepository merchantRepository;
    private final TransactionRepository transactionRepository;

    @Tool("Search known fraud patterns using semantic vector (cosine) similarity")
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

    @Tool("Look up a merchant's trust score, verification status and fraud flags by UPI ID")
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

    @Tool("Analyze a user's recent transaction velocity to detect rapid-fire fraud")
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

    public List<VectorSearchResult> getRawSearchResults(String message) {
        return vectorSearchService.searchSimilarPatterns(message);
    }
}
