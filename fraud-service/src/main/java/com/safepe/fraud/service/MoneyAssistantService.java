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

        if (txns.isEmpty()) {
            return "I don't see any transactions for your account yet. Make a payment and I'll be able to answer questions about your spending.";
        }

        // ── Retrieval: build a compact context of the user's own data ──────
        String context = txns.stream()
                .limit(50)
                .map(t -> String.format("- %s | ₹%s | to %s | %s | %s",
                        t.getCreatedAt(), t.getAmount(), t.getPayeeUpi(),
                        t.getType(), t.getStatus()))
                .collect(Collectors.joining("\n"));

        // ── Augmentation: ground Gemini strictly in the retrieved rows ─────
        String prompt = """
                You are SafePe Money Assistant. Answer ONLY from the user's transaction
                history below. Be concise, use ₹ for amounts, and never invent data.
                If the answer isn't in the data, say so.

                USER TRANSACTION HISTORY:
                %s

                USER QUESTION: %s
                """.formatted(context, question);

        return geminiAIService.chatWithAssistant(prompt);
    }
}
