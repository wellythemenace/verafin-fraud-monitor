package com.wellington.fraudmonitor.controller;

import com.wellington.fraudmonitor.model.Alert;
import com.wellington.fraudmonitor.repository.AlertRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class AlertController {

    private final AlertRepository alerts;

    public AlertController(AlertRepository alerts) {
        this.alerts = alerts;
    }

    // GET /alerts  or  GET /alerts?status=CLOSED
    @GetMapping("/alerts")
    public List<Alert> listAlerts(@RequestParam(defaultValue = "OPEN") String status) {
        return alerts.findByStatusOrderByCreatedAtDesc(status);
    }
}
