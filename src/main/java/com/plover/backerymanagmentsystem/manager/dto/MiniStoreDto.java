package com.plover.backerymanagmentsystem.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiniStoreDto {
    
    private Integer miniStoreId;
    private String name;
    private LocalDate storeDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
