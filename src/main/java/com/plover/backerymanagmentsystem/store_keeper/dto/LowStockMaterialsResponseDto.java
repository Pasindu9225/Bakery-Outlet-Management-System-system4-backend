package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockMaterialsResponseDto {

    private List<LowStockMaterialDto> lowStockMaterials;
    private Integer totalCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockMaterialDto {

        private Long materialId;
        private String materialName;
        private String materialCode;
        private String category;
        private Double currentStock;
        private Double minimumStockLevel;
        private Double maxStockLevel;
        private Double stockDeficit; // How much below minimum level (minimumStockLevel - currentStock)
        private Double unitCost;
        private String unitOfMeasure;
        private Boolean isActive;
        private String stockStatus; // "LOW_STOCK", "OUT_OF_STOCK", "NO_MINIMUM_SET"
        private String notes; // Additional information about the material status
        private String brand;
        private String genericMaterialName;
        private java.util.List<SupplierDetail> suppliers;
        private Boolean isPoCreated; // Whether a purchase order exists for this material
        private PurchaseOrderInfo purchaseOrderInfo; // Purchase order details if exists
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SupplierDetail {
        private Long supplierId;
        private String name;
        private String brand;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseOrderInfo {

        private String estimatedDeliveryDate; // Formatted date string
        private Double totalCost;
        private Integer requiredQty;
        private Integer receivedQty;
    }
}
