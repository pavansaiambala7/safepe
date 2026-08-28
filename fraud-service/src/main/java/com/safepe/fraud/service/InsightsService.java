package com.safepe.fraud.service;

import com.safepe.fraud.model.Transaction;
import com.safepe.fraud.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InsightsService {

    private final TransactionRepository transactionRepository;
    private final GeminiAIService geminiAIService;

    public Map<String, Object> insights(String userId) {
        List<Transaction> txns = transactionRepository.findRecentByUserId(userId);
        Map<String, Object> out = new LinkedHashMap<>();

        if (txns.isEmpty()) {
            out.put("summary", "No transactions yet.");
            out.put("narrative", "Start paying with SafePe and your spending insights will appear here.");
            return out;
        }

        BigDecimal total = txns.stream()
                .map(Transaction::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> byPayee = txns.stream()
                .filter(t -> t.getPayeeUpi() != null && t.getAmount() != null)
                .collect(Collectors.groupingBy(
                        Transaction::getPayeeUpi,
                        Collectors.reducing(BigDecimal.ZERO, Transaction::getAmount, BigDecimal::add)));

        List<Map.Entry<String, BigDecimal>> topPayees = byPayee.entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toList());

        out.put("totalSpent", total);
        out.put("transactionCount", txns.size());
        out.put("topPayees", topPayees.stream()
                .map(e -> Map.of("payee", e.getKey(), "amount", e.getValue()))
                .collect(Collectors.toList()));

        String facts = String.format(
                "Total spent: ₹%s across %d transactions. Top payees: %s",
                total, txns.size(),
                topPayees.stream().map(e -> e.getKey() + " (₹" + e.getValue() + ")")
                        .collect(Collectors.joining(", ")));

        String prompt = """
                You are SafePe Spending Insights. Given these aggregated facts about the
                user's own spending, write a short (3-4 sentence) friendly, factual summary
                with one practical money tip. Do not invent numbers.

                FACTS: %s
                """.formatted(facts);

        out.put("narrative", geminiAIService.chatWithAssistant(prompt));
        return out;
    }
}
