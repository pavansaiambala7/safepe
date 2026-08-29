package com.safepe.fraud.service;

import com.safepe.fraud.model.Transaction;
import com.safepe.fraud.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoneyAssistantService {

    private final TransactionRepository transactionRepository;
    private final GeminiAIService geminiAIService;

    public String answer(String userId, String question) {
        List<Transaction> txns = transactionRepository.findRecentByUserId(userId);

        if (txns == null || txns.isEmpty()) {
            String fallbackPrompt = """
                    You are SafePe Money Assistant, an intelligent financial AI advisor.
                    The user currently has no recorded transactions in their account yet.
                    Answer their financial or payment question clearly, concisely, and helpfully.
                    Provide practical budgeting, FD, saving, or UPI tips if relevant.
                    Use ₹ for currency and keep a warm, professional tone.

                    USER QUESTION: %s
                    """.formatted(question);
            return geminiAIService.chatWithAssistant(fallbackPrompt);
        }

        // ── Retrieval & Augmentation: calculate spending metrics & transaction rows ──
        java.math.BigDecimal totalSpent = txns.stream()
                .map(Transaction::getAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        String context = txns.stream()
                .limit(50)
                .map(t -> String.format("- %s | ₹%s | to %s | Type: %s | Status: %s",
                        t.getCreatedAt() != null ? t.getCreatedAt().toString() : "Recent",
                        t.getAmount(),
                        t.getPayeeUpi() != null ? t.getPayeeUpi() : "Direct Transfer",
                        t.getType() != null ? t.getType() : "UPI",
                        t.getStatus() != null ? t.getStatus() : "SUCCESS"))
                .collect(Collectors.joining("\n"));

        String prompt = """
                You are SafePe Money Assistant, an advanced AI financial copilot grounded in the user's live transaction data (RAG).
                
                USER FINANCIAL SUMMARY (Retrieved from Database):
                - Total Transactions: %d
                - Total Spent: ₹%s
                
                RECENT TRANSACTIONS:
                %s

                INSTRUCTIONS:
                1. Answer the user's question accurately using their transaction data whenever applicable.
                2. Be concise, friendly, and use bullet points when summarizing transactions or spending tips.
                3. Use ₹ for amounts and never hallucinate transactions that are not in the list.
                4. If asking for advice, give actionable money management and savings recommendations based on their habits.

                USER QUESTION: %s
                """.formatted(txns.size(), totalSpent, context, question);

        return geminiAIService.chatWithAssistant(prompt);
    }
}
