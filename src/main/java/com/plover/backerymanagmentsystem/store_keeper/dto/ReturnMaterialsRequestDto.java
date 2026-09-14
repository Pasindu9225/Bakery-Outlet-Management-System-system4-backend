package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialReturnItem.ReturnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.ReturnReason;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnMaterialsRequestDto {

    @NotNull
    private Long returnId;
    @NotNull
    private LocalDate returnDate;
    @NotNull
    private Long supplierId;
    @NotNull
    private String supplierName;
    @NotNull
    private Integer numberOfItems;
    @NotNull
    private BigDecimal totalCost;

    @NotEmpty
    @Valid
    private List<ReturnItemDto> returnItems;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReturnItemDto {
        @NotNull
        private Long returnItemId;
        @NotNull
        private Long rawMaterialId;
        @NotNull
        private String rawMaterialName;
        @NotNull
        private String unitOfMeasure;
        @NotNull
        private Double returnQuantity;
        @NotNull
        private BigDecimal unitPrice;
        @NotNull
        private BigDecimal totalPrice;
        @NotNull
        private ReturnStatus status; // APPROVED required to deduct
        @NotNull
        private ReturnReason reason;
    }
}


