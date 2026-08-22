package com.safepe.layer1.controller;

import com.safepe.model.Transaction;
import com.safepe.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/history")
@RequiredArgsConstructor
public class HistoryController {

    private final TransactionRepository transactionRepository;

    @GetMapping("/transactions")
    public ResponseEntity<List<Transaction>> getRecentTransactions(Principal principal) {
        String userId = principal.getName();
        List<Transaction> transactions = transactionRepository.findRecentByUserId(userId);
        return ResponseEntity.ok(transactions);
    }
}
