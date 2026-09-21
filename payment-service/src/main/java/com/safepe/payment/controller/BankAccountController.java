package com.safepe.payment.controller;

import com.safepe.payment.model.BankAccount;
import com.safepe.payment.repository.BankAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
    public ResponseEntity<?> addAccount(Principal principal, @Valid @RequestBody AddAccountRequest request) {
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
    public ResponseEntity<?> checkBalance(Principal principal, @Valid @RequestBody BalanceRequest request) {
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
        @NotBlank(message = "bankName is required")
        @Size(max = 100, message = "must be at most 100 characters")
        private String bankName;

        @Size(max = 100, message = "must be at most 100 characters")
        private String razorpayTokenId;

        @Pattern(regexp = "^[0-9]{4}$", message = "must be exactly 4 digits")
        private String accountLastFour;
    }

    @Data
    public static class BalanceRequest {
        @NotNull(message = "accountId is required")
        private UUID accountId;

        @Pattern(regexp = "^[0-9]{4,6}$", message = "must be 4 to 6 digits")
        private String upiPin;
    }
}
