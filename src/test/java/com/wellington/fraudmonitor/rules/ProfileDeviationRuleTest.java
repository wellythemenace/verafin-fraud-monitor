package com.wellington.fraudmonitor.rules;

import com.wellington.fraudmonitor.model.Account;
import com.wellington.fraudmonitor.model.Alert;
import com.wellington.fraudmonitor.model.Transaction;
import com.wellington.fraudmonitor.repository.AccountRepository;
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

class ProfileDeviationRuleTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 3, 12, 0);

    private Account account(String expected, String risk, String note) {
        Account a = new Account();
        a.setCustomerName("Test");
        a.setExpectedMonthlyDeposits(expected == null ? null : new BigDecimal(expected));
        a.setRiskRating(risk);
        a.setSourceOfFundsNote(note);
        return a;
    }

    private Transaction deposit(String amount) {
        Transaction t = new Transaction();
        t.setAccountId(1L);
        t.setAmount(new BigDecimal(amount));
        t.setType("DEPOSIT");
        t.setOccurredAt(NOW);
        return t;
    }

    private Optional<Alert> run(Transaction current, Account acct, List<Transaction> inWindow) {
        AccountRepository accounts = mock(AccountRepository.class);
        TransactionRepository transactions = mock(TransactionRepository.class);
        when(accounts.findById(1L)).thenReturn(Optional.ofNullable(acct));
        when(transactions.findByAccountIdAndTypeAndOccurredAtBetween(eq(1L), eq("DEPOSIT"), any(), any()))
                .thenReturn(inWindow);
        return new ProfileDeviationRule(accounts, transactions).evaluate(current);
    }

    @Test
    void noProfileMeansNoAlert() {
        Transaction t = deposit("50000");
        assertTrue(run(t, account(null, "LOW", null), List.of(t)).isEmpty());
    }

    @Test
    void depositsWithinFiveTimesExpectedAreFine() {
        Transaction t = deposit("14000"); // limit is 5 x 3000 = 15000
        assertTrue(run(t, account("3000", "LOW", null), List.of(t)).isEmpty());
    }

    @Test
    void depositsOverFiveTimesExpectedRaiseOpenAlert() {
        Transaction t = deposit("20000");
        Optional<Alert> alert = run(t, account("3000", "LOW", null), List.of(t));
        assertTrue(alert.isPresent());
        assertEquals("PROFILE_DEVIATION", alert.get().getRuleName());
        assertEquals("OPEN", alert.get().getStatus());
    }

    @Test
    void sourceOfFundsOnRecordAutoClearsTheAlert() {
        Transaction t = deposit("20000");
        Optional<Alert> alert = run(t, account("3000", "LOW", "Sale of house"), List.of(t));
        assertTrue(alert.isPresent());
        assertEquals("AUTO_CLEARED", alert.get().getStatus());
    }

    @Test
    void highRiskCustomersGetTighterLimit() {
        Transaction t = deposit("7000"); // HIGH limit is 2 x 3000 = 6000
        assertTrue(run(t, account("3000", "HIGH", null), List.of(t)).isPresent());
    }

    @Test
    void withdrawalsAreIgnored() {
        Transaction t = deposit("50000");
        t.setType("WITHDRAWAL");
        assertTrue(run(t, account("3000", "LOW", null), List.of()).isEmpty());
    }
}
