package com.plover.backerymanagmentsystem.store_keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductSummary {
    
    private Long productId;
    private String productName;
    private String productCode;
    private String description;
    private Double unitPrice;
    private String category;
    private Integer quantity;
    private Double exactQuantity;
    private Double unitCost;
    private Double totalCost;
    private Double estimatedRawMaterialCost;
    private String productionCenterName;
    private Long productionCenterId;
    private Boolean needsRecipe;
    private java.util.List<MiniStoreAvailabilityDto> miniStoreAvailability;
}
