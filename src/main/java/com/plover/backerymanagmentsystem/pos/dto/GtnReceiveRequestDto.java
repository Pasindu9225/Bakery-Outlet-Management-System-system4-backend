package com.plover.backerymanagmentsystem.pos.dto;

import java.util.List;

/**
 * Request DTO for receiving GTN items
 */
public class GtnReceiveRequestDto {

    private Integer gtnId;
    private List<GtnReceiveItemDto> receivedItems;

    public GtnReceiveRequestDto() {
    }

    public GtnReceiveRequestDto(Integer gtnId, List<GtnReceiveItemDto> receivedItems) {
        this.gtnId = gtnId;
        this.receivedItems = receivedItems;
    }

    public Integer getGtnId() {
        return gtnId;
    }

    public void setGtnId(Integer gtnId) {
        this.gtnId = gtnId;
    }

    public List<GtnReceiveItemDto> getReceivedItems() {
        return receivedItems;
    }

    public void setReceivedItems(List<GtnReceiveItemDto> receivedItems) {
        this.receivedItems = receivedItems;
    }
}
