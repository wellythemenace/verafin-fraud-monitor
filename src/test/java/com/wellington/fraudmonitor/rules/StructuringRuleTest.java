package com.wellington.fraudmonitor.rules;

import com.wellington.fraudmonitor.model.Alert;
import com.wellington.fraudmonitor.model.Transaction;
import com.wellington.fraudmonitor.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StructuringRuleTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 3, 12, 0);

    private Transaction deposit(String amount, LocalDateTime when) {
        Transaction t = new Transaction();
        t.setAccountId(1L);
        t.setAmount(new BigDecimal(amount));
        t.setType("DEPOSIT");
        t.setOccurredAt(when);
        return t;
    }

    private Optional<Alert> run(Transaction current, List<Transaction> inWindow) {
        TransactionRepository repo = mock(TransactionRepository.class);
        when(repo.findByAccountIdAndTypeAndOccurredAtBetween(eq(1L), eq("DEPOSIT"), any(), any()))
                .thenReturn(inWindow);
        return new StructuringRule(repo).evaluate(current);
    }

    @Test
    void twoSmallDepositsAddingPastThresholdAreFlagged() {
        Transaction a = deposit("6000", NOW.minusHours(2));
        Transaction b = deposit("5000", NOW);
        assertTrue(run(b, List.of(a, b)).isPresent());
    }

    @Test
    void singleDepositUnderThresholdIsNotFlagged() {
        Transaction a = deposit("9500", NOW);
        assertTrue(run(a, List.of(a)).isEmpty());
    }

    @Test
    void smallDepositsThatStayUnderThresholdTotalAreNotFlagged() {
        Transaction a = deposit("3000", NOW.minusHours(2));
        Transaction b = deposit("4000", NOW);
        assertTrue(run(b, List.of(a, b)).isEmpty());
    }

    @Test
    void aDepositOverThresholdIsNotStructuring() {
        Transaction a = deposit("12000", NOW.minusHours(2));
        Transaction b = deposit("5000", NOW);
        assertTrue(run(b, List.of(a, b)).isEmpty());
    }

    @Test
    void withdrawalsAreIgnored() {
        Transaction w = deposit("9000", NOW);
        w.setType("WITHDRAWAL");
        assertTrue(run(w, List.of()).isEmpty());
    }
}
