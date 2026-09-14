package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialReturnItem.ReturnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.ReturnReason;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRawMaterialReturnRequestDto {

    @NotNull
    private Long returnId;
    @NotNull
    private LocalDate returnDate;
    @NotNull
    private Long supplierId;
    private String supplierName;
    @NotNull
    private Integer numberOfItems;
    @NotNull
    private BigDecimal totalCost;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @NotNull
    private List<UpdateItemDto> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateItemDto {
        @NotNull
        private Long returnItemId;
        @NotNull
        private Long rawMaterialId;
        private String rawMaterialName;
        private String category;
        private String unitOfMeasure;
        @NotNull
        private Double returnQuantity;
        @NotNull
        private BigDecimal unitPrice;
        @NotNull
        private BigDecimal totalPrice;
        @NotNull
        private ReturnStatus status;
        @NotNull
        private ReturnReason reason;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }
}


