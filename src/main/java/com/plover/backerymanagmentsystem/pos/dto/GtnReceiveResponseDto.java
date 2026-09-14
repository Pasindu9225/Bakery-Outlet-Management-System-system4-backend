package com.plover.backerymanagmentsystem.pos.dto;

/**
 * Response DTO for GTN receive operation
 */
public class GtnReceiveResponseDto {

    private boolean success;
    private String message;
    private Integer gtnId;
    private Integer productionId;
    private int itemsProcessed;

    public GtnReceiveResponseDto() {
    }

    public GtnReceiveResponseDto(boolean success, String message, Integer gtnId, Integer productionId, int itemsProcessed) {
        this.success = success;
        this.message = message;
        this.gtnId = gtnId;
        this.productionId = productionId;
        this.itemsProcessed = itemsProcessed;
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

    public Integer getProductionId() {
        return productionId;
    }

    public void setProductionId(Integer productionId) {
        this.productionId = productionId;
    }

    public int getItemsProcessed() {
        return itemsProcessed;
    }

    public void setItemsProcessed(int itemsProcessed) {
        this.itemsProcessed = itemsProcessed;
    }
}
