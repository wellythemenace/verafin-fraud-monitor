package com.wellington.fraudmonitor.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String customerName;

    // set by the database default (NOW()), so we never write it from Java
    @Column(insertable = false, updatable = false)
    private LocalDateTime openedAt;

    // --- KYC profile ---
    @Column(nullable = false)
    private String riskRating = "LOW"; // LOW, MEDIUM, HIGH

    private BigDecimal expectedMonthlyDeposits; // null = no profile yet

    private String sourceOfFundsNote; // e.g. "Sale of house", null = nothing on record

    public Long getId() { return id; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public String getRiskRating() { return riskRating; }
    public void setRiskRating(String riskRating) { this.riskRating = riskRating; }
    public BigDecimal getExpectedMonthlyDeposits() { return expectedMonthlyDeposits; }
    public void setExpectedMonthlyDeposits(BigDecimal v) { this.expectedMonthlyDeposits = v; }
    public String getSourceOfFundsNote() { return sourceOfFundsNote; }
    public void setSourceOfFundsNote(String note) { this.sourceOfFundsNote = note; }
}
