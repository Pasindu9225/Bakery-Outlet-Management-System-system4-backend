package com.plover.backerymanagmentsystem.pos.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.plover.backerymanagmentsystem.pos.model.GtnSource;
import com.plover.backerymanagmentsystem.pos.model.GtnStatus;

/**
 * DTO representing a GTN with its items and product information
 */
public class GtnWithProductsDto {

    private Integer gtnId;
    private LocalDateTime date;
    private GtnStatus status;
    private GtnSource source;
    private UUID addedBy;
    private UUID approvedBy;
    private List<GtnProductItemDto> gtnItems;

    public GtnWithProductsDto() {
    }

    public GtnWithProductsDto(Integer gtnId, LocalDateTime date, GtnStatus status, GtnSource source,
            UUID addedBy, UUID approvedBy, List<GtnProductItemDto> gtnItems) {
        this.gtnId = gtnId;
        this.date = date;
        this.status = status;
        this.source = source;
        this.addedBy = addedBy;
        this.approvedBy = approvedBy;
        this.gtnItems = gtnItems;
    }

    public Integer getGtnId() {
        return gtnId;
    }

    public void setGtnId(Integer gtnId) {
        this.gtnId = gtnId;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public GtnStatus getStatus() {
        return status;
    }

    public void setStatus(GtnStatus status) {
        this.status = status;
    }

    public GtnSource getSource() {
        return source;
    }

    public void setSource(GtnSource source) {
        this.source = source;
    }

    public UUID getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(UUID addedBy) {
        this.addedBy = addedBy;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public List<GtnProductItemDto> getGtnItems() {
        return gtnItems;
    }

    public void setGtnItems(List<GtnProductItemDto> gtnItems) {
        this.gtnItems = gtnItems;
    }
}
