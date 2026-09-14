package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.manager.model.IngredientRequest;
import com.plover.backerymanagmentsystem.manager.model.IngredientRequestItem;
import com.plover.backerymanagmentsystem.manager.model.IngredientRequestStatus;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.MiniStore;
import com.plover.backerymanagmentsystem.manager.model.MiniStoreItem;
import com.plover.backerymanagmentsystem.manager.repository.IngredientRequestRepository;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.MiniStoreItemRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.IssueIngredientRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.exception.InsufficientStockException;
import com.plover.backerymanagmentsystem.store_keeper.service.StorekeeperIngredientRequestService;
import com.plover.backerymanagmentsystem.worker.dto.IngredientRequestDto;
import com.plover.backerymanagmentsystem.worker.dto.IngredientRequestItemDto;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class StorekeeperIngredientRequestServiceImpl implements StorekeeperIngredientRequestService {

    private final IngredientRequestRepository ingredientRequestRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final ProductionCenterRepository productionCenterRepository;
    private final MiniStoreItemRepository miniStoreItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<IngredientRequestDto> listPending() {
        return ingredientRequestRepository
                .findByStatusOrderByCreatedAtAsc(IngredientRequestStatus.PENDING)
                .stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<IngredientRequestDto> listAll() {
        return ingredientRequestRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(IngredientRequest::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public IngredientRequestDto issue(Long requestId, IssueIngredientRequestDto dto) {
        IngredientRequest request = ingredientRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Ingredient request not found: " + requestId));

        if (request.getStatus() != IngredientRequestStatus.PENDING) {
            throw new RuntimeException("Cannot issue: request is not in PENDING status (current: " + request.getStatus() + ")");
        }

        List<String> insufficientStockDetails = new ArrayList<>();

        // First validation pass: check if there is enough stock for all items
        if (dto.getItems() != null) {
            for (IssueIngredientRequestDto.ItemIssue itemIssue : dto.getItems()) {
                if (itemIssue.getIssuedQty() != null && itemIssue.getIssuedQty() > 0) {
                    IngredientRequestItem reqItem = request.getItems().stream()
                            .filter(i -> i.getId().equals(itemIssue.getItemId()))
                            .findFirst()
                            .orElseThrow(() -> new RuntimeException("Item not found: " + itemIssue.getItemId()));

                    RawMaterial rm = rawMaterialRepository.findById(reqItem.getRawMaterialId())
                            .orElseThrow(() -> new RuntimeException("Raw material not found: " + reqItem.getRawMaterialId()));

                    double totalAvailable = 0.0;
                    String genericName = rm.getGenericMaterialName();
                    List<RawMaterial> allBatches;
                    if (genericName != null && !genericName.trim().isEmpty()) {
                        allBatches = rawMaterialRepository.findAllByGenericMaterialName(genericName);
                    } else if (rm.getMaterialCode() != null) {
                        allBatches = rawMaterialRepository.findAllByMaterialCode(rm.getMaterialCode());
                    } else {
                        allBatches = List.of(rm);
                    }
                    totalAvailable = allBatches.stream()
                            .map(RawMaterial::getCurrentStock)
                            .filter(java.util.Objects::nonNull)
                            .mapToDouble(Double::doubleValue)
                            .sum();

                    if (totalAvailable < itemIssue.getIssuedQty()) {
                        insufficientStockDetails.add(String.format("%s (Requested: %.2f, Available: %.2f)", 
                                rm.getMaterialName(), reqItem.getRequestedQty(), totalAvailable));
                    }
                }
            }
        }

        if (!insufficientStockDetails.isEmpty()) {
            throw new InsufficientStockException(insufficientStockDetails);
        }

        // Apply issued quantities and deduct stock
        ProductionCenter productionCenter = productionCenterRepository.findById(request.getProductionCenterId()).orElse(null);
        MiniStore miniStore = (productionCenter != null) ? productionCenter.getMiniStore() : null;

        if (dto.getItems() != null) {
            for (IssueIngredientRequestDto.ItemIssue itemIssue : dto.getItems()) {
                IngredientRequestItem reqItem = request.getItems().stream()
                        .filter(i -> i.getId().equals(itemIssue.getItemId()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("Item not found: " + itemIssue.getItemId()));

                reqItem.setIssuedQty(itemIssue.getIssuedQty());

                // Deduct from raw material stock (FIFO batch-wise deduction)
                if (itemIssue.getIssuedQty() != null && itemIssue.getIssuedQty() > 0) {
                    RawMaterial requestedMaterial = rawMaterialRepository.findById(reqItem.getRawMaterialId())
                            .orElseThrow(() -> new RuntimeException("Raw material not found: " + reqItem.getRawMaterialId()));

                    double remainingToIssue = itemIssue.getIssuedQty();

                    // Collect all batches for this generic name / material code and sort strictly by ID ascending (First Come First Serve / FIFO)
                    List<RawMaterial> availableBatches = new ArrayList<>();
                    String genericName = requestedMaterial.getGenericMaterialName();
                    List<RawMaterial> allBatches;
                    if (genericName != null && !genericName.trim().isEmpty()) {
                        allBatches = rawMaterialRepository.findAllByGenericMaterialName(genericName);
                    } else if (requestedMaterial.getMaterialCode() != null) {
                        allBatches = rawMaterialRepository.findAllByMaterialCode(requestedMaterial.getMaterialCode());
                    } else {
                        allBatches = List.of(requestedMaterial);
                    }
                    availableBatches = allBatches.stream()
                            .filter(b -> b.getCurrentStock() != null && b.getCurrentStock() > 0)
                            .sorted(java.util.Comparator.comparing(RawMaterial::getId))
                            .collect(Collectors.toList());

                    // Fallback to the requested batch if no active batches with stock were resolved
                    if (availableBatches.isEmpty()) {
                        availableBatches.add(requestedMaterial);
                    }

                    for (RawMaterial batch : availableBatches) {
                        if (remainingToIssue <= 0) break;

                        Double batchStock = batch.getCurrentStock();
                        if (batchStock == null || batchStock <= 0) continue;

                        double issueFromThisBatch = Math.min(remainingToIssue, batchStock);
                        double newStock = batchStock - issueFromThisBatch;

                        batch.setCurrentStock(newStock);
                        rawMaterialRepository.save(batch);
                        log.info("Deducted {} {} from raw material batch ID: {} {} (new stock: {})",
                                 issueFromThisBatch, batch.getUnitOfMeasure(), batch.getId(), batch.getMaterialName(), newStock);

                        // Note: Mini store items are upserted when the worker confirms receipt in the Worker module.

                        remainingToIssue -= issueFromThisBatch;
                    }
                }
            }
        }

        request.setStatus(IngredientRequestStatus.ISSUED);
        request.setIssuedAt(LocalDateTime.now());

        IngredientRequest saved = ingredientRequestRepository.save(request);
        log.info("Storekeeper issued ingredient request {}", requestId);
        return toDto(saved);
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private byte[] requireCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthModel user) {
            return user.getId();
        }
        return null;
    }

    private IngredientRequestDto toDto(IngredientRequest r) {
        List<IngredientRequestItemDto> itemDtos = r.getItems() == null ? List.of() :
                r.getItems().stream().map(i -> {
                    Double available = 0.0;
                    try {
                        RawMaterial requestedMaterial = rawMaterialRepository.findById(i.getRawMaterialId()).orElse(null);
                        if (requestedMaterial != null) {
                            String genericName = requestedMaterial.getGenericMaterialName();
                            List<RawMaterial> allBatches;
                            if (genericName != null && !genericName.trim().isEmpty()) {
                                allBatches = rawMaterialRepository.findAllByGenericMaterialName(genericName);
                            } else if (requestedMaterial.getMaterialCode() != null) {
                                allBatches = rawMaterialRepository.findAllByMaterialCode(requestedMaterial.getMaterialCode());
                            } else {
                                allBatches = List.of(requestedMaterial);
                            }
                            available = allBatches.stream()
                                    .map(RawMaterial::getCurrentStock)
                                    .filter(java.util.Objects::nonNull)
                                    .mapToDouble(Double::doubleValue)
                                    .sum();
                        }
                    } catch (Exception ignored) {}

                    return IngredientRequestItemDto.builder()
                        .id(i.getId())
                        .rawMaterialId(i.getRawMaterialId())
                        .rawMaterialName(i.getRawMaterialName())
                        .requestedQty(i.getRequestedQty())
                        .issuedQty(i.getIssuedQty())
                        .availableQty(available)
                        .unitOfMeasure(i.getUnitOfMeasure())
                        .build();
                }).collect(Collectors.toList());

        return IngredientRequestDto.builder()
                .id(r.getId())
                .productionPlanId(r.getProductionPlanId())
                .productionCenterId(r.getProductionCenterId())
                .status(r.getStatus() != null ? r.getStatus().name() : null)
                .notes(r.getNotes())
                .createdAt(r.getCreatedAt())
                .issuedAt(r.getIssuedAt())
                .receivedAt(r.getReceivedAt())
                .items(itemDtos)
                .build();
    }
}
