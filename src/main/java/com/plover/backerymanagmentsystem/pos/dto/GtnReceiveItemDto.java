package com.plover.backerymanagmentsystem.pos.dto;

/**
 * DTO representing an item to be received in a GTN
 */
public class GtnReceiveItemDto {

    private Integer gtnItemId;
    private Double receivedQty;

    public GtnReceiveItemDto() {
    }

    public GtnReceiveItemDto(Integer gtnItemId, Double receivedQty) {
        this.gtnItemId = gtnItemId;
        this.receivedQty = receivedQty;
    }

    public Integer getGtnItemId() {
        return gtnItemId;
    }

    public void setGtnItemId(Integer gtnItemId) {
        this.gtnItemId = gtnItemId;
    }

    public Double getReceivedQty() {
        return receivedQty;
    }

    public void setReceivedQty(Double receivedQty) {
        this.receivedQty = receivedQty;
    }
}
