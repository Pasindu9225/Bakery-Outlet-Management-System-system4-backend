package com.plover.backerymanagmentsystem.store_keeper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterialBatchDto {

    private String batchNo;
    private String supplier;
    private Double purchasePrice;
    private Double quantity; // Keeping for compatibility, maps to balance
    private Double receivedQuantity;
    private Double issuedQuantity;
    private Double balance;
    private String expiryDate;
    private String receiveDate;
    private Double minQty;
}


