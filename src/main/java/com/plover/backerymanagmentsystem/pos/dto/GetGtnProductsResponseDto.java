package com.plover.backerymanagmentsystem.pos.dto;

import java.util.List;

/**
 * Response DTO for getting GTN products
 */
public class GetGtnProductsResponseDto {

    private boolean success;
    private String message;
    private List<GtnWithProductsDto> gtns;
    private int totalCount;

    public GetGtnProductsResponseDto() {
    }

    public GetGtnProductsResponseDto(boolean success, String message, List<GtnWithProductsDto> gtns, int totalCount) {
        this.success = success;
        this.message = message;
        this.gtns = gtns;
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

    public List<GtnWithProductsDto> getGtns() {
        return gtns;
    }

    public void setGtns(List<GtnWithProductsDto> gtns) {
        this.gtns = gtns;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
