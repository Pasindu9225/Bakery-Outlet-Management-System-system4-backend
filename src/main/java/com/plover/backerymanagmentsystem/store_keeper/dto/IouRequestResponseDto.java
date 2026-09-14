package com.plover.backerymanagmentsystem.store_keeper.dto;

import com.plover.backerymanagmentsystem.store_keeper.model.IouStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IouRequestResponseDto {
    private Long id;
    private LocalDate requestDate;
    private String justification;
    private String receiverName;
    private IouStatus status;
    private Double totalEstimatedAmount;
    private Double issuedAmount;
    private Double totalActualAmount;
    private Double differenceAmount;
    private String invoiceNumber;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private UUID addedById;
    private String addedByName;
    private UUID approvedById;
    private String approvedByName;
    private UUID finalApprovedById;
    private String finalApprovedByName;

    private List<IouItemResponseDto> items;
}
