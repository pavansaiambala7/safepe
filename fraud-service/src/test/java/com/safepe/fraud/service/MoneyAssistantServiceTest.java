package com.safepe.fraud.service;

import com.safepe.fraud.model.Transaction;
import com.safepe.fraud.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Retrieval and prompt construction of the Money Assistant RAG; Gemini is mocked. */
class MoneyAssistantServiceTest {

    private final TransactionRepository transactions = mock(TransactionRepository.class);
    private final GeminiAIService gemini = mock(GeminiAIService.class);
    private final MoneyAssistantService service = new MoneyAssistantService(transactions, gemini);

    private String promptSentToGemini() {
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(gemini).chatWithAssistant(prompt.capture());
        return prompt.getValue();
    }

    @Test
    void groundsAnswerInTheUsersOwnTransactions() {
        when(transactions.findRecentByUserId("user_a")).thenReturn(List.of(
                Transaction.builder().userId("user_a").payeeUpi("swiggy@icici").amount(new BigDecimal("450.00")).build(),
                Transaction.builder().userId("user_a").payeeUpi("rent@hdfc").amount(new BigDecimal("15000.00")).build()));
        when(gemini.chatWithAssistant(org.mockito.ArgumentMatchers.anyString())).thenReturn("Your total spending is ₹15450.00.");

        String answer = service.answer("user_a", "What is my total spending?");

        assertThat(answer).isEqualTo("Your total spending is ₹15450.00.");
        verify(transactions).findRecentByUserId("user_a");
        assertThat(promptSentToGemini())
                .contains("Total Transactions: 2", "Total Spent: ₹15450.00", "swiggy@icici", "rent@hdfc",
                        "USER QUESTION: What is my total spending?",
                        "Never mention transactions, payees or amounts that are not in the list above");
    }

    @Test
    void sendsAtMostFiftyTransactionRowsAsContext() {
        List<Transaction> many = java.util.stream.IntStream.range(0, 60)
                .mapToObj(i -> Transaction.builder().userId("user_a").payeeUpi("p" + i + "@upi").amount(BigDecimal.ONE).build())
                .toList();
        when(transactions.findRecentByUserId("user_a")).thenReturn(many);

        service.answer("user_a", "Summarise");

        String prompt = promptSentToGemini();
        assertThat(prompt).contains("Total Transactions: 60", "p49@upi").doesNotContain("p50@upi");
    }

    @Test
    void userWithNoTransactionsGetsGeneralAdviceWithoutSpendingFigures() {
        when(transactions.findRecentByUserId("user_a")).thenReturn(List.of());

        service.answer("user_a", "How do I save money?");

        assertThat(promptSentToGemini())
                .contains("no recorded transactions", "USER QUESTION: How do I save money?")
                .doesNotContain("Total Spent");
    }

    @Test
    void promptAsksForFormalFourToFiveSentenceReply() {
        when(transactions.findRecentByUserId("user_a")).thenReturn(List.of());

        service.answer("user_a", "Hi");

        assertThat(promptSentToGemini()).contains("4 to 5 complete sentences", "Do not use greetings, emojis, Markdown");
    }
}
