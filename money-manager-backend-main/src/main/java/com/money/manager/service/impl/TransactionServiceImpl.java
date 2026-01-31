package com.money.manager.service.impl;

import com.money.manager.dto.CategorySummary;
import com.money.manager.dto.DashboardStats;
import com.money.manager.dto.PagedResponse;
import com.money.manager.dto.TransactionRequest;
import com.money.manager.dto.TransactionResponse;
import com.money.manager.enums.Division;
import com.money.manager.enums.TransactionType;
import com.money.manager.exception.BusinessRuleException;
import com.money.manager.exception.ResourceNotFoundException;
import com.money.manager.model.Transaction;
import com.money.manager.repository.TransactionRepository;
import com.money.manager.service.TransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final MongoTemplate mongoTemplate;

    public TransactionServiceImpl(TransactionRepository transactionRepository, MongoTemplate mongoTemplate) {
        this.transactionRepository = transactionRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public TransactionResponse createTransaction(TransactionRequest request) {
        Transaction transaction = new Transaction();
        mapToEntity(request, transaction);
        // Store timestamps in UTC; never use server timezone
        transaction.setCreatedAt(Instant.now());
        transaction.setUpdatedAt(Instant.now());

        Transaction saved = transactionRepository.save(transaction);
        return mapToResponse(saved);
    }

    @Override
    public PagedResponse<TransactionResponse> getAllTransactions(int page, int size) {
        if (page < 0) {
            page = 0;
        }
        if (size <= 0) {
            size = 10;
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "transactionDate"));
        Page<Transaction> transactionPage = transactionRepository.findAll(pageable);

        List<TransactionResponse> content = transactionPage.getContent()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        PagedResponse<TransactionResponse> response = new PagedResponse<>();
        response.setContent(content);
        response.setPage(transactionPage.getNumber());
        response.setSize(transactionPage.getSize());
        response.setTotalElements(transactionPage.getTotalElements());
        response.setTotalPages(transactionPage.getTotalPages());
        response.setLast(transactionPage.isLast());

        return response;
    }

    @Override
    public TransactionResponse updateTransaction(String id, TransactionRequest request) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        // Check 12-hour rule (both in UTC)
        long hoursDiff = ChronoUnit.HOURS.between(transaction.getCreatedAt(), Instant.now());
        if (hoursDiff > 12) {
            throw new BusinessRuleException("Transaction cannot be edited after 12 hours");
        }

        mapToEntity(request, transaction);
        transaction.setUpdatedAt(Instant.now());

        Transaction saved = transactionRepository.save(transaction);
        return mapToResponse(saved);
    }

    @Override
    public void deleteTransaction(String id) {
        if (!transactionRepository.existsById(id)) {
            throw new ResourceNotFoundException("Transaction not found");
        }
        transactionRepository.deleteById(id);
    }

    @Override
    public List<TransactionResponse> filterTransactions(Instant startDate, Instant endDate, String category,
            Division division) {
        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        // UTC range: transactionDate >= start AND transactionDate < end (exclusive end)
        if (startDate != null && endDate != null) {
            criteriaList.add(Criteria.where("transactionDate").gte(startDate).lt(endDate));
        } else if (startDate != null) {
            criteriaList.add(Criteria.where("transactionDate").gte(startDate));
        } else if (endDate != null) {
            criteriaList.add(Criteria.where("transactionDate").lt(endDate));
        }

        if (category != null && !category.isEmpty()) {
            criteriaList.add(Criteria.where("category").is(category));
        }

        if (division != null) {
            criteriaList.add(Criteria.where("division").is(division));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        query.with(Sort.by(Sort.Direction.DESC, "transactionDate"));

        return mongoTemplate.find(query, Transaction.class)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public DashboardStats getDashboardStats(String period) {
        // All ranges in UTC; never use server timezone
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        Instant start;
        Instant end;

        if ("weekly".equalsIgnoreCase(period)) {
            start = todayUtc.minusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();
            end = todayUtc.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        } else if ("monthly".equalsIgnoreCase(period)) {
            start = todayUtc.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            end = todayUtc.withDayOfMonth(1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        } else if ("yearly".equalsIgnoreCase(period)) {
            start = todayUtc.withDayOfYear(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            end = todayUtc.withDayOfYear(1).plusYears(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        } else {
            start = todayUtc.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            end = todayUtc.withDayOfMonth(1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        Double income = calculateTotal(start, end, TransactionType.INCOME);
        Double expense = calculateTotal(start, end, TransactionType.EXPENSE);

        return new DashboardStats(income, expense, income - expense);
    }

    /** UTC range: start inclusive, end exclusive. No $month/$year. */
    private Double calculateTotal(Instant start, Instant end, TransactionType type) {
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("transactionDate").gte(start).lt(end).and("type").is(type)),
                Aggregation.group().sum("amount").as("total"));

        AggregationResults<DocumentWrapper> results = mongoTemplate.aggregate(aggregation, "transactions",
                DocumentWrapper.class);
        DocumentWrapper result = results.getUniqueMappedResult();
        return result != null ? result.getTotal() : 0.0;
    }

    // Helper class for aggregation result
    @org.springframework.data.mongodb.core.mapping.Document
    static class DocumentWrapper {
        private Double total;

        public Double getTotal() {
            return total;
        }

        public void setTotal(Double total) {
            this.total = total;
        }
    }

    @Override
    public List<CategorySummary> getCategorySummary(String period) {
        // Same UTC ranges as getDashboardStats; no server timezone
        LocalDate todayUtc = LocalDate.now(ZoneOffset.UTC);
        Instant start;
        Instant end;

        if ("weekly".equalsIgnoreCase(period)) {
            start = todayUtc.minusDays(7).atStartOfDay(ZoneOffset.UTC).toInstant();
            end = todayUtc.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        } else if ("monthly".equalsIgnoreCase(period)) {
            start = todayUtc.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            end = todayUtc.withDayOfMonth(1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        } else if ("yearly".equalsIgnoreCase(period)) {
            start = todayUtc.withDayOfYear(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            end = todayUtc.withDayOfYear(1).plusYears(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        } else {
            start = todayUtc.withDayOfMonth(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            end = todayUtc.withDayOfMonth(1).plusMonths(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        }

        Aggregation agg = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("transactionDate").gte(start).lt(end)),
                Aggregation.group("category", "type").sum("amount").as("totalAmount"));

        AggregationResults<org.bson.Document> results = mongoTemplate.aggregate(agg, "transactions",
                org.bson.Document.class);

        return results.getMappedResults().stream().map(doc -> {
            org.bson.Document id = (org.bson.Document) doc.get("_id");
            String cat = id.getString("category");
            String typeStr = id.getString("type");
            Double total = doc.getDouble("totalAmount");
            return new CategorySummary(cat, TransactionType.valueOf(typeStr), total);
        }).collect(Collectors.toList());
    }

    private void mapToEntity(TransactionRequest request, Transaction transaction) {
        transaction.setType(request.getType());
        transaction.setAmount(request.getAmount());
        transaction.setCategory(request.getCategory());
        transaction.setDivision(request.getDivision());
        transaction.setDescription(request.getDescription());
        transaction.setTransactionDate(request.getTransactionDate());
        transaction.setSourceAccount(request.getSourceAccount());
        transaction.setTargetAccount(request.getTargetAccount());
    }

    private TransactionResponse mapToResponse(Transaction transaction) {
        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setType(transaction.getType());
        response.setAmount(transaction.getAmount());
        response.setCategory(transaction.getCategory());
        response.setDivision(transaction.getDivision());
        response.setDescription(transaction.getDescription());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setSourceAccount(transaction.getSourceAccount());
        response.setTargetAccount(transaction.getTargetAccount());
        return response;
    }
}
