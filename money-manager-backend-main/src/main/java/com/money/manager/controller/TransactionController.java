package com.money.manager.controller;

import com.money.manager.dto.CategorySummary;
import com.money.manager.dto.DashboardStats;
import com.money.manager.dto.PagedResponse;
import com.money.manager.dto.TransactionRequest;
import com.money.manager.dto.TransactionResponse;
import com.money.manager.enums.Division;
import com.money.manager.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping("/transactions")
    public ResponseEntity<TransactionResponse> createTransaction(@Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.createTransaction(request));
    }

    @GetMapping("/transactions")
    public ResponseEntity<PagedResponse<TransactionResponse>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionService.getAllTransactions(page, size));
    }

    @PutMapping("/transactions/{id}")
    public ResponseEntity<TransactionResponse> updateTransaction(@PathVariable String id,
            @Valid @RequestBody TransactionRequest request) {
        return ResponseEntity.ok(transactionService.updateTransaction(id, request));
    }

    /** startDate/endDate must be UTC ISO-8601 (e.g. 2026-01-01T00:00:00Z). End is exclusive. */
    @GetMapping("/transactions/filter")
    public ResponseEntity<List<TransactionResponse>> filterTransactions(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Division division) {
        return ResponseEntity.ok(transactionService.filterTransactions(startDate, endDate, category, division));
    }

    @GetMapping("/dashboard/weekly")
    public ResponseEntity<DashboardStats> getWeeklyDashboard() {
        return ResponseEntity.ok(transactionService.getDashboardStats("weekly"));
    }

    @GetMapping("/dashboard/monthly")
    public ResponseEntity<DashboardStats> getMonthlyDashboard() {
        return ResponseEntity.ok(transactionService.getDashboardStats("monthly"));
    }

    @GetMapping("/dashboard/yearly")
    public ResponseEntity<DashboardStats> getYearlyDashboard() {
        return ResponseEntity.ok(transactionService.getDashboardStats("yearly"));
    }

    @GetMapping("/summary/categories")
    public ResponseEntity<List<CategorySummary>> getCategorySummary(
            @RequestParam(required = false, defaultValue = "monthly") String period) {
        return ResponseEntity.ok(transactionService.getCategorySummary(period));
    }
}
