package com.plover.backerymanagmentsystem.worker.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.manager.model.ProductionOrder;
import com.plover.backerymanagmentsystem.manager.model.ProductionOrderItem;
import com.plover.backerymanagmentsystem.store_keeper.repository.ProductionOrderRepository;
import com.plover.backerymanagmentsystem.worker.dto.WorkerKotDto;
import com.plover.backerymanagmentsystem.worker.dto.WorkerKotItemDto;
import com.plover.backerymanagmentsystem.worker.service.WorkerKotService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkerKotServiceImpl implements WorkerKotService {

    private final ProductionOrderRepository productionOrderRepository;

    @Override
    public List<WorkerKotDto> listMyKots() {
        AuthModel user = currentUser();
        Long pcId = resolvePcId(user);
        log.info("Listing KOTs for worker {} at production center {}", user.getUsername(), pcId);
        List<ProductionOrder> orders = productionOrderRepository.findKotsByProductionCenterId(pcId);
        return orders.stream().map(po -> toDto(po, pcId)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WorkerKotDto updateKotStatus(Long kotId, String status) {
        AuthModel user = currentUser();
        Long pcId = resolvePcId(user);

        ProductionOrder.ProductionOrderStatus parsed;
        try {
            parsed = ProductionOrder.ProductionOrderStatus.valueOf(status);
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Invalid status: " + status);
        }

        if (parsed != ProductionOrder.ProductionOrderStatus.IN_PROGRESS
                && parsed != ProductionOrder.ProductionOrderStatus.COMPLETED) {
            throw new RuntimeException("Workers can only set status IN_PROGRESS or COMPLETED");
        }

        ProductionOrder order = productionOrderRepository.findById(kotId)
                .orElseThrow(() -> new RuntimeException("KOT not found with id: " + kotId));

        boolean ownsItem = order.getProductionOrderItems() != null &&
                order.getProductionOrderItems().stream()
                        .anyMatch(it -> pcId.equals(it.getProductionCenterId()));
        if (!ownsItem) {
            throw new RuntimeException("You are not assigned to any item on this KOT");
        }

        order.setStatus(parsed);
        ProductionOrder saved = productionOrderRepository.save(order);
        log.info("Worker {} updated KOT {} status to {}", user.getUsername(), kotId, parsed);
        return toDto(saved, pcId);
    }

    private Long resolvePcId(AuthModel user) {
        Long pcId = user.getMpcId() != null ? user.getMpcId() : user.getProductionCenterId();
        if (pcId == null) {
            throw new RuntimeException("Worker is not assigned to any production center");
        }
        return pcId;
    }

    private AuthModel currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthModel)) {
            throw new RuntimeException("Not authenticated");
        }
        return (AuthModel) auth.getPrincipal();
    }

    private WorkerKotDto toDto(ProductionOrder po, Long pcId) {
        List<WorkerKotItemDto> items = po.getProductionOrderItems() == null
                ? List.of()
                : po.getProductionOrderItems().stream()
                        .filter(it -> pcId.equals(it.getProductionCenterId()))
                        .map(this::itemToDto)
                        .collect(Collectors.toList());
        int total = items.stream().mapToInt(it -> it.getPlannedQuantity() != null ? it.getPlannedQuantity() : 0).sum();
        return WorkerKotDto.builder()
                .id(po.getId())
                .orderNumber(po.getOrderNumber())
                .status(po.getStatus() != null ? po.getStatus().name() : null)
                .orderDate(po.getOrderDate())
                .items(items)
                .totalQuantity(total)
                .build();
    }

    private WorkerKotItemDto itemToDto(ProductionOrderItem it) {
        return WorkerKotItemDto.builder()
                .id(it.getId())
                .productName(it.getProductName())
                .productId(it.getProductId())
                .plannedQuantity(it.getPlannedQuantity())
                .completedQuantity(it.getCompletedQuantity())
                .productionCenterId(it.getProductionCenterId())
                .build();
    }
}
