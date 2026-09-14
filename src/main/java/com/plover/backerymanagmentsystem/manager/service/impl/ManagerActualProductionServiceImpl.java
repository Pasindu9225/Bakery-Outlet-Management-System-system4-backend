package com.plover.backerymanagmentsystem.manager.service.impl;

import com.plover.backerymanagmentsystem.manager.dto.ActualProductionDto;
import com.plover.backerymanagmentsystem.manager.dto.ActualProductionHistoryDto;
import com.plover.backerymanagmentsystem.manager.dto.ActualProductionDistributeRequestDto;
import com.plover.backerymanagmentsystem.manager.model.ActualProduction;
import com.plover.backerymanagmentsystem.manager.model.ActualProductionHistory;
import com.plover.backerymanagmentsystem.manager.model.ActualProductionHistoryAction;
import com.plover.backerymanagmentsystem.manager.model.DistributionPlan;
import com.plover.backerymanagmentsystem.manager.model.DistributionPlanItem;
import com.plover.backerymanagmentsystem.manager.model.DistributionPlanStatus;
import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.repository.ActualProductionRepository;
import com.plover.backerymanagmentsystem.manager.repository.ActualProductionHistoryRepository;
import com.plover.backerymanagmentsystem.manager.repository.DistributionPlanItemRepository;
import com.plover.backerymanagmentsystem.manager.repository.DistributionPlanRepository;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.manager.service.ManagerActualProductionService;
import com.plover.backerymanagmentsystem.pos.dto.CreateGtnRequestDto;
import com.plover.backerymanagmentsystem.pos.model.EntryStatus;
import com.plover.backerymanagmentsystem.pos.model.GtnSource;
import com.plover.backerymanagmentsystem.pos.model.GtnStatus;
import com.plover.backerymanagmentsystem.pos.model.ProductUnit;
import com.plover.backerymanagmentsystem.pos.dto.DayProductionItemResponseDto;
import com.plover.backerymanagmentsystem.pos.service.DayProductionService;
import com.plover.backerymanagmentsystem.pos.service.GtnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerActualProductionServiceImpl implements ManagerActualProductionService {

    private final ActualProductionRepository actualProductionRepository;
    private final DistributionPlanRepository distributionPlanRepository;
    private final DistributionPlanItemRepository distributionPlanItemRepository;
    private final OutletRepository outletRepository;
    private final ProductRepository productRepository;
    private final GtnService gtnService;
    private final ActualProductionHistoryRepository actualProductionHistoryRepository;
    private final DayProductionService dayProductionService;
    private final com.plover.backerymanagmentsystem.pos.repository.OutletTransferRequestRepository outletTransferRequestRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.ProductionPlanRepository productionPlanRepository;

    @Override
    @Transactional
    public List<ActualProductionDto> getAllActualProductions() {
        syncDistributedPlansToActualProduction();
        return actualProductionRepository.findAll().stream()
                .map(ap -> ActualProductionDto.builder()
                        .id(ap.getId())
                        .productId(ap.getProductId())
                        .productName(ap.getProductName())
                        .availableQuantity(ap.getAvailableQuantity())
                        .lastUpdated(ap.getLastUpdated())
                        .build())
                .collect(Collectors.toList());
    }

    private void syncDistributedPlansToActualProduction() {
        try {
            List<com.plover.backerymanagmentsystem.manager.model.ProductionPlan> distributedPlans = 
                    productionPlanRepository.findByStatus(com.plover.backerymanagmentsystem.manager.model.ProductionPlan.ProductionPlanStatus.DISTRIBUTED);

            for (var plan : distributedPlans) {
                if (plan.getProductionPlanItems() == null) continue;
                for (var item : plan.getProductionPlanItems()) {
                    if (item.getProductId() == null) continue;
                    boolean isTop = Boolean.TRUE.equals(item.getIsTopLevel()) || item.getParentPlanItemId() == null;
                    if (!isTop) continue;

                    boolean historyExists = actualProductionHistoryRepository
                            .existsByProductIdAndReferenceNameAndActionType(
                                    item.getProductId(), 
                                    plan.getPlanName(), 
                                    ActualProductionHistoryAction.PRODUCTION_IN);

                    if (!historyExists) {
                        int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                        if (qty <= 0) continue;

                        ActualProduction actualProd = actualProductionRepository.findByProductId(item.getProductId())
                                .orElseGet(() -> ActualProduction.builder()
                                        .productId(item.getProductId())
                                        .productName(item.getProductName())
                                        .availableQuantity(0)
                                        .build());

                        actualProd.setAvailableQuantity(actualProd.getAvailableQuantity() + qty);
                        actualProd.setLastUpdated(java.time.OffsetDateTime.now());
                        actualProductionRepository.save(actualProd);

                        ActualProductionHistory history = ActualProductionHistory.builder()
                                .productId(item.getProductId())
                                .productName(item.getProductName())
                                .quantity(qty)
                                .actionType(ActualProductionHistoryAction.PRODUCTION_IN)
                                .referenceName(plan.getPlanName())
                                .build();
                        actualProductionHistoryRepository.save(history);
                        log.info("Backfilled {} units of {} from plan {} into Actual Production Pool", qty, item.getProductName(), plan.getPlanName());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to sync distributed plans to Actual Production Pool: {}", e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public void distributeToOutlet(ActualProductionDistributeRequestDto requestDto) {
        log.info("Distributing {} units of product {} to outlet {}", 
                requestDto.getQuantity(), requestDto.getProductId(), requestDto.getOutletId());

        ActualProduction actualProduction = actualProductionRepository.findByProductId(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found in Actual Production pool."));

        if (actualProduction.getAvailableQuantity() < requestDto.getQuantity()) {
            throw new RuntimeException("Insufficient quantity in Actual Production pool. Available: " 
                    + actualProduction.getAvailableQuantity());
        }

        Outlet outlet = outletRepository.findById(requestDto.getOutletId())
                .orElseThrow(() -> new RuntimeException("Outlet not found."));

        Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found."));

        // Deduct from pool
        actualProduction.setAvailableQuantity(actualProduction.getAvailableQuantity() - requestDto.getQuantity());
        actualProductionRepository.save(actualProduction);

        // Date for distribution
        LocalDate distDate = requestDto.getDistributionDate() != null ? requestDto.getDistributionDate() : LocalDate.now();

        // Create Distribution Plan
        DistributionPlan dp = DistributionPlan.builder()
                .name("Manual Distribution - " + product.getProductName())
                .outlet(outlet)
                .date(distDate)
                .isActive(true)
                .status(DistributionPlanStatus.NOT_RECEIVED)
                .productionPlan(null) // Not linked to a specific plan
                .build();
        
        dp = distributionPlanRepository.save(dp);

        DistributionPlanItem dpi = DistributionPlanItem.builder()
                .distributionPlan(dp)
                .product(product)
                .qty(requestDto.getQuantity())
                .build();
        
        distributionPlanItemRepository.save(dpi);
        
        // Also create a GTN so it shows up in the POS Goods Entry
        CreateGtnRequestDto.GtnItemRequest gtnItem = CreateGtnRequestDto.GtnItemRequest.builder()
                .productId(product.getId())
                .expectedQty((double) requestDto.getQuantity())
                .receivedQty(0.0)
                .status(GtnStatus.NOT_RECEIVED)
                .expiryDate(LocalDateTime.now().plusDays(7)) // Default expiry
                .unit(ProductUnit.PIECES)
                .entryStatus(EntryStatus.SYSTEM)
                .remarks("Manager Distribution")
                .build();

        CreateGtnRequestDto gtnRequest = CreateGtnRequestDto.builder()
                .date(LocalDateTime.now())
                .source(GtnSource.BAKERY)
                .addedBy("35000000-0000-0000-0000-000000000000") // System/Manager ID placeholder
                .outletId(requestDto.getOutletId())
                .status(GtnStatus.NOT_RECEIVED)
                .items(Collections.singletonList(gtnItem))
                .build();

        gtnService.createGtn(gtnRequest);

        // Add history record
        ActualProductionHistory history = ActualProductionHistory.builder()
                .productId(product.getId())
                .productName(product.getProductName())
                .quantity(requestDto.getQuantity())
                .actionType(ActualProductionHistoryAction.DISTRIBUTION_OUT)
                .referenceName(outlet.getName())
                .build();
        actualProductionHistoryRepository.save(history);

        log.info("Successfully created distribution plan {} and GTN for Outlet {}.", dp.getDpId(), outlet.getOutletId());
    }

    @Override
    public List<ActualProductionHistoryDto> getProductionHistory() {
        return actualProductionHistoryRepository.findByActionTypeOrderByCreatedAtDesc(ActualProductionHistoryAction.PRODUCTION_IN)
                .stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ActualProductionHistoryDto> getDistributionHistory() {
        return actualProductionHistoryRepository.findByActionTypeOrderByCreatedAtDesc(ActualProductionHistoryAction.DISTRIBUTION_OUT)
                .stream()
                .map(this::mapToHistoryDto)
                .collect(Collectors.toList());
    }

    private ActualProductionHistoryDto mapToHistoryDto(ActualProductionHistory history) {
        return ActualProductionHistoryDto.builder()
                .id(history.getId())
                .productId(history.getProductId())
                .productName(history.getProductName())
                .quantity(history.getQuantity())
                .actionType(history.getActionType())
                .referenceName(history.getReferenceName())
                .createdAt(history.getCreatedAt())
                .build();
    }

    @Override
    public List<DayProductionItemResponseDto> getOutletStock(Long outletId) {
        return dayProductionService.getTodayProductionItems(outletId);
    }

    @Override
    public void createOutletTransferRequest(com.plover.backerymanagmentsystem.manager.dto.OutletTransferRequestDto requestDto) {
        com.plover.backerymanagmentsystem.manager.model.Product product = productRepository.findById(requestDto.getProductId())
                .orElseThrow(() -> new RuntimeException("Product not found with id: " + requestDto.getProductId()));

        com.plover.backerymanagmentsystem.pos.model.OutletTransferRequest request = com.plover.backerymanagmentsystem.pos.model.OutletTransferRequest.builder()
                .sourceOutletId(requestDto.getSourceOutletId())
                .destinationOutletId(requestDto.getDestinationOutletId())
                .product(product)
                .requestedQuantity(requestDto.getRequestedQuantity())
                .status(com.plover.backerymanagmentsystem.pos.model.OutletTransferRequestStatus.PENDING)
                .build();
        outletTransferRequestRepository.save(request);
        log.info("Created Outlet Transfer Request from {} to {} for {} units of {}", 
                requestDto.getSourceOutletId(), requestDto.getDestinationOutletId(), requestDto.getRequestedQuantity(), product.getProductName());
    }
}
