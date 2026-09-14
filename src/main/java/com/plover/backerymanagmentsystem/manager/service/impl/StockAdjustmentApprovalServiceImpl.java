package com.plover.backerymanagmentsystem.manager.service.impl;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;
import com.plover.backerymanagmentsystem.manager.dto.StockAdjustmentApprovalRequestDto;
import com.plover.backerymanagmentsystem.manager.dto.StockAdjustmentApprovalResponseDto;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.service.StockAdjustmentApprovalService;
import com.plover.backerymanagmentsystem.store_keeper.exception.StockAdjustmentException;
import com.plover.backerymanagmentsystem.store_keeper.exception.StockAdjustmentNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjust;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentStatus;
import com.plover.backerymanagmentsystem.store_keeper.repository.StockAdjustRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of StockAdjustmentApprovalService. Handles stock adjustment
 * approval/rejection with stock updates.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockAdjustmentApprovalServiceImpl implements StockAdjustmentApprovalService {

    private final StockAdjustRepository stockAdjustRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final AuthRepository authRepository;

    @Override
    public StockAdjustmentApprovalResponseDto processStockAdjustmentApproval(StockAdjustmentApprovalRequestDto requestDto) {
        log.info("Processing stock adjustment approval for ID: {} with status: {}",
                requestDto.getStockAdjustmentId(), requestDto.getStatus());

        // Validate and retrieve stock adjustment
        StockAdjust stockAdjustment = stockAdjustRepository.findById(requestDto.getStockAdjustmentId())
                .orElseThrow(() -> new StockAdjustmentNotFoundException(requestDto.getStockAdjustmentId()));

        // Validate current status (only PENDING adjustments can be approved/rejected)
        if (stockAdjustment.getStatus() != StockAdjustmentStatus.PENDING) {
            throw new StockAdjustmentException(
                    String.format("Stock adjustment ID %d is already %s and cannot be modified",
                            requestDto.getStockAdjustmentId(), stockAdjustment.getStatus()));
        }

        // Validate approver exists
        AuthModel approver = authRepository.findById(IdUtil.uuidToBytes(requestDto.getApproverId()))
                .orElseThrow(() -> new StockAdjustmentException(
                String.format("Approver not found with ID: %s", requestDto.getApproverId())));

        // Validate status is either APPROVED or REJECTED
        if (requestDto.getStatus() != StockAdjustmentStatus.APPROVED
                && requestDto.getStatus() != StockAdjustmentStatus.REJECTED) {
            throw new StockAdjustmentException("Status must be either APPROVED or REJECTED");
        }

        RawMaterial rawMaterial = stockAdjustment.getRawMaterial();
        Double updatedStockLevel = rawMaterial.getCurrentStock();
        String message;

        // Process based on approval status
        if (requestDto.getStatus() == StockAdjustmentStatus.APPROVED) {
            // Apply stock change for approved adjustments
            Double newStockLevel = rawMaterial.getCurrentStock() + stockAdjustment.getChangeQuantity();

            // Validate that stock won't go negative
            if (newStockLevel < 0) {
                // If stock level is set to zero (afterQuantity <= 0.001), or adjustment reason is EXPIRED/DAMAGE,
                // or if minor negative rounding occurred (>= -0.5), safely set newStockLevel to 0.0 instead of throwing an error
                if (newStockLevel >= -0.5
                        || (stockAdjustment.getAfterQuantity() != null && stockAdjustment.getAfterQuantity() <= 0.001)
                        || stockAdjustment.getReasonForAdjust() == com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentReason.EXPIRED
                        || stockAdjustment.getReasonForAdjust() == com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentReason.DAMAGE) {
                    newStockLevel = 0.0;
                } else {
                    throw new StockAdjustmentException(
                            String.format("Cannot approve adjustment: would result in negative stock (%.2f) for material: %s",
                                    newStockLevel, rawMaterial.getMaterialName()));
                }
            }

            // Update raw material stock
            rawMaterial.setCurrentStock(newStockLevel);
            rawMaterial.setUpdatedAt(LocalDateTime.now());
            rawMaterialRepository.save(rawMaterial);

            updatedStockLevel = newStockLevel;
            message = String.format("Stock adjustment approved. Material stock updated from %.2f to %.2f",
                    stockAdjustment.getBeforeQuantity(), newStockLevel);

            log.info("Approved stock adjustment ID: {}. Updated {} stock from {} to {}",
                    stockAdjustment.getId(), rawMaterial.getMaterialName(),
                    stockAdjustment.getBeforeQuantity(), newStockLevel);
        } else {
            // Rejected - no stock changes
            message = "Stock adjustment rejected. No changes made to material stock.";
            log.info("Rejected stock adjustment ID: {}", stockAdjustment.getId());
        }

        // Update stock adjustment record
        stockAdjustment.setStatus(requestDto.getStatus());
        stockAdjustment.setApprovedBy(approver);
        stockAdjustment.setUpdatedAt(LocalDateTime.now());

        StockAdjust savedAdjustment = stockAdjustRepository.save(stockAdjustment);

        // Build response
        return StockAdjustmentApprovalResponseDto.builder()
                .stockAdjustmentId(savedAdjustment.getId())
                .rawMaterialId(rawMaterial.getId())
                .rawMaterialName(rawMaterial.getMaterialName())
                .changeQuantity(savedAdjustment.getChangeQuantity())
                .beforeQuantity(savedAdjustment.getBeforeQuantity())
                .afterQuantity(savedAdjustment.getAfterQuantity())
                .updatedStockLevel(updatedStockLevel)
                .status(savedAdjustment.getStatus())
                .approvedBy(IdUtil.bytesToUuid(approver.getId()))
                .approvedByName(approver.getFirstName() + " " + approver.getLastName())
                .approvedAt(savedAdjustment.getUpdatedAt())
                .message(message)
                .build();
    }
}
