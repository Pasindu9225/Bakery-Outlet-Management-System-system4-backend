package com.plover.backerymanagmentsystem.pos.dto;

import com.plover.backerymanagmentsystem.pos.model.DayProductionItem;

/**
 * Response DTO for day production items with product details
 */
public class DayProductionItemResponseDto {

    private Integer dayProductionItemId;
    private Long productId;
    private String productName;
    private String productCode;
    private Double unitPrice;
    private Integer orderedQty;
    private Integer receivedQty;
    private Integer currentQty;
    private String categoryName;
    private Boolean isFastMoving;
    private Boolean isKotEnabled;
    private String unitName;

    public DayProductionItemResponseDto() {
    }

    public DayProductionItemResponseDto(Integer dayProductionItemId, Long productId, String productName,
            String productCode, Double unitPrice, Integer orderedQty, Integer receivedQty, Integer currentQty,
            String categoryName, Boolean isFastMoving, Boolean isKotEnabled) {
        this.dayProductionItemId = dayProductionItemId;
        this.productId = productId;
        this.productName = productName;
        this.productCode = productCode;
        this.unitPrice = unitPrice;
        this.orderedQty = orderedQty;
        this.receivedQty = receivedQty;
        this.currentQty = currentQty;
        this.categoryName = categoryName;
        this.isFastMoving = isFastMoving;
        this.isKotEnabled = isKotEnabled;
    }

    // Getters and Setters
    public Integer getDayProductionItemId() {
        return dayProductionItemId;
    }

    public void setDayProductionItemId(Integer dayProductionItemId) {
        this.dayProductionItemId = dayProductionItemId;
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

    public Double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(Double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public Integer getOrderedQty() {
        return orderedQty;
    }

    public void setOrderedQty(Integer orderedQty) {
        this.orderedQty = orderedQty;
    }

    public Integer getReceivedQty() {
        return receivedQty;
    }

    public void setReceivedQty(Integer receivedQty) {
        this.receivedQty = receivedQty;
    }

    public Integer getCurrentQty() {
        return currentQty;
    }

    public void setCurrentQty(Integer currentQty) {
        this.currentQty = currentQty;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Boolean getIsFastMoving() {
        return isFastMoving;
    }

    public void setIsFastMoving(Boolean isFastMoving) {
        this.isFastMoving = isFastMoving;
    }

    public Boolean getIsKotEnabled() {
        return isKotEnabled;
    }

    public void setIsKotEnabled(Boolean isKotEnabled) {
        this.isKotEnabled = isKotEnabled;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    /**
     * Factory method to create DTO from entity
     */
    public static DayProductionItemResponseDto fromEntity(DayProductionItem dayProductionItem) {
        DayProductionItemResponseDto dto = new DayProductionItemResponseDto(
                dayProductionItem.getDayProductionItemId(),
                dayProductionItem.getProduct().getId(),
                dayProductionItem.getProduct().getProductName(),
                dayProductionItem.getProduct().getProductCode(),
                dayProductionItem.getProduct().getSalePrice() != null && dayProductionItem.getProduct().getSalePrice() > 0 ?
                        dayProductionItem.getProduct().getSalePrice() : dayProductionItem.getProduct().getUnitPrice(),
                dayProductionItem.getOrderedQty(),
                dayProductionItem.getReceivedQty(),
                dayProductionItem.getCurrentQty(),
                dayProductionItem.getProduct().getCategory(),
                dayProductionItem.getProduct().getIsFastMoving(),
                dayProductionItem.getProduct().getIsKotEnabled());
        dto.setUnitName(dayProductionItem.getProduct().getUnitOfMeasure());
        return dto;
    }
}
