package com.wellington.fraudmonitor.rules;

import com.wellington.fraudmonitor.model.Account;
import com.wellington.fraudmonitor.model.Alert;
import com.wellington.fraudmonitor.model.Transaction;
import com.wellington.fraudmonitor.repository.AccountRepository;
import com.wellington.fraudmonitor.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Compares what the customer is doing against their profile (KYC).
 *
 * If deposits over the last 30 days exceed the expected monthly amount by
 * 5x (2x for HIGH risk customers), raise an alert.
 * If a source of funds is on record (e.g. "sale of house"), the alert is
 * still logged for audit but marked AUTO_CLEARED, so it never reaches the
 * analyst queue. That is how known-good customers skip the wait.
 */
@Component
public class ProfileDeviationRule implements FraudRule {

    static final Duration WINDOW = Duration.ofDays(30);
    static final BigDecimal NORMAL_MULTIPLIER = new BigDecimal("5");
    static final BigDecimal HIGH_RISK_MULTIPLIER = new BigDecimal("2");

    private final AccountRepository accounts;
    private final TransactionRepository transactions;

    public ProfileDeviationRule(AccountRepository accounts, TransactionRepository transactions) {
        this.accounts = accounts;
        this.transactions = transactions;
    }

    @Override
    public Optional<Alert> evaluate(Transaction tx) {
        if (!"DEPOSIT".equals(tx.getType())) {
            return Optional.empty();
        }

        Optional<Account> found = accounts.findById(tx.getAccountId());
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Account account = found.get();

        // No profile means we have nothing to compare against
        BigDecimal expected = account.getExpectedMonthlyDeposits();
        if (expected == null || expected.signum() <= 0) {
            return Optional.empty();
        }

        BigDecimal multiplier = "HIGH".equals(account.getRiskRating())
                ? HIGH_RISK_MULTIPLIER : NORMAL_MULTIPLIER;
        BigDecimal limit = expected.multiply(multiplier);

        List<Transaction> deposits = transactions.findByAccountIdAndTypeAndOccurredAtBetween(
                tx.getAccountId(), "DEPOSIT", tx.getOccurredAt().minus(WINDOW), tx.getOccurredAt());

        BigDecimal total = deposits.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (total.compareTo(limit) <= 0) {
            return Optional.empty();
        }

        BigDecimal ratio = total.divide(expected, 2, RoundingMode.HALF_UP);

        Alert alert = new Alert();
        alert.setTransactionId(tx.getId());
        alert.setAccountId(tx.getAccountId());
        alert.setRuleName("PROFILE_DEVIATION");
        alert.setRiskScore(BigDecimal.valueOf(Math.min(100, 40 + ratio.intValue() * 10)));

        String base = "Deposits of $" + total.toPlainString() + " in 30 days are "
                + ratio + "x the expected $" + expected.toPlainString() + " per month";

        String note = account.getSourceOfFundsNote();
        if (note != null && !note.isBlank()) {
            alert.setStatus("AUTO_CLEARED");
            alert.setReason(base + ". Cleared: source of funds on record (" + note + ")");
        } else {
            alert.setReason(base + ". No source of funds on record");
        }
        return Optional.of(alert);
    }
}
