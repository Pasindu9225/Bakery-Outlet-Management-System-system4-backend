package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.PurchaseOrder;
import com.plover.backerymanagmentsystem.manager.model.PurchaseOrderItem;
import com.plover.backerymanagmentsystem.store_keeper.dto.AllPurchaseOrdersResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.AllPurchaseOrdersResponseDto.PurchaseOrderDetailDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.AllPurchaseOrdersResponseDto.PurchaseOrderItemDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.DetailedPurchaseOrderResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.DetailedPurchaseOrderResponseDto.PurchaseOrderWithItemsDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.DetailedPurchaseOrderResponseDto.PurchaseOrderItemDetailDto;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.PurchaseOrderRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;
import com.plover.backerymanagmentsystem.store_keeper.service.PurchaseOrderQueryService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderQueryServiceImpl implements PurchaseOrderQueryService {

    private final PurchaseOrderRepository purchaseOrderRepository;
    private final SupplierRepository supplierRepository;

    @Override
    public AllPurchaseOrdersResponseDto getAllPurchaseOrders() {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAllByOrderByPoIdDesc();

        List<PurchaseOrderDetailDto> poDtos = orders.stream()
                .map(this::mapToDetail)
                .collect(Collectors.toList());

        return AllPurchaseOrdersResponseDto.builder()
                .purchaseOrders(poDtos)
                .totalCount(poDtos.size())
                .build();
    }

    private PurchaseOrderDetailDto mapToDetail(PurchaseOrder po) {
        Optional<Supplier> supplierOpt = supplierRepository.findById(po.getSupplierId());
        String supplierName = supplierOpt.map(Supplier::getName).orElse("Unknown Supplier");

        List<PurchaseOrderItemDto> items = po.getPurchaseOrderItems().stream()
                .map(this::mapItem)
                .collect(Collectors.toList());

        // Get status directly from purchase order
        String poStatus = po.getStatus();

        return PurchaseOrderDetailDto.builder()
                .poId(po.getPoId())
                .totalCost(po.getTotalCost())
                .numberOfItems(po.getNumberOfItems())
                .estimatedDeliveryDate(po.getEstimatedDeliveryDate())
                .supplierId(po.getSupplierId())
                .supplierName(supplierName)
                .poStatus(poStatus)
                .items(items)
                .build();
    }

    private PurchaseOrderItemDto mapItem(PurchaseOrderItem item) {
        return PurchaseOrderItemDto.builder()
                .poiId(item.getPoiId())
                .rawMaterialId(item.getRawMaterialId().longValue())
                .rawMaterialName(item.getRawMaterial() != null ? item.getRawMaterial().getMaterialName() : null)
                .requiredQty(item.getRequiredQty())
                .receivedQty(item.getReceivedQty())
                .actualCost(item.getActualCost())
                .estimatedCost(item.getEstimatedCost())
                .unitOfMeasure(item.getUnitOfMeasure())
                .build();
    }

    @Override
    public DetailedPurchaseOrderResponseDto getAllDetailedPurchaseOrders() {
        List<PurchaseOrder> orders = purchaseOrderRepository.findAllByOrderByPoIdDesc();

        List<PurchaseOrderWithItemsDto> poDtos = orders.stream()
                .map(this::mapToDetailedPurchaseOrder)
                .collect(Collectors.toList());

        return DetailedPurchaseOrderResponseDto.builder()
                .purchaseOrders(poDtos)
                .totalCount(poDtos.size())
                .build();
    }

    private PurchaseOrderWithItemsDto mapToDetailedPurchaseOrder(PurchaseOrder po) {
        Optional<Supplier> supplierOpt = supplierRepository.findById(po.getSupplierId());
        String supplierName = supplierOpt.map(Supplier::getName).orElse("Unknown Supplier");

        List<PurchaseOrderItemDetailDto> items = po.getPurchaseOrderItems().stream()
                .map(this::mapToDetailedItem)
                .collect(Collectors.toList());

        // Get status directly from purchase order
        String status = po.getStatus();

        return PurchaseOrderWithItemsDto.builder()
                .poId(po.getPoId())
                .estimatedDeliveryDate(po.getEstimatedDeliveryDate())
                .numberOfItems(po.getNumberOfItems())
                .supplierId(po.getSupplierId())
                .totalCost(po.getTotalCost())
                .status(status)
                .supplierName(supplierName)
                .items(items)
                .build();
    }

    private PurchaseOrderItemDetailDto mapToDetailedItem(PurchaseOrderItem item) {
        return PurchaseOrderItemDetailDto.builder()
                .poiId(item.getPoiId())
                .actualCost(item.getActualCost())
                .estimatedCost(item.getEstimatedCost())
                .rawMaterialId(item.getRawMaterialId().longValue())
                .receivedQty(item.getReceivedQty())
                .requiredQty(item.getRequiredQty())
                .unitOfMeasure(item.getUnitOfMeasure())
                .rawMaterialName(item.getRawMaterial() != null ? item.getRawMaterial().getMaterialName() : null)
                .build();
    }
}


