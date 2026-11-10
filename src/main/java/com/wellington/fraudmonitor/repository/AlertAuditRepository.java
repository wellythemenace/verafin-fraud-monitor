package com.wellington.fraudmonitor.repository;

import com.wellington.fraudmonitor.model.AlertAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertAuditRepository extends JpaRepository<AlertAudit, Long> {

    List<AlertAudit> findByAlertIdOrderByIdAsc(Long alertId);
}
