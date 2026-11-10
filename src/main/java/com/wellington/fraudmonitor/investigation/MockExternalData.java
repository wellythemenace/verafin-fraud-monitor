package com.wellington.fraudmonitor.investigation;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * MOCK external data. In a real system these would be licensed sources:
 * a sanctions/watchlist provider and company / property / brokerage records.
 * Everything here is fake, so the demo needs no web scraping or personal data.
 */
@Component
public class MockExternalData {

    private static final Set<String> SANCTIONED = Set.of("viktor petrov", "ivan drago");

    private static final Map<String, List<String>> PUBLIC_RECORDS = Map.of(
            "home seller", List.of("Property sale registered 2026-11-05, approx. $450,000"),
            "day trader", List.of("Brokerage withdrawal of approx. $38,000 on 2026-11-09"));

    public boolean isSanctioned(String customerName) {
        return SANCTIONED.contains(customerName.trim().toLowerCase());
    }

    public List<String> publicRecordsFor(String customerName) {
        return PUBLIC_RECORDS.getOrDefault(customerName.trim().toLowerCase(), List.of());
    }
}
