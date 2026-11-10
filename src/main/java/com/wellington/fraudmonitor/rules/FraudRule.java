package com.wellington.fraudmonitor.rules;

import com.wellington.fraudmonitor.model.Alert;
import com.wellington.fraudmonitor.model.Transaction;

import java.util.Optional;

/** Every detection rule looks at one transaction and either raises an alert or not. */
public interface FraudRule {
    Optional<Alert> evaluate(Transaction tx);
}
