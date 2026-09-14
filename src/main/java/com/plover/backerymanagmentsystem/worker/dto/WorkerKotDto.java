package com.plover.backerymanagmentsystem.worker.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkerKotDto {
    private Long id;
    private String orderNumber;
    private String status;
    private LocalDateTime orderDate;
    private List<WorkerKotItemDto> items;
    private Integer totalQuantity;
}
