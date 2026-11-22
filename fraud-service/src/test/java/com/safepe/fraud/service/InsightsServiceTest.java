package com.safepe.fraud.service;

import com.safepe.fraud.model.Transaction;
import com.safepe.fraud.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class InsightsServiceTest {

    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final GeminiAIService gemini = mock(GeminiAIService.class);
    private final InsightsService service = new InsightsService(transactions, gemini);

    private static Transaction paid(String payee, String amount) {
        return Transaction.builder().userId("user_a").payeeUpi(payee).amount(new BigDecimal(amount)).build();
    }

    @Test
    void newUserGetsExplicitZerosAndNoGeminiCall() {
        when(transactions.findRecentByUserId("user_a")).thenReturn(List.of());

        Map<String, Object> insights = service.insights("user_a");

        assertThat(insights).containsEntry("totalSpent", BigDecimal.ZERO)
                .containsEntry("transactionCount", 0)
                .containsEntry("topPayees", List.of());
        verifyNoInteractions(gemini);
    }

    @Test
    @SuppressWarnings("unchecked")
    void totalsSpendingAndRanksPayeesByAmount() {
        when(transactions.findRecentByUserId("user_a")).thenReturn(List.of(
                paid("swiggy@icici", "450.00"), paid("rent@hdfc", "15000.00"),
                paid("swiggy@icici", "300.50"), paid("metro@sbi", "60.00")));
        when(gemini.chatWithAssistant(anyString())).thenReturn("You spend most on rent.");

        Map<String, Object> insights = service.insights("user_a");

        assertThat((BigDecimal) insights.get("totalSpent")).isEqualByComparingTo("15810.50");
        assertThat(insights).containsEntry("transactionCount", 4)
                .containsEntry("narrative", "You spend most on rent.");
        List<Map<String, Object>> top = (List<Map<String, Object>>) insights.get("topPayees");
        assertThat(top).extracting(p -> p.get("payee")).containsExactly("rent@hdfc", "swiggy@icici", "metro@sbi");
        assertThat((BigDecimal) top.get(1).get("amount")).isEqualByComparingTo("750.50");
    }

    @Test
    @SuppressWarnings("unchecked")
    void keepsTopFivePayeesOnly() {
        when(transactions.findRecentByUserId("user_a")).thenReturn(List.of(
                paid("a@x", "60"), paid("b@x", "50"), paid("c@x", "40"),
                paid("d@x", "30"), paid("e@x", "20"), paid("f@x", "10")));

        List<Map<String, Object>> top = (List<Map<String, Object>>) service.insights("user_a").get("topPayees");

        assertThat(top).hasSize(5).extracting(p -> p.get("payee")).doesNotContain("f@x");
    }

    @Test
    void promptGivesGeminiOnlyComputedFacts() {
        when(transactions.findRecentByUserId("user_a")).thenReturn(List.of(paid("rent@hdfc", "15000.00")));

        service.insights("user_a");

        verify(gemini).chatWithAssistant(argThatContains("Total spent: ₹15000.00 across 1 transactions"));
    }

    private static String argThatContains(String text) {
        return org.mockito.ArgumentMatchers.argThat(prompt -> prompt != null && prompt.contains(text));
    }
}
