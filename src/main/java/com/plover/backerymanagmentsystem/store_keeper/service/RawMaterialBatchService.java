package com.plover.backerymanagmentsystem.store_keeper.service;

import java.math.BigDecimal;

/**
 * Service interface for managing raw material batches.
 */
public interface RawMaterialBatchService {

    /**
     * Creates a new batch of raw material based on existing material details.
     *
     * @param existingMaterialId the ID of the existing raw material to copy details from
     * @param batchNo the batch number for the new batch
     * @param receivedQuantity the quantity received for this batch
     * @param unitCost the price per unit for this batch
     * @return the ID of the newly created batch
     */
    Long createNewBatch(Long existingMaterialId, String batchNo, BigDecimal receivedQuantity, String supplierName, BigDecimal unitCost, java.time.LocalDate expireDate);
}
