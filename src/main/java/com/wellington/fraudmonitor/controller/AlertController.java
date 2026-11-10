package com.wellington.fraudmonitor.controller;

import com.wellington.fraudmonitor.investigation.AlertInvestigator;
import com.wellington.fraudmonitor.model.Alert;
import com.wellington.fraudmonitor.model.AlertAudit;
import com.wellington.fraudmonitor.repository.AlertAuditRepository;
import com.wellington.fraudmonitor.repository.AlertRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
public class AlertController {

    private final AlertRepository alerts;
    private final AlertAuditRepository audits;
    private final AlertInvestigator investigator;

    public AlertController(AlertRepository alerts, AlertAuditRepository audits, AlertInvestigator investigator) {
        this.alerts = alerts;
        this.audits = audits;
        this.investigator = investigator;
    }

    public record DecisionRequest(
            @NotBlank @Pattern(regexp = "CLEAR|ESCALATE|NEED_INFO") String decision,
            @NotBlank String note,
            @NotBlank String analyst) {}

    // GET /alerts  or  GET /alerts?status=AUTO_CLEARED
    @GetMapping("/alerts")
    public List<Alert> listAlerts(@RequestParam(defaultValue = "OPEN") String status) {
        return alerts.findByStatusOrderByCreatedAtDesc(status);
    }

    @GetMapping("/alerts/{id}")
    public Alert getAlert(@PathVariable Long id) {
        return alerts.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
    }

    // The bot does the first pass and writes a draft case summary
    @PostMapping("/alerts/{id}/investigate")
    public Alert investigate(@PathVariable Long id) {
        return investigator.investigate(id);
    }

    // The analyst has the final say
    @PutMapping("/alerts/{id}/decision")
    public Alert decide(@PathVariable Long id, @Valid @RequestBody DecisionRequest body) {
        return investigator.decide(id, body.decision(), body.note(), body.analyst());
    }

    @GetMapping("/alerts/{id}/audit")
    public List<AlertAudit> audit(@PathVariable Long id) {
        return audits.findByAlertIdOrderByIdAsc(id);
    }
}
