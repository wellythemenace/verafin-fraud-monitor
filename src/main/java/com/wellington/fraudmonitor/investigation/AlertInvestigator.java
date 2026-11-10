package com.wellington.fraudmonitor.investigation;

import com.wellington.fraudmonitor.model.Account;
import com.wellington.fraudmonitor.model.Alert;
import com.wellington.fraudmonitor.model.AlertAudit;
import com.wellington.fraudmonitor.model.Transaction;
import com.wellington.fraudmonitor.repository.AccountRepository;
import com.wellington.fraudmonitor.repository.AlertAuditRepository;
import com.wellington.fraudmonitor.repository.AlertRepository;
import com.wellington.fraudmonitor.repository.TransactionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Does the first pass of an analyst's research and writes a DRAFT case summary.
 * It only recommends. A human analyst makes the final decision (see decide()).
 * Every step is written to the audit trail.
 */
@Service
public class AlertInvestigator {

    private final AlertRepository alerts;
    private final AccountRepository accounts;
    private final TransactionRepository transactions;
    private final AlertAuditRepository audits;
    private final MockExternalData external;

    public AlertInvestigator(AlertRepository alerts, AccountRepository accounts,
                             TransactionRepository transactions, AlertAuditRepository audits,
                             MockExternalData external) {
        this.alerts = alerts;
        this.accounts = accounts;
        this.transactions = transactions;
        this.audits = audits;
        this.external = external;
    }

    public Alert investigate(Long alertId) {
        Alert alert = alerts.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
        Account account = accounts.findById(alert.getAccountId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));

        // 1. Gather evidence
        List<Transaction> deposits = transactions.findByAccountIdOrderByOccurredAtDesc(alert.getAccountId())
                .stream().filter(t -> "DEPOSIT".equals(t.getType())).toList();
        BigDecimal total = deposits.stream().map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal largest = deposits.stream().map(Transaction::getAmount)
                .max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO);

        boolean sanctioned = external.isSanctioned(account.getCustomerName());
        List<String> records = external.publicRecordsFor(account.getCustomerName());
        String sourceNote = account.getSourceOfFundsNote();
        boolean hasSourceNote = sourceNote != null && !sourceNote.isBlank();

        // 2. Decide what to recommend, and why
        String recommendation;
        String confidence;
        String why;
        if (sanctioned) {
            recommendation = "ESCALATE";
            confidence = "HIGH";
            why = "The customer name matches the sanctions list.";
        } else if ("PROFILE_DEVIATION".equals(alert.getRuleName()) && !records.isEmpty()) {
            recommendation = "CLEAR";
            confidence = "MEDIUM";
            why = "Public records show a possible explanation for the deposits. "
                    + "The analyst should confirm it is the same person and that the amounts line up.";
        } else if ("STRUCTURING".equals(alert.getRuleName())) {
            recommendation = "ESCALATE";
            confidence = "MEDIUM";
            why = "The deposits look split to stay under the reporting threshold and nothing found explains it.";
        } else {
            recommendation = "NEED_INFO";
            confidence = "LOW";
            why = "No explanation was found in the available data. Ask the customer for their source of funds.";
        }

        // 3. Write the draft summary, with where each fact came from
        StringBuilder s = new StringBuilder();
        s.append("DRAFT CASE SUMMARY (for analyst review)\n");
        s.append("Alert: ").append(alert.getRuleName()).append(" - ").append(alert.getReason()).append("\n");
        s.append("Customer: ").append(account.getCustomerName())
                .append(", risk rating ").append(account.getRiskRating())
                .append(", expected monthly deposits ")
                .append(account.getExpectedMonthlyDeposits() == null ? "not on file"
                        : "$" + account.getExpectedMonthlyDeposits().toPlainString()).append("\n");
        s.append("Source of funds on record: ").append(hasSourceNote ? sourceNote : "none").append("\n");
        s.append("Deposit activity: ").append(deposits.size()).append(" deposits totalling $")
                .append(total.toPlainString()).append(", largest $").append(largest.toPlainString());
        if (!deposits.isEmpty()) {
            s.append(", from ").append(deposits.get(deposits.size() - 1).getOccurredAt())
                    .append(" to ").append(deposits.get(0).getOccurredAt());
        }
        s.append("\n");
        s.append("Sanctions check (MOCK list): ").append(sanctioned ? "MATCH" : "no match").append("\n");
        s.append("Public records (MOCK data): ")
                .append(records.isEmpty() ? "nothing found" : String.join("; ", records)).append("\n");
        s.append("Recommendation: ").append(recommendation).append(" (confidence ").append(confidence).append(")\n");
        s.append("Why: ").append(why).append("\n");
        s.append("Sources: internal transactions, internal customer profile, mock sanctions list, mock public records\n");
        s.append("This is a draft. The analyst makes the final decision.");

        // 4. Save the draft and log it
        alert.setInvestigationSummary(s.toString());
        alert.setRecommendation(recommendation);
        alert.setConfidence(confidence);
        alert.setInvestigatedAt(LocalDateTime.now());
        Alert saved = alerts.save(alert);

        audits.save(new AlertAudit(alertId, "INVESTIGATED", "system",
                "Draft recommends " + recommendation + " (" + confidence + ")"));
        return saved;
    }

    /** The analyst's final call. Only a human can close or escalate an alert. */
    public Alert decide(Long alertId, String decision, String note, String analyst) {
        Alert alert = alerts.findById(alertId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));

        alert.setDecision(decision);
        alert.setDecisionNote(note);
        alert.setDecidedBy(analyst);
        alert.setDecidedAt(LocalDateTime.now());
        if ("CLEAR".equals(decision)) {
            alert.setStatus("CLOSED");
        } else if ("ESCALATE".equals(decision)) {
            alert.setStatus("ESCALATED");
        } // NEED_INFO keeps the alert OPEN
        Alert saved = alerts.save(alert);

        boolean agreed = decision.equals(alert.getRecommendation());
        audits.save(new AlertAudit(alertId, "DECISION", analyst,
                "decision=" + decision + ", agreed with draft=" + agreed + ", note=" + note));
        return saved;
    }
}
