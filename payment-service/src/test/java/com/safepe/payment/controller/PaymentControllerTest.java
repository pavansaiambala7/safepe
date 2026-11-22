package com.safepe.payment.controller;

import com.safepe.payment.config.SecurityConfig;
import com.safepe.payment.service.PaymentService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer slice: request binding, bean validation, the shared error format
 * and how the user id reaches the service. The service itself is mocked.
 */
@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
@MockBean(PaymentService.class)
class PaymentControllerTest {

    private final MockMvc mvc;
    private final PaymentService paymentService;

    PaymentControllerTest(MockMvc mvc, PaymentService paymentService) {
        this.mvc = mvc;
        this.paymentService = paymentService;
    }

    private static final String VALID_BODY = "{\"amount\": 250.00, \"upiId\": \"shop@oksbi\"}";

    @Test
    void createsOrderForTheUserFromTheGatewayHeader() throws Exception {
        when(paymentService.createPaymentOrder(any(), any(), any())).thenReturn(Map.of("orderId", "order_abc"));

        mvc.perform(post("/api/v1/payments/create").header("X-User-Id", "user_a")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value("order_abc"));

        verify(paymentService).createPaymentOrder(eq("user_a"), eq("shop@oksbi"), argThat(a -> a.compareTo(new BigDecimal("250")) == 0));
    }

    @Test
    void rejectsInvalidUpiIdWithFieldError() throws Exception {
        mvc.perform(post("/api/v1/payments/create").header("X-User-Id", "user_a")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 250, \"upiId\": \"not-a-upi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.upiId").exists());

        verifyNoInteractions(paymentService);
    }

    @Test
    void rejectsAmountOutsideAllowedRange() throws Exception {
        mvc.perform(post("/api/v1/payments/create").header("X-User-Id", "user_a")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 0.50, \"upiId\": \"shop@oksbi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());

        mvc.perform(post("/api/v1/payments/create").header("X-User-Id", "user_a")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"amount\": 500000.01, \"upiId\": \"shop@oksbi\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void rejectsMalformedJson() throws Exception {
        mvc.perform(post("/api/v1/payments/create").header("X-User-Id", "user_a")
                        .contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    @Disabled("Known bug, fixed in Phase 1: without X-User-Id the service falls back to 'user_123_temp' "
            + "instead of rejecting the request")
    void rejectsRequestWithoutAuthenticatedUser() throws Exception {
        mvc.perform(post("/api/v1/payments/create")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(paymentService);
    }
}
