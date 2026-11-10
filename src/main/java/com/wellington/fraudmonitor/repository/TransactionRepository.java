package com.wellington.fraudmonitor.repository;

import com.wellington.fraudmonitor.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // SELECT * FROM transactions WHERE account_id = ? ORDER BY occurred_at DESC
    List<Transaction> findByAccountIdOrderByOccurredAtDesc(Long accountId);

    // SELECT * FROM transactions WHERE account_id = ? AND type = ? AND occurred_at BETWEEN ? AND ?
    // This is the query the (account_id, occurred_at) index exists for.
    List<Transaction> findByAccountIdAndTypeAndOccurredAtBetween(
            Long accountId, String type, LocalDateTime from, LocalDateTime to);
}
