package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.core.converter.DataTypeConverter;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.ApproveRawMaterialReturnRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.ApproveRawMaterialReturnResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreateRawMaterialReturnRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.CreateRawMaterialReturnResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.exception.InsufficientStockForReturnException;
import com.plover.backerymanagmentsystem.store_keeper.exception.RawMaterialNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.exception.RawMaterialReturnNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.exception.ReturnItemAlreadyApprovedException;
import com.plover.backerymanagmentsystem.store_keeper.exception.ReturnItemNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.exception.SupplierMaterialValidationException;
import com.plover.backerymanagmentsystem.store_keeper.exception.SupplierNotFoundException;
import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialReturn;
import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialReturnItem;
import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialReturnItem.ReturnStatus;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialReturnItemRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialReturnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialSupplierRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;
import com.plover.backerymanagmentsystem.store_keeper.service.RawMaterialReturnService;
import com.plover.backerymanagmentsystem.store_keeper.dto.GetAllRawMaterialReturnsResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.UpdateRawMaterialReturnRequestDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of RawMaterialReturnService. Handles raw material retur
 * creation and approval with comprehensive validation.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RawMaterialReturnServiceImpl implements RawMaterialReturnService {

    private final RawMaterialReturnRepository rawMaterialReturnRepository;
    private final RawMaterialReturnItemRepository rawMaterialReturnItemRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final SupplierRepository supplierRepository;
    private final RawMaterialSupplierRepository rawMaterialSupplierRepository;

    @Override
    public CreateRawMaterialReturnResponseDto createRawMaterialReturn(CreateRawMaterialReturnRequestDto requestDto) {
        log.info("Creating raw material return for supplier ID: {} with {} items",
                requestDto.getSupplierId(), requestDto.getReturnItems().size());

        // Step 1: Validate supplier exists
        Supplier supplier = validateSupplierExists(requestDto.getSupplierId());

        // Step 2: Validate and get raw materials
        List<RawMaterial> rawMaterials = validateRawMaterialsExist(requestDto);

        // Step 3: Validate supplier can supply all materials
        validateSupplierMaterialRelationships(requestDto.getSupplierId(), rawMaterials);

        // Step 4: Validate stock availability for returns
        validateStockAvailability(requestDto, rawMaterials);

        // Step 5: Calculate costs and create return items data
        List<ReturnItemData> returnItemsData = calculateReturnItemsCosts(requestDto, rawMaterials);

        // Step 6: Calculate total cost
        BigDecimal totalCost = calculateTotalCost(returnItemsData);

        // Step 7: Create and save return entity
        RawMaterialReturn savedReturn = createAndSaveReturn(requestDto, supplier, totalCost, returnItemsData.size());

        // Step 8: Create and save return items
        List<RawMaterialReturnItem> savedItems = createAndSaveReturnItems(savedReturn, returnItemsData);

        // Step 9: Build and return response
        return buildCreateReturnResponse(savedReturn, supplier, savedItems, rawMaterials);
    }

    @Override
    public ApproveRawMaterialReturnResponseDto approveReturnItems(ApproveRawMaterialReturnRequestDto requestDto) {
        log.info("Approving {} return items", requestDto.getReturnItemIds().size());

        // Step 1: Validate return items exist and get details
        List<RawMaterialReturnItem> returnItems = validateReturnItemsExist(requestDto.getReturnItemIds());

        // Step 2: Filter out already approved items
        List<RawMaterialReturnItem> itemsToApprove = filterNotApprovedItems(returnItems);

        if (itemsToApprove.isEmpty()) {
            throw new ReturnItemAlreadyApprovedException("All specified return items are already approved");
        }

        // Step 3: Update return items status to APPROVED
        List<RawMaterialReturnItem> approvedItems = updateReturnItemsStatus(itemsToApprove);

        // Step 4: Update raw material stocks
        List<StockUpdateInfo> stockUpdates = updateRawMaterialStocks(approvedItems);

        // Step 5: Calculate totals
        BigDecimal totalApprovedValue = calculateTotalApprovedValue(approvedItems);

        // Step 6: Build and return response
        return buildApprovalResponse(approvedItems, stockUpdates, totalApprovedValue, returnItems.size());
    }

    @Override
    @Transactional(readOnly = true)
    public CreateRawMaterialReturnResponseDto getRawMaterialReturnById(Long returnId) {
        log.info("Retrieving raw material return with ID: {}", returnId);

        RawMaterialReturn rawMaterialReturn = rawMaterialReturnRepository.findById(returnId)
                .orElseThrow(() -> new RawMaterialReturnNotFoundException(returnId));

        List<RawMaterialReturnItem> returnItems = rawMaterialReturnItemRepository
                .findByReturnIdWithRawMaterialDetails(returnId);

        Supplier supplier = supplierRepository.findById(rawMaterialReturn.getSupplierId())
                .orElseThrow(() -> new SupplierNotFoundException(rawMaterialReturn.getSupplierId()));

        List<RawMaterial> rawMaterials = returnItems.stream()
                .map(RawMaterialReturnItem::getRawMaterial)
                .collect(Collectors.toList());

        return buildCreateReturnResponse(rawMaterialReturn, supplier, returnItems, rawMaterials);
    }

    @Override
    public CreateRawMaterialReturnResponseDto updateRawMaterialReturn(UpdateRawMaterialReturnRequestDto requestDto) {
        log.info("Updating raw material return {} with {} items", requestDto.getReturnId(), requestDto.getItems().size());

        RawMaterialReturn existing = rawMaterialReturnRepository.findById(requestDto.getReturnId())
                .orElseThrow(() -> new RawMaterialReturnNotFoundException(requestDto.getReturnId()));

        // Update header fields
        existing.setReturnDate(requestDto.getReturnDate());
        existing.setSupplierId(requestDto.getSupplierId());
        existing.setNumberOfItems(requestDto.getNumberOfItems());
        existing.setTotalCost(requestDto.getTotalCost());
        rawMaterialReturnRepository.save(existing);

        // Update items - only fields that are editable, not changing IDs
        List<Long> itemIds = requestDto.getItems().stream().map(UpdateRawMaterialReturnRequestDto.UpdateItemDto::getReturnItemId).toList();
        List<RawMaterialReturnItem> items = rawMaterialReturnItemRepository.findAllById(itemIds);
        Map<Long, RawMaterialReturnItem> byId = items.stream().collect(Collectors.toMap(RawMaterialReturnItem::getId, i -> i));

        for (UpdateRawMaterialReturnRequestDto.UpdateItemDto dto : requestDto.getItems()) {
            RawMaterialReturnItem item = byId.get(dto.getReturnItemId());
            if (item == null) {
                throw new ReturnItemNotFoundException("Return item not found: " + dto.getReturnItemId());
            }
            // Prevent changing foreign keys to another return
            item.setRawMaterialId(dto.getRawMaterialId());
            item.setReturnQuantity(dto.getReturnQuantity());
            item.setPrice(dto.getUnitPrice());
            item.setStatus(dto.getStatus());
            item.setReason(dto.getReason());
        }

        rawMaterialReturnItemRepository.saveAll(items);

        // Build response using existing get-by-id logic
        return getRawMaterialReturnById(existing.getReturnId());
    }

    @Override
    @Transactional(readOnly = true)
    public GetAllRawMaterialReturnsResponseDto getAllRawMaterialReturns() {
        List<RawMaterialReturn> returns = rawMaterialReturnRepository.findAll();

        List<GetAllRawMaterialReturnsResponseDto.ReturnSummaryDto> summaries = new ArrayList<>();

        for (RawMaterialReturn r : returns) {
            Supplier supplier = supplierRepository.findById(r.getSupplierId())
                    .orElseThrow(() -> new SupplierNotFoundException(r.getSupplierId()));

            List<RawMaterialReturnItem> items = rawMaterialReturnItemRepository
                    .findByReturnIdWithRawMaterialDetails(r.getReturnId());

            List<GetAllRawMaterialReturnsResponseDto.ReturnItemDto> itemDtos = new ArrayList<>();
            for (RawMaterialReturnItem item : items) {
                BigDecimal totalPrice = item.getPrice().multiply(BigDecimal.valueOf(item.getReturnQuantity()));
                GetAllRawMaterialReturnsResponseDto.ReturnItemDto itemDto =
                        GetAllRawMaterialReturnsResponseDto.ReturnItemDto.builder()
                                .returnItemId(item.getId())
                                .rawMaterialId(item.getRawMaterialId())
                                .rawMaterialName(item.getRawMaterial().getMaterialName())
                                .category(item.getRawMaterial().getCategory())
                                .unitOfMeasure(item.getRawMaterial().getUnitOfMeasure())
                                .returnQuantity(item.getReturnQuantity())
                                .unitPrice(item.getPrice())
                                .totalPrice(totalPrice)
                                .status(item.getStatus())
                                .reason(item.getReason())
                                .createdAt(item.getCreatedAt())
                                .updatedAt(item.getUpdatedAt())
                                .brand(item.getRawMaterial().getBrandName())
                                .batchNo(item.getRawMaterial().getBatchNo())
                                .expiryDate(item.getRawMaterial().getExpireDate())
                                .availableStock(item.getRawMaterial().getCurrentStock())
                                .build();
                itemDtos.add(itemDto);
            }

            GetAllRawMaterialReturnsResponseDto.ReturnSummaryDto summary =
                    GetAllRawMaterialReturnsResponseDto.ReturnSummaryDto.builder()
                            .returnId(r.getReturnId())
                            .returnDate(r.getReturnDate())
                            .numberOfItems(r.getNumberOfItems())
                            .supplierId(r.getSupplierId())
                            .supplierName(supplier.getName())
                            .totalCost(r.getTotalCost())
                            .createdAt(r.getCreatedAt())
                            .updatedAt(r.getUpdatedAt())
                            .items(itemDtos)
                            .build();
            summaries.add(summary);
        }

        return GetAllRawMaterialReturnsResponseDto.builder()
                .totalCount(summaries.size())
                .returns(summaries)
                .build();
    }

    // Private helper methods for validation and business logic
    /**
     * Validates that supplier exists.
     */
    private Supplier validateSupplierExists(Long supplierId) {
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> new SupplierNotFoundException(supplierId));
    }

    /**
     * Validates that all raw materials exist.
     */
    private List<RawMaterial> validateRawMaterialsExist(CreateRawMaterialReturnRequestDto requestDto) {
        List<Long> materialIds = requestDto.getReturnItems().stream()
                .map(CreateRawMaterialReturnRequestDto.ReturnItemRequestDto::getRawMaterialId)
                .collect(Collectors.toList());

        List<RawMaterial> materials = rawMaterialRepository.findAllById(materialIds);

        if (materials.size() != materialIds.size()) {
            List<Long> foundIds = materials.stream().map(RawMaterial::getId).collect(Collectors.toList());
            List<Long> missingIds = materialIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new RawMaterialNotFoundException("Raw materials not found with IDs: " + missingIds);
        }

        return materials;
    }

    /**
     * Validates supplier can supply all specified materials.
     */
    private void validateSupplierMaterialRelationships(Long supplierId, List<RawMaterial> rawMaterials) {
        List<String> invalidMaterials = new ArrayList<>();

        for (RawMaterial material : rawMaterials) {
            boolean canSupply = false;
            List<RawMaterial> allMaterialsWithCode = rawMaterialRepository.findAllByMaterialCode(material.getMaterialCode());
            for (RawMaterial mat : allMaterialsWithCode) {
                if (rawMaterialSupplierRepository.existsByRawMaterialIdAndSupplierId(mat.getId(), supplierId)) {
                    canSupply = true;
                    break;
                }
            }
            if (!canSupply) {
                invalidMaterials.add(material.getMaterialName() + " (ID: " + material.getId() + ")");
            }
        }

        if (!invalidMaterials.isEmpty()) {
            throw new SupplierMaterialValidationException(supplierId, invalidMaterials);
        }
    }

    /**
     * Validates stock availability for return quantities.
     */
    private void validateStockAvailability(CreateRawMaterialReturnRequestDto requestDto,
            List<RawMaterial> rawMaterials) {
        Map<Long, RawMaterial> materialMap = rawMaterials.stream()
                .collect(Collectors.toMap(RawMaterial::getId, material -> material));

        List<String> insufficientStockMaterials = new ArrayList<>();

        for (CreateRawMaterialReturnRequestDto.ReturnItemRequestDto item : requestDto.getReturnItems()) {
            RawMaterial material = materialMap.get(item.getRawMaterialId());
            if (material != null) {
                Double availableStock = material.getCurrentStock();
                Double returnQuantity = item.getReturnQuantity();

                if (returnQuantity > availableStock) {
                    insufficientStockMaterials.add(String.format("%s: requested %s, available %s",
                            material.getMaterialName(), returnQuantity, availableStock));
                }
            }
        }

        if (!insufficientStockMaterials.isEmpty()) {
            throw new InsufficientStockForReturnException(insufficientStockMaterials);
        }
    }

    /**
     * Calculates costs for return items and builds data objects.
     */
    private List<ReturnItemData> calculateReturnItemsCosts(CreateRawMaterialReturnRequestDto requestDto,
            List<RawMaterial> rawMaterials) {
        Map<Long, RawMaterial> materialMap = rawMaterials.stream()
                .collect(Collectors.toMap(RawMaterial::getId, material -> material));

        List<ReturnItemData> returnItemsData = new ArrayList<>();

        for (CreateRawMaterialReturnRequestDto.ReturnItemRequestDto item : requestDto.getReturnItems()) {
            RawMaterial material = materialMap.get(item.getRawMaterialId());

            // Use unit cost from raw material (could be enhanced to use original purchase price)
            BigDecimal unitPrice = DataTypeConverter.doubleToBigDecimal(material.getUnitCost());
            BigDecimal totalPrice = unitPrice.multiply(BigDecimal.valueOf(item.getReturnQuantity()));

            ReturnItemData itemData = ReturnItemData.builder()
                    .rawMaterialId(item.getRawMaterialId())
                    .returnQuantity(item.getReturnQuantity())
                    .reason(item.getReason())
                    .unitPrice(unitPrice)
                    .totalPrice(totalPrice)
                    .rawMaterial(material)
                    .build();

            returnItemsData.add(itemData);
        }

        return returnItemsData;
    }

    /**
     * Calculates total cost for all return items.
     */
    private BigDecimal calculateTotalCost(List<ReturnItemData> returnItemsData) {
        return returnItemsData.stream()
                .map(ReturnItemData::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Creates and saves the main return entity.
     */
    private RawMaterialReturn createAndSaveReturn(CreateRawMaterialReturnRequestDto requestDto,
            Supplier supplier,
            BigDecimal totalCost,
            int numberOfItems) {
        RawMaterialReturn rawMaterialReturn = RawMaterialReturn.builder()
                .returnDate(requestDto.getReturnDate())
                .numberOfItems(numberOfItems)
                .supplierId(supplier.getSupplierId())
                .totalCost(totalCost)
                .build();

        return rawMaterialReturnRepository.save(rawMaterialReturn);
    }

    /**
     * Creates and saves return items.
     */
    private List<RawMaterialReturnItem> createAndSaveReturnItems(RawMaterialReturn savedReturn,
            List<ReturnItemData> returnItemsData) {
        List<RawMaterialReturnItem> returnItems = new ArrayList<>();

        for (ReturnItemData itemData : returnItemsData) {
            RawMaterialReturnItem item = RawMaterialReturnItem.builder()
                    .returnId(savedReturn.getReturnId())
                    .rawMaterialId(itemData.getRawMaterialId())
                    .returnQuantity(itemData.getReturnQuantity())
                    .price(itemData.getUnitPrice())
                    .status(ReturnStatus.NOT_APPROVED)
                    .reason(itemData.getReason())
                    .build();

            returnItems.add(item);
        }

        return rawMaterialReturnItemRepository.saveAll(returnItems);
    }

    /**
     * Validates return items exist and retrieves them with details.
     */
    private List<RawMaterialReturnItem> validateReturnItemsExist(List<Long> itemIds) {
        List<RawMaterialReturnItem> items = rawMaterialReturnItemRepository
                .findByIdInWithRawMaterialDetails(itemIds);

        if (items.size() != itemIds.size()) {
            List<Long> foundIds = items.stream().map(RawMaterialReturnItem::getId).collect(Collectors.toList());
            List<Long> missingIds = itemIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .collect(Collectors.toList());
            throw new ReturnItemNotFoundException("Return items not found with IDs: " + missingIds);
        }

        return items;
    }

    /**
     * Filters out already approved items.
     */
    private List<RawMaterialReturnItem> filterNotApprovedItems(List<RawMaterialReturnItem> returnItems) {
        return returnItems.stream()
                .filter(item -> item.getStatus() == ReturnStatus.NOT_APPROVED)
                .collect(Collectors.toList());
    }

    /**
     * Updates return items status to APPROVED.
     */
    private List<RawMaterialReturnItem> updateReturnItemsStatus(List<RawMaterialReturnItem> itemsToApprove) {
        List<Long> itemIds = itemsToApprove.stream()
                .map(RawMaterialReturnItem::getId)
                .collect(Collectors.toList());

        rawMaterialReturnItemRepository.updateStatusByIdIn(itemIds, ReturnStatus.APPROVED);

        // Reload items to get updated status
        return rawMaterialReturnItemRepository.findByIdInWithRawMaterialDetails(itemIds);
    }

    /**
     * Updates raw material stocks by reducing the returned quantities.
     */
    private List<StockUpdateInfo> updateRawMaterialStocks(List<RawMaterialReturnItem> approvedItems) {
        List<StockUpdateInfo> stockUpdates = new ArrayList<>();

        for (RawMaterialReturnItem item : approvedItems) {
            RawMaterial material = item.getRawMaterial();
            Double previousStock = material.getCurrentStock();
            Double returnQuantity = item.getReturnQuantity();
            Double newStock = previousStock - returnQuantity;

            // Update the stock
            material.setCurrentStock(newStock);
            rawMaterialRepository.save(material);

            StockUpdateInfo updateInfo = StockUpdateInfo.builder()
                    .rawMaterialId(material.getId())
                    .rawMaterialName(material.getMaterialName())
                    .previousStock(previousStock)
                    .returnedQuantity(returnQuantity)
                    .newStock(newStock)
                    .build();

            stockUpdates.add(updateInfo);

            log.info("Updated stock for material '{}': {} -> {} (returned: {})",
                    material.getMaterialName(), previousStock, newStock, returnQuantity);
        }

        return stockUpdates;
    }

    /**
     * Calculates total approved value.
     */
    private BigDecimal calculateTotalApprovedValue(List<RawMaterialReturnItem> approvedItems) {
        return approvedItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getReturnQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Builds create return response DTO.
     */
    private CreateRawMaterialReturnResponseDto buildCreateReturnResponse(RawMaterialReturn savedReturn,
            Supplier supplier,
            List<RawMaterialReturnItem> savedItems,
            List<RawMaterial> rawMaterials) {
        Map<Long, RawMaterial> materialMap = rawMaterials.stream()
                .collect(Collectors.toMap(RawMaterial::getId, material -> material));

        List<CreateRawMaterialReturnResponseDto.ReturnItemResponseDto> itemResponses = new ArrayList<>();

        for (RawMaterialReturnItem item : savedItems) {
            RawMaterial material = materialMap.get(item.getRawMaterialId());
            BigDecimal totalPrice = item.getPrice().multiply(BigDecimal.valueOf(item.getReturnQuantity()));

            CreateRawMaterialReturnResponseDto.ReturnItemResponseDto itemResponse
                    = CreateRawMaterialReturnResponseDto.ReturnItemResponseDto.builder()
                            .returnItemId(item.getId())
                            .rawMaterialId(item.getRawMaterialId())
                            .rawMaterialName(material.getMaterialName())
                            .category(material.getCategory())
                            .unitOfMeasure(material.getUnitOfMeasure())
                            .returnQuantity(item.getReturnQuantity())
                            .unitPrice(item.getPrice())
                            .totalPrice(totalPrice)
                            .status(item.getStatus())
                            .reason(item.getReason())
                            .brand(material.getBrandName())
                            .batchNo(material.getBatchNo())
                            .expiryDate(material.getExpireDate())
                            .availableStock(material.getCurrentStock())
                            .build();

            itemResponses.add(itemResponse);
        }

        return CreateRawMaterialReturnResponseDto.builder()
                .returnId(savedReturn.getReturnId())
                .returnDate(savedReturn.getReturnDate())
                .numberOfItems(savedReturn.getNumberOfItems())
                .supplierId(savedReturn.getSupplierId())
                .supplierName(supplier.getName())
                .totalCost(savedReturn.getTotalCost())
                .returnItems(itemResponses)
                .createdAt(savedReturn.getCreatedAt())
                .build();
    }

    /**
     * Builds approval response DTO.
     */
    private ApproveRawMaterialReturnResponseDto buildApprovalResponse(List<RawMaterialReturnItem> approvedItems,
            List<StockUpdateInfo> stockUpdates,
            BigDecimal totalApprovedValue,
            int totalItemsCount) {
        List<ApproveRawMaterialReturnResponseDto.ApprovedItemDto> approvedItemDtos = new ArrayList<>();

        for (RawMaterialReturnItem item : approvedItems) {
            BigDecimal totalPrice = item.getPrice().multiply(BigDecimal.valueOf(item.getReturnQuantity()));

            ApproveRawMaterialReturnResponseDto.ApprovedItemDto approvedItemDto
                    = ApproveRawMaterialReturnResponseDto.ApprovedItemDto.builder()
                            .returnItemId(item.getId())
                            .rawMaterialId(item.getRawMaterialId())
                            .rawMaterialName(item.getRawMaterial().getMaterialName())
                            .returnQuantity(item.getReturnQuantity())
                            .unitPrice(item.getPrice())
                            .totalPrice(totalPrice)
                            .build();

            approvedItemDtos.add(approvedItemDto);
        }

        List<ApproveRawMaterialReturnResponseDto.StockUpdateDto> stockUpdateDtos = stockUpdates.stream()
                .map(update -> ApproveRawMaterialReturnResponseDto.StockUpdateDto.builder()
                .rawMaterialId(update.getRawMaterialId())
                .rawMaterialName(update.getRawMaterialName())
                .previousStock(update.getPreviousStock())
                .returnedQuantity(update.getReturnedQuantity())
                .newStock(update.getNewStock())
                .build())
                .collect(Collectors.toList());

        return ApproveRawMaterialReturnResponseDto.builder()
                .approvedItemsCount(approvedItems.size())
                .totalItemsCount(totalItemsCount)
                .totalApprovedValue(totalApprovedValue)
                .approvedItems(approvedItemDtos)
                .stockUpdates(stockUpdateDtos)
                .processedAt(LocalDateTime.now())
                .build();
    }

    // Internal data classes
    /**
     * Internal class to hold return item data during processing.
     */
    @lombok.Data
    @lombok.Builder
    private static class ReturnItemData {

        private Long rawMaterialId;
        private Double returnQuantity;
        private com.plover.backerymanagmentsystem.store_keeper.model.ReturnReason reason;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private RawMaterial rawMaterial;
    }

    /**
     * Internal class to hold stock update information.
     */
    @lombok.Data
    @lombok.Builder
    private static class StockUpdateInfo {

        private Long rawMaterialId;
        private String rawMaterialName;
        private Double previousStock;
        private Double returnedQuantity;
        private Double newStock;
    }
}
