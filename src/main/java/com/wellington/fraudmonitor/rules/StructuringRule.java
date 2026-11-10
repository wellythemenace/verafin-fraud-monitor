package com.wellington.fraudmonitor.rules;

import com.wellington.fraudmonitor.model.Alert;
import com.wellington.fraudmonitor.model.Transaction;
import com.wellington.fraudmonitor.repository.TransactionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Structuring = splitting cash deposits so each one stays under the
 * reporting threshold, even though together they exceed it.
 *
 * Rule: look at this account's deposits in the 24 hours up to this one.
 * Flag if there are 2+ deposits, every one is under $10,000,
 * and the total is $10,000 or more.
 */
@Component
public class StructuringRule implements FraudRule {

    static final BigDecimal REPORTING_THRESHOLD = new BigDecimal("10000");
    static final Duration WINDOW = Duration.ofHours(24);
    static final int MIN_DEPOSITS = 2;

    private final TransactionRepository transactions;

    public StructuringRule(TransactionRepository transactions) {
        this.transactions = transactions;
    }

    @Override
    public Optional<Alert> evaluate(Transaction tx) {
        // Only deposits can be structured
        if (!"DEPOSIT".equals(tx.getType())) {
            return Optional.empty();
        }

        LocalDateTime from = tx.getOccurredAt().minus(WINDOW);
        List<Transaction> deposits = transactions.findByAccountIdAndTypeAndOccurredAtBetween(
                tx.getAccountId(), "DEPOSIT", from, tx.getOccurredAt());

        BigDecimal total = deposits.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // A deposit at or over the threshold gets reported anyway, so it is not "structured"
        boolean allUnderThreshold = deposits.stream()
                .allMatch(d -> d.getAmount().compareTo(REPORTING_THRESHOLD) < 0);

        boolean suspicious = deposits.size() >= MIN_DEPOSITS
                && allUnderThreshold
                && total.compareTo(REPORTING_THRESHOLD) >= 0;

        if (!suspicious) {
            return Optional.empty();
        }

        Alert alert = new Alert();
        alert.setTransactionId(tx.getId());
        alert.setAccountId(tx.getAccountId());
        alert.setRuleName("STRUCTURING");
        // more deposits in the window = higher score, capped at 100
        alert.setRiskScore(BigDecimal.valueOf(Math.min(100, 50 + deposits.size() * 10)));
        alert.setReason(deposits.size() + " deposits totalling $" + total.toPlainString()
                + " within 24 hours, each under the $" + REPORTING_THRESHOLD.toPlainString()
                + " reporting threshold");
        return Optional.of(alert);
    }
}
