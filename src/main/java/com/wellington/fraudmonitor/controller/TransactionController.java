package com.wellington.fraudmonitor.controller;

import com.wellington.fraudmonitor.model.Account;
import com.wellington.fraudmonitor.model.Transaction;
import com.wellington.fraudmonitor.repository.AccountRepository;
import com.wellington.fraudmonitor.repository.AlertRepository;
import com.wellington.fraudmonitor.repository.TransactionRepository;
import com.wellington.fraudmonitor.rules.FraudRule;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
public class TransactionController {

    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final AlertRepository alerts;
    private final List<FraudRule> rules; // Spring injects every FraudRule it finds

    public TransactionController(AccountRepository accounts,
                                 TransactionRepository transactions,
                                 AlertRepository alerts,
                                 List<FraudRule> rules) {
        this.accounts = accounts;
        this.transactions = transactions;
        this.alerts = alerts;
        this.rules = rules;
    }

    public record NewAccount(
            @NotBlank String customerName,
            @Positive BigDecimal expectedMonthlyDeposits,
            @Pattern(regexp = "LOW|MEDIUM|HIGH") String riskRating,
            String sourceOfFundsNote) {}

    public record ProfileUpdate(
            @Positive BigDecimal expectedMonthlyDeposits,
            @Pattern(regexp = "LOW|MEDIUM|HIGH") String riskRating,
            String sourceOfFundsNote) {}

    public record NewTransaction(
            @NotNull Long accountId,
            @NotNull @Positive BigDecimal amount,
            @NotBlank String type,
            @NotNull LocalDateTime occurredAt) {}

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public Account createAccount(@Valid @RequestBody NewAccount body) {
        Account a = new Account();
        a.setCustomerName(body.customerName());
        a.setExpectedMonthlyDeposits(body.expectedMonthlyDeposits());
        if (body.riskRating() != null) {
            a.setRiskRating(body.riskRating());
        }
        a.setSourceOfFundsNote(body.sourceOfFundsNote());
        return accounts.save(a);
    }

    // Update the customer profile, e.g. an analyst records a source of funds
    @PutMapping("/accounts/{id}/profile")
    public Account updateProfile(@PathVariable Long id, @Valid @RequestBody ProfileUpdate body) {
        Account a = accounts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        if (body.expectedMonthlyDeposits() != null) a.setExpectedMonthlyDeposits(body.expectedMonthlyDeposits());
        if (body.riskRating() != null) a.setRiskRating(body.riskRating());
        if (body.sourceOfFundsNote() != null) a.setSourceOfFundsNote(body.sourceOfFundsNote());
        return accounts.save(a);
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public Transaction createTransaction(@Valid @RequestBody NewTransaction body) {
        if (!accounts.existsById(body.accountId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        Transaction t = new Transaction();
        t.setAccountId(body.accountId());
        t.setAmount(body.amount());
        t.setType(body.type());
        t.setOccurredAt(body.occurredAt());
        Transaction saved = transactions.save(t);

        // Save first, then run every rule, so the rules can see this transaction in their queries
        for (FraudRule rule : rules) {
            rule.evaluate(saved).ifPresent(alerts::save);
        }

        return saved;
    }

    @GetMapping("/transactions")
    public List<Transaction> listTransactions(@RequestParam Long accountId) {
        return transactions.findByAccountIdOrderByOccurredAtDesc(accountId);
    }
}
