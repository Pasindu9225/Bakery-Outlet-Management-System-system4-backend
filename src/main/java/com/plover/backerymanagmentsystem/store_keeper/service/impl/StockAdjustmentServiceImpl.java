// package com.plover.backerymanagmentsystem.store_keeper.service.impl;
// import java.util.List;
// import java.util.stream.Collectors;
// import org.springframework.stereotype.Service;
// import org.springframework.transaction.annotation.Transactional;
// import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
// import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
// import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
// import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
// import com.plover.backerymanagmentsystem.store_keeper.dto.CreateStockAdjustmentRequestDto;
// import com.plover.backerymanagmentsystem.store_keeper.dto.StockAdjustmentResponseDto;
// import com.plover.backerymanagmentsystem.store_keeper.exception.RawMaterialNotFoundException;
// import com.plover.backerymanagmentsystem.store_keeper.exception.StockAdjustmentException;
// import com.plover.backerymanagmentsystem.store_keeper.exception.StockAdjustmentNotFoundException;
// import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjust;
// import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentStatus;
// import com.plover.backerymanagmentsystem.store_keeper.repository.StockAdjustRepository;
// import com.plover.backerymanagmentsystem.store_keeper.service.StockAdjustmentService;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// /**
//  * Implementation of StockAdjustmentService. Handles stock adjustment creation
//  * and management with comprehensive validation.
//  */
// @Service
// @RequiredArgsConstructor
// @Slf4j
// @Transactional
// public class StockAdjustmentServiceImpl implements StockAdjustmentService {
//     private final StockAdjustRepository stockAdjustRepository;
//     private final RawMaterialRepository rawMaterialRepository;
//     private final AuthRepository authRepository;
//     @Override
//     public StockAdjustmentResponseDto createStockAdjustment(CreateStockAdjustmentRequestDto requestDto) {
//         log.info("Creating stock adjustment for raw material ID: {}", requestDto.getRawMaterialId());
//         // Validate raw material exists and is active
//         RawMaterial rawMaterial = rawMaterialRepository.findById(requestDto.getRawMaterialId())
//                 .orElseThrow(() -> new RawMaterialNotFoundException(requestDto.getRawMaterialId()));
//         if (!rawMaterial.getIsActive()) {
//             throw new StockAdjustmentException(
//                     String.format("Cannot create adjustment for inactive raw material: %s", rawMaterial.getMaterialName()));
//         }
//         // Validate user exists
//         AuthModel user = authRepository.findById(requestDto.getUserId())
//                 .orElseThrow(() -> new StockAdjustmentException(
//                 String.format("User not found with ID: %s", requestDto.getUserId())));
//         // Validate adjustment quantity
//         if (requestDto.getAdjustmentQty() == null || requestDto.getAdjustmentQty() == 0) {
//             throw new StockAdjustmentException("Adjustment quantity cannot be null or zero");
//         }
//         // Create stock adjustment entity
//         StockAdjust stockAdjust = StockAdjust.builder()
//                 .rawMaterial(rawMaterial)
//                 .changeQuantity(requestDto.getAdjustmentQty())
//                 .beforeQuantity(rawMaterial.getCurrentStock())
//                 .reasonForAdjust(requestDto.getReasonForAdjustment())
//                 .remarks(requestDto.getRemarks())
//                 .createdAt(requestDto.getDate())
//                 .status(StockAdjustmentStatus.PENDING)
//                 .addedBy(user)
//                 .build();
//         // Save the adjustment
//         StockAdjust savedAdjustment = stockAdjustRepository.save(stockAdjust);
//         log.info("Stock adjustment created successfully with ID: {}", savedAdjustment.getId());
//         return mapToResponseDto(savedAdjustment);
//     }
//     @Override
//     @Transactional(readOnly = true)
//     public List<StockAdjustmentResponseDto> getStockAdjustmentsByRawMaterial(Long rawMaterialId) {
//         log.info("Retrieving stock adjustments for raw material ID: {}", rawMaterialId);
//         // Validate raw material exists
//         if (!rawMaterialRepository.existsById(rawMaterialId)) {
//             throw new RawMaterialNotFoundException(rawMaterialId);
//         }
//         List<StockAdjust> adjustments = stockAdjustRepository.findByRawMaterialIdOrderByCreatedAtDesc(rawMaterialId);
//         return adjustments.stream()
//                 .map(this::mapToResponseDto)
//                 .collect(Collectors.toList());
//     }
//     @Override
//     @Transactional(readOnly = true)
//     public List<StockAdjustmentResponseDto> getStockAdjustmentsByStatus(StockAdjustmentStatus status) {
//         log.info("Retrieving stock adjustments with status: {}", status);
//         List<StockAdjust> adjustments = stockAdjustRepository.findByStatusOrderByCreatedAtDesc(status);
//         return adjustments.stream()
//                 .map(this::mapToResponseDto)
//                 .collect(Collectors.toList());
//     }
//     @Override
//     @Transactional(readOnly = true)
//     public List<StockAdjustmentResponseDto> getAllPendingAdjustments() {
//         log.info("Retrieving all pending stock adjustments");
//         List<StockAdjust> adjustments = stockAdjustRepository.findAllPendingAdjustments();
//         return adjustments.stream()
//                 .map(this::mapToResponseDto)
//                 .collect(Collectors.toList());
//     }
//     @Override
//     @Transactional(readOnly = true)
//     public StockAdjustmentResponseDto getStockAdjustmentById(Long adjustmentId) {
//         log.info("Retrieving stock adjustment with ID: {}", adjustmentId);
//         StockAdjust adjustment = stockAdjustRepository.findById(adjustmentId)
//                 .orElseThrow(() -> new StockAdjustmentNotFoundException(adjustmentId));
//         return mapToResponseDto(adjustment);
//     }
//     @Override
//     @Transactional(readOnly = true)
//     public List<StockAdjustmentResponseDto> getAllStockAdjustments() {
//         log.info("Retrieving all stock adjustments");
//         List<StockAdjust> adjustments = stockAdjustRepository.findAllWithRawMaterialData();
//         log.info("Successfully retrieved {} stock adjustments", adjustments.size());
//         return adjustments.stream()
//                 .map(this::mapToResponseDto)
//                 .collect(Collectors.toList());
//     }
//     /**
//      * Maps StockAdjust entity to StockAdjustmentResponseDto.
//      *
//      * @param stockAdjust the stock adjust entity
//      * @return mapped response DTO
//      */
//     private StockAdjustmentResponseDto mapToResponseDto(StockAdjust stockAdjust) {
//         return StockAdjustmentResponseDto.builder()
//                 .id(stockAdjust.getId())
//                 .rawMaterialId(stockAdjust.getRawMaterial().getId())
//                 .rawMaterialName(stockAdjust.getRawMaterial().getMaterialName())
//                 .changeQuantity(stockAdjust.getChangeQuantity())
//                 .beforeQuantity(stockAdjust.getBeforeQuantity())
//                 .afterQuantity(stockAdjust.getAfterQuantity())
//                 .reasonForAdjust(stockAdjust.getReasonForAdjust())
//                 .remarks(stockAdjust.getRemarks())
//                 .createdAt(stockAdjust.getCreatedAt())
//                 .updatedAt(stockAdjust.getUpdatedAt())
//                 .status(stockAdjust.getStatus())
//                 .approvedBy(stockAdjust.getApprovedBy() != null ? stockAdjust.getApprovedBy().getId() : null)
//                 .approvedByName(stockAdjust.getApprovedBy() != null
//                         ? stockAdjust.getApprovedBy().getFirst_name() + " " + stockAdjust.getApprovedBy().getLast_name() : null)
//                 .addedBy(stockAdjust.getAddedBy().getId())
//                 .addedByName(stockAdjust.getAddedBy().getFirst_name() + " " + stockAdjust.getAddedBy().getLast_name())
//                 .build();
//     }
// }
//new code
package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreateStockAdjustmentRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.StockAdjustmentResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.exception.RawMaterialNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.exception.StockAdjustmentException;
import com.plover.backerymanagmentsystem.store_keeper.exception.StockAdjustmentNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjust;
import com.plover.backerymanagmentsystem.store_keeper.model.StockAdjustmentStatus;
import com.plover.backerymanagmentsystem.store_keeper.repository.StockAdjustRepository;
import com.plover.backerymanagmentsystem.store_keeper.service.StockAdjustmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;

