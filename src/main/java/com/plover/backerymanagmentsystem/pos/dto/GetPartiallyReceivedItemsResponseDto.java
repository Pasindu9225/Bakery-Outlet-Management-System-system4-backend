package com.plover.backerymanagmentsystem.pos.dto;

import java.util.List;

/**
 * Response DTO for getting partially received items
 */
public class GetPartiallyReceivedItemsResponseDto {

    private boolean success;
    private String message;
    private List<PartiallyReceivedItemDto> partiallyReceivedItems;

    private int totalCount;

    public GetPartiallyReceivedItemsResponseDto() {
    }

    public GetPartiallyReceivedItemsResponseDto(boolean success, String message,
            List<PartiallyReceivedItemDto> partiallyReceivedItems, int totalCount) {
        this.success = success;
        this.message = message;
        this.partiallyReceivedItems = partiallyReceivedItems;
        this.totalCount = totalCount;
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

    public List<PartiallyReceivedItemDto> getPartiallyReceivedItems() {
        return partiallyReceivedItems;
    }

    public void setPartiallyReceivedItems(List<PartiallyReceivedItemDto> partiallyReceivedItems) {
        this.partiallyReceivedItems = partiallyReceivedItems;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
