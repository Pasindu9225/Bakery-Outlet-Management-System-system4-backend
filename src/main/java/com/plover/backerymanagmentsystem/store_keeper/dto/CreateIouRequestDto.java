package com.plover.backerymanagmentsystem.store_keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateIouRequestDto {
    private UUID userId; // The Storekeeper requesting it
    private LocalDate requestDate;
    private String justification;
    private String receiverName;
    private List<CreateIouItemDto> items;
}
