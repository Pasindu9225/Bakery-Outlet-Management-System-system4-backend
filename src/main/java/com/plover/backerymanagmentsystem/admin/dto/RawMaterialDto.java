package com.plover.backerymanagmentsystem.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterialDto {

    private Long id;
    private String category;
    private LocalDateTime createdAt;
    private Double currentStock;
    private String description;
    private LocalDate expireDate;
    private Boolean isActive;
    private String materialCode;
    private String materialName;
    private String batchNo;
    private Double minimumStockLevel;
    private Double unitCost;
    private String unitOfMeasure;
    private LocalDateTime updatedAt;
    private String brand;
    private Double maxStockLevel;
    private Boolean vatIncluded;
    private List<PackDetailsDto> packDetails;
    private Long supplierId;
    private String supplierName;
    private Long genericMaterialId;
    private Long brandId;
    private String genericMaterialName;
}
