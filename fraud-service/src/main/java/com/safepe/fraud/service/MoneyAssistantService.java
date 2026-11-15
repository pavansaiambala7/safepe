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

    /** Reply style shared by both prompts: the chat UI shows plain text. */
    private static final String STYLE = """
            REPLY STYLE:
            - Write in a formal, professional tone, as a bank's financial advisor would.
            - Answer in 4 to 5 complete sentences of plain prose, as one short paragraph.
            - Do not use greetings, emojis, Markdown, asterisks, bullet points or headings.
            - Use ₹ for amounts.
            """;

    private final TransactionRepository transactionRepository;
    private final GeminiAIService geminiAIService;

    public String answer(String userId, String question) {
        List<Transaction> txns = transactionRepository.findRecentByUserId(userId);

        if (txns == null || txns.isEmpty()) {
            String fallbackPrompt = """
                    You are SafePe Money Assistant, a financial advisor in a payments app.
                    The user has no recorded transactions yet, so do not refer to their spending.
                    Answer their financial or payment question accurately, with practical
                    budgeting, savings, fixed deposit or UPI guidance where relevant.

                    %s
                    USER QUESTION: %s
                    """.formatted(STYLE, question);
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
                You are SafePe Money Assistant, a financial advisor grounded in the user's own transaction data.

                USER FINANCIAL SUMMARY (Retrieved from Database):
                - Total Transactions: %d
                - Total Spent: ₹%s

                RECENT TRANSACTIONS:
                %s

                INSTRUCTIONS:
                1. Answer the user's question accurately using their transaction data whenever applicable.
                2. Never mention transactions, payees or amounts that are not in the list above.
                3. If the user asks for advice, base it on their actual spending habits.

                %s
                USER QUESTION: %s
                """.formatted(txns.size(), totalSpent, context, STYLE, question);

        return geminiAIService.chatWithAssistant(prompt);
    }
}
