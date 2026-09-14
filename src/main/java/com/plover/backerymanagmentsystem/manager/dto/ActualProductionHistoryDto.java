package com.plover.backerymanagmentsystem.manager.dto;

import com.plover.backerymanagmentsystem.manager.model.ActualProductionHistoryAction;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ActualProductionHistoryDto {
    private Long id;
    private Long productId;
    private String productName;
    private Integer quantity;
    private ActualProductionHistoryAction actionType;
    private String referenceName;
    private LocalDateTime createdAt;
}
