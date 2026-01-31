package com.money.manager.dto;

import com.money.manager.enums.TransactionType;

public class CategorySummary {
    private String category;
    private TransactionType type;
    private Double totalAmount;

    public CategorySummary() {
    }

    public CategorySummary(String category, TransactionType type, Double totalAmount) {
        this.category = category;
        this.type = type;
        this.totalAmount = totalAmount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public TransactionType getType() {
        return type;
    }

    public void setType(TransactionType type) {
        this.type = type;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }
}