/**
 * Implementation of StockAdjustmentService. Handles stock adjustment creation
 * and management with comprehensive validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StockAdjustmentServiceImpl implements StockAdjustmentService {

    private final StockAdjustRepository stockAdjustRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final AuthRepository authRepository;

    @Override
    public StockAdjustmentResponseDto createStockAdjustment(CreateStockAdjustmentRequestDto requestDto) {
        log.info("Creating stock adjustment for raw material ID: {}", requestDto.getRawMaterialId());

        // Validate raw material exists and is active
        RawMaterial rawMaterial = rawMaterialRepository.findById(requestDto.getRawMaterialId())
                .orElseThrow(() -> new RawMaterialNotFoundException(requestDto.getRawMaterialId()));

        if (!rawMaterial.getIsActive()) {
            throw new StockAdjustmentException(
                    String.format("Cannot create adjustment for inactive raw material: %s", rawMaterial.getMaterialName()));
        }

        // Validate user exists
        AuthModel user = authRepository.findById(IdUtil.uuidToBytes(requestDto.getUserId()))
                .orElseThrow(() -> new StockAdjustmentException(
                String.format("User not found with ID: %s", requestDto.getUserId())));

        // Validate adjustment quantity
        if (requestDto.getAdjustmentQty() == null || requestDto.getAdjustmentQty() == 0) {
            throw new StockAdjustmentException("Adjustment quantity cannot be null or zero");
        }

        // Helper method for rounding to 5 decimal places
        java.util.function.Function<Double, Double> roundTo5 = val -> {
            if (val == null) return null;
            return Math.round(val * 100000.0) / 100000.0;
        };

        Double beforeQty = rawMaterial.getCurrentStock() != null ? rawMaterial.getCurrentStock() : 0.0;
        Double changeQty = requestDto.getAdjustmentQty() != null ? requestDto.getAdjustmentQty() : 0.0;

        // Create stock adjustment entity
        StockAdjust stockAdjust = StockAdjust.builder()
                .rawMaterial(rawMaterial)
                .changeQuantity(roundTo5.apply(changeQty))
                .beforeQuantity(roundTo5.apply(beforeQty))
                .reasonForAdjust(requestDto.getReasonForAdjustment())
                .remarks(requestDto.getRemarks())
                .createdAt(requestDto.getDate())
                .status(StockAdjustmentStatus.PENDING)
                .addedBy(user)
                .build();

        // Save the adjustment
        StockAdjust savedAdjustment = stockAdjustRepository.save(stockAdjust);

        log.info("Stock adjustment created successfully with ID: {}", savedAdjustment.getId());

        return mapToResponseDto(savedAdjustment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentResponseDto> getStockAdjustmentsByRawMaterial(Long rawMaterialId) {
        log.info("Retrieving stock adjustments for raw material ID: {}", rawMaterialId);

        // Validate raw material exists
        if (!rawMaterialRepository.existsById(rawMaterialId)) {
            throw new RawMaterialNotFoundException(rawMaterialId);
        }

        List<StockAdjust> adjustments = stockAdjustRepository.findByRawMaterialIdOrderByCreatedAtDesc(rawMaterialId);

        return adjustments.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentResponseDto> getStockAdjustmentsByStatus(StockAdjustmentStatus status) {
        log.info("Retrieving stock adjustments with status: {}", status);

        List<StockAdjust> adjustments = stockAdjustRepository.findByStatusOrderByCreatedAtDesc(status);

        return adjustments.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentResponseDto> getAllPendingAdjustments() {
        log.info("Retrieving all pending stock adjustments");

        List<StockAdjust> adjustments = stockAdjustRepository.findAllPendingAdjustments();

        return adjustments.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public StockAdjustmentResponseDto getStockAdjustmentById(Long adjustmentId) {
        log.info("Retrieving stock adjustment with ID: {}", adjustmentId);

        StockAdjust adjustment = stockAdjustRepository.findById(adjustmentId)
                .orElseThrow(() -> new StockAdjustmentNotFoundException(adjustmentId));

        return mapToResponseDto(adjustment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockAdjustmentResponseDto> getAllStockAdjustments() {
        log.info("Retrieving all stock adjustments");
        try {
            List<StockAdjust> adjustments = stockAdjustRepository.findAllWithRawMaterialData();
            log.info("Successfully retrieved {} stock adjustments", adjustments.size());
            for (StockAdjust adj : adjustments) {
                log.debug("StockAdjust: id={}, status={}, reason={}", adj.getId(), adj.getStatus());
            }
            return adjustments.stream()
                    .map(this::mapToResponseDto)
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            log.error("Error retrieving stock adjustments", ex);
            throw ex;
        }
    }

    /**
     * Maps StockAdjust entity to StockAdjustmentResponseDto.
     *
     * @param stockAdjust the stock adjust entity
     * @return mapped response DTO
     */
    private StockAdjustmentResponseDto mapToResponseDto(StockAdjust stockAdjust) {
        RawMaterial rm = stockAdjust.getRawMaterial();
        String matName = "Unknown Material";
        if (rm != null) {
            matName = rm.getDisplayName();
            if (matName == null || matName.trim().isEmpty() || matName.equalsIgnoreCase("Unknown Material")) {
                matName = rm.getMaterialName();
            }
        }
        return StockAdjustmentResponseDto.builder()
                .id(stockAdjust.getId())
                .rawMaterialId(rm != null ? rm.getId() : null)
                .rawMaterialName(matName)
                .changeQuantity(stockAdjust.getChangeQuantity())
                .beforeQuantity(stockAdjust.getBeforeQuantity())
                .afterQuantity(stockAdjust.getAfterQuantity())
                .reasonForAdjust(stockAdjust.getReasonForAdjust())
                .remarks(stockAdjust.getRemarks())
                .createdAt(stockAdjust.getCreatedAt())
                .updatedAt(stockAdjust.getUpdatedAt())
                .status(stockAdjust.getStatus())
                .approvedBy(stockAdjust.getApprovedBy() != null ? IdUtil.bytesToUuid(stockAdjust.getApprovedBy().getId()) : null)
                .approvedByName(stockAdjust.getApprovedBy() != null
                        ? stockAdjust.getApprovedBy().getFirstName() + " " + stockAdjust.getApprovedBy().getLastName() : null)
                .addedBy(stockAdjust.getAddedBy() != null ? IdUtil.bytesToUuid(stockAdjust.getAddedBy().getId()) : null)
                .addedByName(stockAdjust.getAddedBy() != null ? stockAdjust.getAddedBy().getFirstName() + " " + stockAdjust.getAddedBy().getLastName() : "Storekeeper")
                .build();
    }
}
