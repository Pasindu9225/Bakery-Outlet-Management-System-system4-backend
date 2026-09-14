package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
import com.plover.backerymanagmentsystem.manager.model.PurchaseOrder;
import com.plover.backerymanagmentsystem.manager.model.PurchaseOrderItem;
import com.plover.backerymanagmentsystem.manager.repository.ManagerPurchaseOrderRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.*;
import com.plover.backerymanagmentsystem.store_keeper.model.*;
import com.plover.backerymanagmentsystem.store_keeper.repository.*;
import com.plover.backerymanagmentsystem.store_keeper.service.IouRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class IouRequestServiceImpl implements IouRequestService {

    private final IouRequestRepository iouRequestRepository;
    private final IouRequestItemRepository iouRequestItemRepository;
    private final AuthRepository authRepository;
    private final ManagerPurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final GrnRepository grnRepository;
    private final GrnItemRepository grnItemRepository;
    private final SupplierRepository supplierRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository rawMaterialRepository;

    private final com.plover.backerymanagmentsystem.manager.repository.BrandRepository brandRepository;

    @Override
    public IouRequestResponseDto createIouRequest(CreateIouRequestDto requestDto) {
        AuthModel user = authRepository.findById(IdUtil.uuidToBytes(requestDto.getUserId()))
                .orElseThrow(() -> new RuntimeException("User not found"));

        double totalEstimated = requestDto.getItems().stream()
                .mapToDouble(item -> (item.getEstimatedQuantity() != null ? item.getEstimatedQuantity() : 0) * 
                                     (item.getEstimatedPrice() != null ? item.getEstimatedPrice() : 0))
                .sum();

        IouRequest request = IouRequest.builder()
                .requestDate(requestDto.getRequestDate() != null ? requestDto.getRequestDate() : LocalDate.now())
                .justification(requestDto.getJustification())
                .receiverName(requestDto.getReceiverName())
                .status(IouStatus.PENDING)
                .totalEstimatedAmount(totalEstimated)
                .issuedAmount(totalEstimated) // Initial default issued amount
                .addedBy(user)
                .build();

        IouRequest savedRequest = iouRequestRepository.save(request);

        List<IouRequestItem> items = requestDto.getItems().stream().map(dto -> 
                IouRequestItem.builder()
                        .iouRequest(savedRequest)
                        .supplierName(dto.getSupplierName())
                        .supplierContact(dto.getSupplierContact())
                        .itemType(dto.getItemType())
                        .rawMaterialId(dto.getRawMaterialId())
                        .itemName(dto.getItemName())
                        .unitOfMeasure(dto.getUnitOfMeasure() != null ? dto.getUnitOfMeasure() : "Unit")
                        .estimatedQuantity(dto.getEstimatedQuantity())
                        .estimatedPrice(dto.getEstimatedPrice())
                        .build()
        ).collect(Collectors.toList());

        iouRequestItemRepository.saveAll(items);
        savedRequest.setItems(items);

        return mapToDto(savedRequest);
    }

    @Override
    public IouRequestResponseDto approveIouRequest(Long iouRequestId, UUID managerId) {
        return approveIouRequest(iouRequestId, managerId, null);
    }

    @Override
    public IouRequestResponseDto approveIouRequest(Long iouRequestId, UUID managerId, ApproveIouRequestDto approveDto) {
        IouRequest request = getRequest(iouRequestId);
        AuthModel manager = authRepository.findById(IdUtil.uuidToBytes(managerId))
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (request.getStatus() != IouStatus.PENDING) {
            throw new RuntimeException("Can only approve PENDING IOUs");
        }

        Double issuedAmt = (approveDto != null && approveDto.getIssuedAmount() != null)
                ? approveDto.getIssuedAmount()
                : request.getTotalEstimatedAmount();

        request.setStatus(IouStatus.APPROVED);
        request.setApprovedBy(manager);
        request.setIssuedAmount(issuedAmt);

        return mapToDto(iouRequestRepository.save(request));
    }

    @Override
    public IouRequestResponseDto settleIouRequest(Long iouRequestId, UUID storekeeperId, IouSettleRequestDto settleDto) {
        IouRequest request = getRequest(iouRequestId);
        if (request.getStatus() != IouStatus.APPROVED) {
            throw new RuntimeException("Can only settle APPROVED IOUs");
        }

        request.setInvoiceNumber(settleDto.getInvoiceNumber());
        
        double totalActual = 0.0;

        for (IouSettleItemDto itemDto : settleDto.getActualItems()) {
            IouRequestItem item = iouRequestItemRepository.findById(itemDto.getIouRequestItemId())
                    .orElseThrow(() -> new RuntimeException("Item not found"));
            
            item.setActualQuantity(itemDto.getActualQuantity());
            item.setActualPrice(itemDto.getActualPrice());
            item.setActualItemName(itemDto.getActualItemName());
            item.setActualSupplierName(itemDto.getActualSupplierName());
            if (itemDto.getRawMaterialId() != null) {
                item.setRawMaterialId(itemDto.getRawMaterialId());
            }
            if (itemDto.getUnitOfMeasure() != null) {
                item.setUnitOfMeasure(itemDto.getUnitOfMeasure());
            }
            iouRequestItemRepository.save(item);

            double actualCost = (itemDto.getActualQuantity() != null ? itemDto.getActualQuantity() : 0) * 
                                (itemDto.getActualPrice() != null ? itemDto.getActualPrice() : 0);
            totalActual += actualCost;

            Long suppId = findOrCreateSupplier(itemDto.getActualSupplierName());
            
            PurchaseOrder po = PurchaseOrder.builder()
                .totalCost(BigDecimal.valueOf(actualCost))
                .numberOfItems(1)
                .estimatedDeliveryDate(LocalDate.now())
                .supplierId(suppId)
                .status("APPROVED")
                .build();
            po = purchaseOrderRepository.save(po);

            Integer targetRawMaterialId = (item.getRawMaterialId() != null) 
                    ? item.getRawMaterialId().intValue() 
                    : getFallbackRawMaterialId();

            PurchaseOrderItem poItem = PurchaseOrderItem.builder()
                .purchaseOrder(po)
                .rawMaterialId(targetRawMaterialId)
                .requiredQty(itemDto.getActualQuantity() != null ? itemDto.getActualQuantity().intValue() : 0)
                .receivedQty(itemDto.getActualQuantity() != null ? itemDto.getActualQuantity().intValue() : 0)
                .estimatedCost(BigDecimal.valueOf(actualCost))
                .actualCost(BigDecimal.valueOf(actualCost))
                .unitOfMeasure(item.getUnitOfMeasure() != null ? item.getUnitOfMeasure() : "Unit")
                .build();
            purchaseOrderItemRepository.save(poItem);

            Grn grn = Grn.builder()
                .poId(po.getPoId())
                .supplierId(suppId)
                .receivedDate(LocalDateTime.now())
                .total(po.getTotalCost())
                .grnStatus(GrnStatus.RECEIVED)
                .isReceived(true)
                .invoiceNumber(settleDto.getInvoiceNumber())
                .build();
            grnRepository.save(grn);

            GrnItem grnItem = GrnItem.builder()
                .grn(grn)
                .grnId(grn.getGrnId())
                .rawMaterialId(targetRawMaterialId.longValue())
                .receivedQuantity(BigDecimal.valueOf(itemDto.getActualQuantity() != null ? itemDto.getActualQuantity() : 0))
                .uom(item.getUnitOfMeasure() != null ? item.getUnitOfMeasure() : "Unit")
                .pricePerUnit(BigDecimal.valueOf(itemDto.getActualPrice() != null ? itemDto.getActualPrice() : 0))
                .invoiceQuantity(BigDecimal.valueOf(itemDto.getActualQuantity() != null ? itemDto.getActualQuantity() : 0))
                .invoicePrice(BigDecimal.valueOf(itemDto.getActualPrice() != null ? itemDto.getActualPrice() : 0))
                .build();
            grnItemRepository.save(grnItem);

            // Update Raw Material stock immediately upon IOU Settlement
            if (item.getItemType() == IouItemType.RAW_MATERIAL) {
                Long matId = item.getRawMaterialId();
                Double qtyToAdd = itemDto.getActualQuantity() != null ? itemDto.getActualQuantity() : item.getEstimatedQuantity();
                Double unitCost = itemDto.getActualPrice() != null ? itemDto.getActualPrice() : item.getEstimatedPrice();
                String targetName = (itemDto.getActualItemName() != null && !itemDto.getActualItemName().trim().isEmpty())
                        ? itemDto.getActualItemName().trim()
                        : (item.getItemName() != null ? item.getItemName().trim() : "Unassigned Raw Material");
                String supplierName = itemDto.getActualSupplierName() != null ? itemDto.getActualSupplierName() : item.getSupplierName();

                com.plover.backerymanagmentsystem.manager.model.RawMaterial material = null;
                if (matId != null) {
                    material = rawMaterialRepository.findById(matId).orElse(null);
                }
                if (material == null) {
                    material = rawMaterialRepository.findAll().stream()
                            .filter(rm -> rm.getMaterialName().equalsIgnoreCase(targetName))
                            .findFirst()
                            .orElse(null);
                }

                if (material != null) {
                    Double currentStock = material.getCurrentStock() != null ? material.getCurrentStock() : 0.0;
                    material.setCurrentStock(currentStock + (qtyToAdd != null ? qtyToAdd : 0.0));
                    if (unitCost != null && unitCost > 0) {
                        material.setUnitCost(unitCost);
                    }
                    rawMaterialRepository.save(material);
                    log.info("Increased stock on IOU settlement for material '{}' (ID: {}) by {} -> new stock {}",
                            material.getMaterialName(), material.getId(), qtyToAdd, material.getCurrentStock());
                } else {
                    com.plover.backerymanagmentsystem.manager.model.Brand defaultBrand = brandRepository.findAll().stream().findFirst().orElse(null);
                    com.plover.backerymanagmentsystem.manager.model.RawMaterial newMaterial = com.plover.backerymanagmentsystem.manager.model.RawMaterial.builder()
                            .materialName(targetName)
                            .materialCode("RM-IOU-" + System.currentTimeMillis() % 10000)
                            .unitOfMeasure(item.getUnitOfMeasure() != null ? item.getUnitOfMeasure() : "Unit")
                            .unitCost(unitCost != null ? unitCost : 0.0)
                            .currentStock(qtyToAdd != null ? qtyToAdd : 0.0)
                            .initialQuantity(qtyToAdd != null ? qtyToAdd : 0.0)
                            .minimumStockLevel(5.0)
                            .brand(defaultBrand)
                            .isActive(true)
                            .supplierName(supplierName)
                            .build();
                    newMaterial = rawMaterialRepository.save(newMaterial);
                    item.setRawMaterialId(newMaterial.getId());
                    iouRequestItemRepository.save(item);
                    log.info("Created new raw material from IOU settlement: '{}' (ID: {}) with initial stock {}",
                            targetName, newMaterial.getId(), qtyToAdd);
                }
            }
        }

        request.setTotalActualAmount(totalActual);
        Double baseAmount = request.getIssuedAmount() != null ? request.getIssuedAmount() : request.getTotalEstimatedAmount();
        request.setDifferenceAmount(baseAmount - totalActual);
        request.setStatus(IouStatus.SETTLEMENT_PENDING);

        return mapToDto(iouRequestRepository.save(request));
    }

    @Override
    public IouRequestResponseDto approveFinalSettlement(Long iouRequestId, UUID managerId) {
        IouRequest request = getRequest(iouRequestId);
        AuthModel manager = authRepository.findById(IdUtil.uuidToBytes(managerId))
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (request.getStatus() != IouStatus.SETTLEMENT_PENDING) {
            throw new RuntimeException("Can only final approve SETTLEMENT_PENDING IOUs");
        }

        request.setStatus(IouStatus.CLOSED);
        request.setFinalApprovedBy(manager);

        return mapToDto(iouRequestRepository.save(request));
    }

    @Override
    public List<IouRequestResponseDto> getAllIouRequests() {
        return iouRequestRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public List<IouRequestResponseDto> getIouRequestsByStatus(IouStatus status) {
        return iouRequestRepository.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(this::mapToDto).collect(Collectors.toList());
    }

    @Override
    public IouRequestResponseDto getIouRequestById(Long id) {
        return mapToDto(getRequest(id));
    }

    private IouRequest getRequest(Long id) {
        return iouRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("IOU Request not found"));
    }

    private Long findOrCreateSupplier(String supplierName) {
        if (supplierName == null || supplierName.trim().isEmpty()) {
            supplierName = "Market Vendor";
        }
        String finalName = supplierName;
        return supplierRepository.findAll().stream()
                .filter(s -> s.getName().equalsIgnoreCase(finalName))
                .findFirst()
                .map(Supplier::getSupplierId)
                .orElseGet(() -> {
                    Supplier s = Supplier.builder()
                            .name(finalName)
                            .address("Market")
                            .contactNumber("N/A")
                            .build();
                    return supplierRepository.save(s).getSupplierId();
                });
    }

    private Integer getFallbackRawMaterialId() {
        return rawMaterialRepository.findAll().stream()
                .findFirst()
                .map(rm -> rm.getId().intValue())
                .orElseThrow(() -> new RuntimeException("No RawMaterials exist to map generic PO items. Please create at least one raw material in the system."));
    }

    private IouRequestResponseDto mapToDto(IouRequest entity) {
        return IouRequestResponseDto.builder()
                .id(entity.getId())
                .requestDate(entity.getRequestDate())
                .justification(entity.getJustification())
                .receiverName(entity.getReceiverName())
                .status(entity.getStatus())
                .totalEstimatedAmount(entity.getTotalEstimatedAmount())
                .issuedAmount(entity.getIssuedAmount() != null ? entity.getIssuedAmount() : entity.getTotalEstimatedAmount())
                .totalActualAmount(entity.getTotalActualAmount())
                .differenceAmount(entity.getDifferenceAmount())
                .invoiceNumber(entity.getInvoiceNumber())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .addedById(entity.getAddedBy() != null ? IdUtil.bytesToUuid(entity.getAddedBy().getId()) : null)
                .addedByName(entity.getAddedBy() != null ? entity.getAddedBy().getFirstName() + " " + entity.getAddedBy().getLastName() : null)
                .approvedById(entity.getApprovedBy() != null ? IdUtil.bytesToUuid(entity.getApprovedBy().getId()) : null)
                .approvedByName(entity.getApprovedBy() != null ? entity.getApprovedBy().getFirstName() + " " + entity.getApprovedBy().getLastName() : null)
                .finalApprovedById(entity.getFinalApprovedBy() != null ? IdUtil.bytesToUuid(entity.getFinalApprovedBy().getId()) : null)
                .finalApprovedByName(entity.getFinalApprovedBy() != null ? entity.getFinalApprovedBy().getFirstName() + " " + entity.getFinalApprovedBy().getLastName() : null)
                .items(entity.getItems() != null ? entity.getItems().stream().map(this::mapItemToDto).collect(Collectors.toList()) : null)
                .build();
    }

    private IouItemResponseDto mapItemToDto(IouRequestItem item) {
        return IouItemResponseDto.builder()
                .id(item.getId())
                .iouRequestId(item.getIouRequest().getId())
                .supplierName(item.getSupplierName())
                .supplierContact(item.getSupplierContact())
                .itemType(item.getItemType())
                .rawMaterialId(item.getRawMaterialId())
                .itemName(item.getItemName())
                .rawMaterialName(item.getItemName())
                .unitOfMeasure(item.getUnitOfMeasure())
                .estimatedQuantity(item.getEstimatedQuantity())
                .estimatedPrice(item.getEstimatedPrice())
                .actualQuantity(item.getActualQuantity())
                .actualPrice(item.getActualPrice())
                .actualItemName(item.getActualItemName())
                .actualSupplierName(item.getActualSupplierName())
                .build();
    }
}
