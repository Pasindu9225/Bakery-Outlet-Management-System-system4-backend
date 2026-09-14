package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiniStoreAvailabilityDto {

    private Integer miniStoreId;
    private String miniStoreName;
    private BigDecimal availableQty;
}


