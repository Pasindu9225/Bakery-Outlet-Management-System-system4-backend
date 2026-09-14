package com.plover.backerymanagmentsystem.worker.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TransferNoteDto {
    private Long id;
    private String transferNumber;
    private Long sourceProductionCenterId;
    private Long destinationOutletId;
    private Long destinationMpcId;
    private String status;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime receivedAt;
    private List<TransferNoteItemDto> items;
}
