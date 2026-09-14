package com.plover.backerymanagmentsystem.pos.dto;

import java.time.LocalDateTime;

import com.plover.backerymanagmentsystem.pos.model.GtnSource;
import com.plover.backerymanagmentsystem.pos.model.ProductUnit;

/**
 * Request DTO for manual entry of products
 */
public class ManualEntryRequestDto {

    private Long productId;
    private Double quantity;
    private ProductUnit unit;
    private String remarks;
    private GtnSource source;
    private String userId; // UUID format from frontend
    private Long outletId;

    public ManualEntryRequestDto() {
    }

    public ManualEntryRequestDto(Long productId, Double quantity, ProductUnit unit,
            String remarks, GtnSource source, String userId, Long outletId) {
        this.productId = productId;
        this.quantity = quantity;
        this.unit = unit;
        this.remarks = remarks;
        this.source = source;
        this.userId = userId;
        this.outletId = outletId;
    }

    /**
     * Get hardcoded expiry date for manual entries This can be modified to
     * accept from frontend if needed in future
     */
    public LocalDateTime getExpiryDate() {
        // Hardcoded to 1 year from now - can be easily changed
        return LocalDateTime.now().plusYears(1);
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public GtnSource getSource() {
        return source;
    }

    public void setSource(GtnSource source) {
        this.source = source;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Long getOutletId() {
        return outletId;
    }

    public void setOutletId(Long outletId) {
        this.outletId = outletId;
    }
}
