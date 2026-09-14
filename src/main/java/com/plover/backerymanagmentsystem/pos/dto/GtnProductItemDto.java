package com.plover.backerymanagmentsystem.pos.dto;

import java.time.LocalDateTime;

import com.plover.backerymanagmentsystem.pos.model.EntryStatus;
import com.plover.backerymanagmentsystem.pos.model.GtnStatus;
import com.plover.backerymanagmentsystem.pos.model.ProductUnit;

/**
 * DTO representing a GTN item with product information
 */
public class GtnProductItemDto {

    private Integer gtnItemId;
    private Long productId;
    private String productName;
    private Double expectedQty;
    private Double receivedQty;
    private GtnStatus status;
    private LocalDateTime expiryDate;
    private ProductUnit unit;
    private EntryStatus entryStatus;
    private String remarks;

    public GtnProductItemDto() {
    }

    public GtnProductItemDto(Integer gtnItemId, Long productId, String productName, Double expectedQty,
            Double receivedQty, GtnStatus status, LocalDateTime expiryDate, ProductUnit unit, 
            EntryStatus entryStatus, String remarks) {
        this.gtnItemId = gtnItemId;
        this.productId = productId;
        this.productName = productName;
        this.expectedQty = expectedQty;
        this.receivedQty = receivedQty;
        this.status = status;
        this.expiryDate = expiryDate;
        this.unit = unit;
        this.entryStatus = entryStatus;
        this.remarks = remarks;
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

    public LocalDateTime getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDateTime expiryDate) {
        this.expiryDate = expiryDate;
    }

    public GtnStatus getStatus() {
        return status;
    }

    public void setStatus(GtnStatus status) {
        this.status = status;
    }

    public ProductUnit getUnit() {
        return unit;
    }

    public void setUnit(ProductUnit unit) {
        this.unit = unit;
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
