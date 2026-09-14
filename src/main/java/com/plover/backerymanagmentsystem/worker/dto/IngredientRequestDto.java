package com.plover.backerymanagmentsystem.worker.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IngredientRequestDto {
    private Long id;
    private Long productionPlanId;
    private Long productionCenterId;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime issuedAt;
    private LocalDateTime receivedAt;
    private List<IngredientRequestItemDto> items;
}
