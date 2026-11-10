package com.wellington.fraudmonitor.investigation;

import com.wellington.fraudmonitor.model.Account;
import com.wellington.fraudmonitor.model.Alert;
import com.wellington.fraudmonitor.model.Transaction;
import com.wellington.fraudmonitor.repository.AccountRepository;
import com.wellington.fraudmonitor.repository.AlertAuditRepository;
import com.wellington.fraudmonitor.repository.AlertRepository;
import com.wellington.fraudmonitor.repository.TransactionRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AlertInvestigatorTest {

    private AlertRepository alerts = mock(AlertRepository.class);
    private AccountRepository accounts = mock(AccountRepository.class);
    private TransactionRepository transactions = mock(TransactionRepository.class);
    private AlertAuditRepository audits = mock(AlertAuditRepository.class);
    private AlertInvestigator investigator =
            new AlertInvestigator(alerts, accounts, transactions, audits, new MockExternalData());

    private Alert setUp(String customerName, String rule) {
        Account account = new Account();
        account.setCustomerName(customerName);

        Alert alert = new Alert();
        alert.setAccountId(1L);
        alert.setRuleName(rule);
        alert.setReason("test reason");

        Transaction t = new Transaction();
        t.setAccountId(1L);
        t.setAmount(new BigDecimal("20000"));
        t.setType("DEPOSIT");
        t.setOccurredAt(LocalDateTime.of(2026, 11, 10, 9, 0));

        when(alerts.findById(1L)).thenReturn(Optional.of(alert));
        when(accounts.findById(1L)).thenReturn(Optional.of(account));
        when(transactions.findByAccountIdOrderByOccurredAtDesc(1L)).thenReturn(List.of(t));
        when(alerts.save(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));
        return alert;
    }

    @Test
    void sanctionsMatchRecommendsEscalate() {
        setUp("Viktor Petrov", "PROFILE_DEVIATION");
        Alert result = investigator.investigate(1L);
        assertEquals("ESCALATE", result.getRecommendation());
        assertEquals("HIGH", result.getConfidence());
    }

    @Test
    void publicRecordExplainingDepositsRecommendsClear() {
        setUp("Day Trader", "PROFILE_DEVIATION");
        Alert result = investigator.investigate(1L);
        assertEquals("CLEAR", result.getRecommendation());
        assertTrue(result.getInvestigationSummary().contains("Brokerage withdrawal"));
    }

    @Test
    void structuringWithNoExplanationRecommendsEscalate() {
        setUp("Nobody Known", "STRUCTURING");
        assertEquals("ESCALATE", investigator.investigate(1L).getRecommendation());
    }

    @Test
    void unexplainedDeviationNeedsMoreInfo() {
        setUp("Nobody Known", "PROFILE_DEVIATION");
        assertEquals("NEED_INFO", investigator.investigate(1L).getRecommendation());
    }

    @Test
    void investigationNeverChangesAlertStatus() {
        setUp("Day Trader", "PROFILE_DEVIATION");
        assertEquals("OPEN", investigator.investigate(1L).getStatus());
    }

    @Test
    void analystClearClosesTheAlert() {
        setUp("Day Trader", "PROFILE_DEVIATION");
        Alert result = investigator.decide(1L, "CLEAR", "Verified brokerage statement", "Sam");
        assertEquals("CLOSED", result.getStatus());
        assertEquals("Sam", result.getDecidedBy());
    }

    @Test
    void analystEscalateMarksAlertEscalated() {
        setUp("Nobody Known", "STRUCTURING");
        assertEquals("ESCALATED", investigator.decide(1L, "ESCALATE", "Pattern repeats", "Sam").getStatus());
    }
}
