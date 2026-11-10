package com.wellington.fraudmonitor.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long transactionId;

    @Column(nullable = false)
    private Long accountId;

    @Column(nullable = false)
    private String ruleName; // STRUCTURING, PROFILE_DEVIATION, ...

    private BigDecimal riskScore;

    @Column(nullable = false)
    private String reason;

    @Column(nullable = false)
    private String status = "OPEN"; // OPEN, AUTO_CLEARED, CLOSED, ESCALATED

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // --- Investigator draft (written by the system) ---
    private String investigationSummary;
    private String recommendation; // CLEAR, ESCALATE, NEED_INFO
    private String confidence;     // LOW, MEDIUM, HIGH
    private LocalDateTime investigatedAt;

    // --- Analyst's final call (written by a human) ---
    private String decision;       // CLEAR, ESCALATE, NEED_INFO
    private String decisionNote;
    private String decidedBy;
    private LocalDateTime decidedAt;

    public Long getId() { return id; }
    public Long getTransactionId() { return transactionId; }
    public void setTransactionId(Long transactionId) { this.transactionId = transactionId; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public BigDecimal getRiskScore() { return riskScore; }
    public void setRiskScore(BigDecimal riskScore) { this.riskScore = riskScore; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public String getInvestigationSummary() { return investigationSummary; }
    public void setInvestigationSummary(String s) { this.investigationSummary = s; }
    public String getRecommendation() { return recommendation; }
    public void setRecommendation(String r) { this.recommendation = r; }
    public String getConfidence() { return confidence; }
    public void setConfidence(String c) { this.confidence = c; }
    public LocalDateTime getInvestigatedAt() { return investigatedAt; }
    public void setInvestigatedAt(LocalDateTime t) { this.investigatedAt = t; }
    public String getDecision() { return decision; }
    public void setDecision(String d) { this.decision = d; }
    public String getDecisionNote() { return decisionNote; }
    public void setDecisionNote(String n) { this.decisionNote = n; }
    public String getDecidedBy() { return decidedBy; }
    public void setDecidedBy(String d) { this.decidedBy = d; }
    public LocalDateTime getDecidedAt() { return decidedAt; }
    public void setDecidedAt(LocalDateTime t) { this.decidedAt = t; }
}
