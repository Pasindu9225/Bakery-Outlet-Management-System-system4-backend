package com.plover.backerymanagmentsystem.store_keeper.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionCenterResponseDto {
    private Long id;
    private String centerName;
    @JsonProperty("mini_store_id")
    private Integer miniStoreId;
}
