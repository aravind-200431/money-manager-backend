package com.money.manager.service;

import com.money.manager.dto.CategorySummary;
import com.money.manager.dto.DashboardStats;
import com.money.manager.dto.PagedResponse;
import com.money.manager.dto.TransactionRequest;
import com.money.manager.dto.TransactionResponse;
import com.money.manager.enums.Division;

import java.time.Instant;
import java.util.List;

public interface TransactionService {
    TransactionResponse createTransaction(TransactionRequest request);

    PagedResponse<TransactionResponse> getAllTransactions(int page, int size);

    TransactionResponse updateTransaction(String id, TransactionRequest request);

    void deleteTransaction(String id);

    /** start inclusive, end exclusive (UTC). */
    List<TransactionResponse> filterTransactions(Instant startDate, Instant endDate, String category,
            Division division);

    DashboardStats getDashboardStats(String period); // weekly, monthly, yearly (UTC ranges)

    List<CategorySummary> getCategorySummary(String period);
}
