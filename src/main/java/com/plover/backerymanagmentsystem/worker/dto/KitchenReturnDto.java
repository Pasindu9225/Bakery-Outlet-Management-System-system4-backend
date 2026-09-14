package com.plover.backerymanagmentsystem.worker.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KitchenReturnDto {
    private Long id;
    private String returnNumber;
    private Long sourceProductionCenterId;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private List<KitchenReturnItemDto> items;
}
