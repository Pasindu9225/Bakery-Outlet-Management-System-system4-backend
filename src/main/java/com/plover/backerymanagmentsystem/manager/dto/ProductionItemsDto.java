package com.plover.backerymanagmentsystem.manager.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data                       // generates getters, setters, equals, hashCode, toString
@Builder                    // enables builder pattern
@NoArgsConstructor          // generates no-arg constructor
@AllArgsConstructor         // generates all-args constructor
public class ProductionItemsDto {

    private Long id;                        // 44
    private Long productId;                 // 1
    private String productName;
    private Integer quantity;
    private Double unitCost;
    private Double totalCost;
    private Double estimatedRawMaterialCost;
}
