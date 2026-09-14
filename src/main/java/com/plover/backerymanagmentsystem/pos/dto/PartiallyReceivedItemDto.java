package com.plover.backerymanagmentsystem.pos.dto;

import java.time.LocalDateTime;

import com.plover.backerymanagmentsystem.pos.model.EntryStatus;
import com.plover.backerymanagmentsystem.pos.model.GtnStatus;
import com.plover.backerymanagmentsystem.pos.model.ProductUnit;

/**
 * DTO for partially received GTN items with full details
 */
public class PartiallyReceivedItemDto {

    private Integer gtnItemId;
    private Integer gtnId;
    private LocalDateTime gtnDate;
    private Long productId;
    private String productName;
    private String productCode;
    private Double expectedQty;
    private Double receivedQty;
    private Double remainingQty;
    private LocalDateTime expiryDate;
    private ProductUnit unit;
    private GtnStatus status;
    private EntryStatus entryStatus;

    private String remarks;

    public PartiallyReceivedItemDto() {
    }

    public PartiallyReceivedItemDto(Integer gtnItemId, Integer gtnId, LocalDateTime gtnDate,
            Long productId, String productName, String productCode, Double expectedQty, Double receivedQty,
            Double remainingQty, LocalDateTime expiryDate, ProductUnit unit,
            GtnStatus status, EntryStatus entryStatus, String remarks) {
        this.gtnItemId = gtnItemId;
        this.gtnId = gtnId;
        this.gtnDate = gtnDate;
        this.productId = productId;
        this.productName = productName;
        this.productCode = productCode;
        this.expectedQty = expectedQty;
        this.receivedQty = receivedQty;
        this.remainingQty = remainingQty;
        this.expiryDate = expiryDate;
        this.unit = unit;
        this.status = status;
        this.entryStatus = entryStatus;
        this.remarks = remarks;
    }

    // Getters and setters
    public Integer getGtnItemId() {
        return gtnItemId;
    }

    public void setGtnItemId(Integer gtnItemId) {
        this.gtnItemId = gtnItemId;
    }

    public Integer getGtnId() {
        return gtnId;
    }

    public void setGtnId(Integer gtnId) {
        this.gtnId = gtnId;
    }

    public LocalDateTime getGtnDate() {
        return gtnDate;
    }

    public void setGtnDate(LocalDateTime gtnDate) {
        this.gtnDate = gtnDate;
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

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public Double getExpectedQty() {
        return expectedQty;
    }

    public void setExpectedQty(Double expectedQty) {
        this.expectedQty = expectedQty;
    }

    public Double getReceivedQty() {
        return receivedQty;
    }

    public void setReceivedQty(Double receivedQty) {
        this.receivedQty = receivedQty;
    }

    public Double getRemainingQty() {
        return remainingQty;
    }

    public void setRemainingQty(Double remainingQty) {
        this.remainingQty = remainingQty;
    }

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public ProductUnit getUnit() {
        return unit;
    }

    public void setUnit(ProductUnit unit) {
        this.unit = unit;
    }

    public GtnStatus getStatus() {
        return status;
    }

    public void setStatus(GtnStatus status) {
        this.status = status;
    }

    public EntryStatus getEntryStatus() {
        return entryStatus;
    }

    public void setEntryStatus(EntryStatus entryStatus) {
        this.entryStatus = entryStatus;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
