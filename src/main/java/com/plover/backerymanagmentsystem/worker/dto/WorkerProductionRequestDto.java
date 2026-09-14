package com.plover.backerymanagmentsystem.worker.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkerProductionRequestDto {
    private Long id;
    private String planName;
    private LocalDateTime planDate;
    private String status;
    private String department;
    private String notes;
    private List<WorkerProductionRequestItemDto> items;
    private Integer totalQuantity;
    private Integer producedQuantity;
    private Boolean ingredientsConfirmed;
    private String ingredientRequestStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
