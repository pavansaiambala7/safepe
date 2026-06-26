package com.safepe.layer1.controller;

import com.safepe.model.BankAccount;
import com.safepe.repository.BankAccountRepository;
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
        String userId = principal.getName();
        List<BankAccount> accounts = bankAccountRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return ResponseEntity.ok(accounts);
    }

    @PostMapping("/accounts")
    public ResponseEntity<?> addAccount(Principal principal, @RequestBody AddAccountRequest request) {
        String userId = principal.getName();
        
        BankAccount account = BankAccount.builder()
                .userId(userId)
                .bankName(request.getBankName())
                .razorpayTokenId(request.getRazorpayTokenId())
                .accountLastFour(request.getAccountLastFour())
                // Random starting balance between 5,000 and 1,00,000
                .balance(BigDecimal.valueOf(5000 + Math.random() * 95000))
                .build();
                
        bankAccountRepository.save(account);
        
        log.info("Added new tokenized bank account for user {}: {}", userId, request.getBankName());
        return ResponseEntity.status(HttpStatus.CREATED).body(account);
    }

    @PostMapping("/balance")
    public ResponseEntity<?> checkBalance(Principal principal, @RequestBody BalanceRequest request) {
        String userId = principal.getName();
        
        Optional<BankAccount> optionalAccount = bankAccountRepository.findByIdAndUserId(request.getAccountId(), userId);
        
        if (optionalAccount.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Account not found"));
        }
        
        BankAccount account = optionalAccount.get();
        
        // In a real flow, checking balance with UPI PIN happens directly with the bank via UPI network (NPCI),
        // or through Razorpay if they provide standard APIs for this. For now, we mock success since
        // the client SDK authenticates the PIN locally.
        
        return ResponseEntity.ok(Map.of(
                "balance", account.getBalance(),
                "currency", "INR",
                "bankName", account.getBankName(),
                "accountLastFour", account.getAccountLastFour() != null ? account.getAccountLastFour() : "XXXX"
        ));
    }
}

@Data
class AddAccountRequest {
    private String bankName;
    private String razorpayTokenId;
    private String accountLastFour;
}

@Data
class BalanceRequest {
    private UUID accountId;
    private String upiPin;
}
