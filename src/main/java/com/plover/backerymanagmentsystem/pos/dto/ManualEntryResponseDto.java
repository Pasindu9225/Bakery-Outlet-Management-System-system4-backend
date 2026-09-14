package com.plover.backerymanagmentsystem.pos.dto;

import java.time.LocalDateTime;

import com.plover.backerymanagmentsystem.pos.model.GtnSource;
import com.plover.backerymanagmentsystem.pos.model.ProductUnit;

/**
 * Response DTO for manual entry operation
 */
public class ManualEntryResponseDto {

    private boolean success;
    private String message;
    private Integer gtnId;
    private Integer gtnItemId;
    private Long productId;
    private String productName;
    private Double quantity;
    private ProductUnit unit;
    private GtnSource source;
    private LocalDateTime entryDate;
    private LocalDateTime expiryDate;
    private String remarks;

    public ManualEntryResponseDto() {
    }

    public ManualEntryResponseDto(boolean success, String message, Integer gtnId,
            Integer gtnItemId, Long productId, String productName, Double quantity,
            ProductUnit unit, GtnSource source, LocalDateTime entryDate,
            LocalDateTime expiryDate, String remarks) {
        this.success = success;
        this.message = message;
        this.gtnId = gtnId;
        this.gtnItemId = gtnItemId;
        this.productId = productId;
        this.productName = productName;
        this.quantity = quantity;
        this.unit = unit;
        this.source = source;
        this.entryDate = entryDate;
        this.expiryDate = expiryDate;
        this.remarks = remarks;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getGtnId() {
        return gtnId;
    }

    public void setGtnId(Integer gtnId) {
        this.gtnId = gtnId;
    }

    public Integer getGtnItemId() {
        return gtnItemId;
    }

    public void setGtnItemId(Integer gtnItemId) {
        this.gtnItemId = gtnItemId;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public ProductUnit getUnit() {
        return unit;
    }

    public void setUnit(ProductUnit unit) {
        this.unit = unit;
    }

    public GtnSource getSource() {
        return source;
    }

    public void setSource(GtnSource source) {
        this.source = source;
    }

    public LocalDateTime getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDateTime entryDate) {
        this.entryDate = entryDate;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
