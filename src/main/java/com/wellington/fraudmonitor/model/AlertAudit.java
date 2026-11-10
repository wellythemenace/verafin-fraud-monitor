package com.wellington.fraudmonitor.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "alert_audit")
public class AlertAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long alertId;

    @Column(nullable = false)
    private String action; // INVESTIGATED, DECISION

    @Column(nullable = false)
    private String actor;  // "system" or analyst name

    private String detail;

    @Column(insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public AlertAudit() {}

    public AlertAudit(Long alertId, String action, String actor, String detail) {
        this.alertId = alertId;
        this.action = action;
        this.actor = actor;
        this.detail = detail;
    }

    public Long getId() { return id; }
    public Long getAlertId() { return alertId; }
    public String getAction() { return action; }
    public String getActor() { return actor; }
    public String getDetail() { return detail; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
