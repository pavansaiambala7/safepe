package com.safepe.payment.controller;

import com.safepe.payment.model.BankAccount;
import com.safepe.payment.repository.BankAccountRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bank")
@RequiredArgsConstructor
@Slf4j
public class BankAccountController {

    private final BankAccountRepository bankAccountRepository;

    @GetMapping("/accounts")
    public ResponseEntity<List<BankAccount>> getAccounts(Principal principal) {
        String userId = principal != null ? principal.getName() : "user_123_temp";
        List<BankAccount> accounts = bankAccountRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/accounts")
    public ResponseEntity<?> addAccount(Principal principal, @RequestBody AddAccountRequest request) {
        try {
            String userId = principal != null ? principal.getName() : "user_123_temp";

            log.info("📥 Adding bank account for user {}: bank={}, lastFour={}",
                    userId, request.getBankName(), request.getAccountLastFour());

            BankAccount account = BankAccount.builder()
                    .userId(userId)
                    .bankName(request.getBankName())
                    .razorpayTokenId(request.getRazorpayTokenId())
                    .accountLastFour(request.getAccountLastFour())
                    .balance(BigDecimal.valueOf(5000 + Math.random() * 95000))
                    .build();

            bankAccountRepository.save(account);

            log.info("✅ Successfully added bank account for user {}: {}", userId, request.getBankName());
            return ResponseEntity.status(HttpStatus.CREATED).body(account);
        } catch (Exception e) {
            log.error("❌ Failed to add bank account: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to add bank account: " + e.getMessage()));
        }
    }

    @PostMapping("/balance")
    public ResponseEntity<?> checkBalance(Principal principal, @RequestBody BalanceRequest request) {
        String userId = principal != null ? principal.getName() : "user_123_temp";

        Optional<BankAccount> optionalAccount = bankAccountRepository.findByIdAndUserId(request.getAccountId(), userId);

        if (optionalAccount.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Account not found"));
        }

        BankAccount account = optionalAccount.get();

        return ResponseEntity.ok(Map.of(
                "balance", account.getBalance(),
                "currency", "INR",
                "bankName", account.getBankName(),
                "accountLastFour", account.getAccountLastFour() != null ? account.getAccountLastFour() : "XXXX"
        ));
    }

    @Data
    public static class AddAccountRequest {
        private String bankName;
        private String razorpayTokenId;
        private String accountLastFour;
    }

    @Data
    public static class BalanceRequest {
        private UUID accountId;
        private String upiPin;
    }
}
