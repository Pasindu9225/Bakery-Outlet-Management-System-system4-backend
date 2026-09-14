package com.plover.backerymanagmentsystem.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutletTransferResponseDto {
    private Long id;
    private Long sourceOutletId;
    private String sourceOutletName;
    private Long destinationOutletId;
    private String destinationOutletName;
    private Long productId;
    private String productName;
    private Integer requestedQuantity;
    private Integer approvedQuantity;
    private String status;
    private LocalDateTime createdAt;
}
