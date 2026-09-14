package com.plover.backerymanagmentsystem.store_keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductionPlanSummaryResponseDto {
    
    private List<ProductionPlanDto> productionPlans;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductionPlanDto {
        private Long planId;
        private String planName;
        private String productionDate;
        private String createdBy;
        private String remarks;
        private Double totalEstimatedCost;
        private String status;
        private List<SummaryProductDto> summaryProducts;
        private List<ConsolidatedProductDto> consolidatedProducts;
        private List<RawMaterialSummaryDto> rawMaterials;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryProductDto {
        private Long productId;
        private String productName;
        private Integer plannedQuantity;
        private String unit;
        private String productionCenter;
        private String category;
        private List<RequiredSemiProductDto> requiredSemiProducts;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RequiredSemiProductDto {
        private Long semiProductRefId;
        private String semiProductName;
        private BigDecimal quantityUsed;
        private String unit;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConsolidatedProductDto {
        private Long semiProductId;
        private String semiProductName;
        private String productionCenter;
        private BigDecimal totalRequiredQty;
        private String unit;
        private String category;
        private List<UsageBreakdownDto> usageBreakdown;
        private List<ChildItemDto> children;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageBreakdownDto {
        private String usedFor;
        private BigDecimal quantityUsed;
        private String unit;
        private ConsolidatedProductDto semiProduct; // Nested semi-product if usedFor is itself a semi-product
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChildItemDto {
        private Long childItemId;
        private Long childRefId;
        private String childName;
        private String childType;
        private BigDecimal quantity;
        private String unit;
        private BigDecimal unitCost;
        private BigDecimal totalCost;
        private String productionCenter;
        private List<MiniStoreAvailabilityDto> miniStoreAvailability;
        private LocalDate expireDate;
        private BigDecimal currentStock;
        private List<ChildItemDto> children;
        private List<BatchDetailDto> batches;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BatchDetailDto {
        private Long id;
        private String batchNo;
        private Double currentStock;
        private LocalDate expireDate;
        private Double unitCost;
        private List<MiniStoreAvailabilityDto> miniStoreAvailability;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MiniStoreAvailabilityDto {
        private Long miniStoreId;
        private String miniStoreName;
        private BigDecimal availableQty;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RawMaterialSummaryDto {
        private Long rawMaterialId;
        private String rawMaterialName;
        private Integer plannedQuantity;
        private Double unitCost;
        private Double totalCost;
        private String unitOfMeasure;
        private String productionCenter;
        private LocalDate expireDate;
        private Double currentStock;
        private List<MiniStoreAvailabilityDto> miniStoreAvailability;
        private List<BatchDetailDto> batches;
    }
}
