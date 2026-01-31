package com.money.manager.repository;

import com.money.manager.model.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * All date queries use UTC Instant. End is exclusive (transactionDate < end).
 */
@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    List<Transaction> findByTransactionDateGreaterThanEqualAndTransactionDateLessThanOrderByTransactionDateDesc(
            Instant start, Instant end);
}
