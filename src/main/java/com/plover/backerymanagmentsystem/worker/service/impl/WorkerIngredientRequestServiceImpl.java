package com.plover.backerymanagmentsystem.worker.service.impl;

import java.time.LocalDateTime;
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
import com.plover.backerymanagmentsystem.manager.repository.IngredientRequestRepository;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.worker.dto.CreateIngredientRequestDto;
import com.plover.backerymanagmentsystem.worker.dto.IngredientRequestDto;
import com.plover.backerymanagmentsystem.worker.dto.IngredientRequestItemDto;
import com.plover.backerymanagmentsystem.worker.service.WorkerIngredientRequestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkerIngredientRequestServiceImpl implements WorkerIngredientRequestService {

    private final IngredientRequestRepository ingredientRequestRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final com.plover.backerymanagmentsystem.store_keeper.repository.StoreKeeperProductionPlanRepository productionPlanRepository;
    private final com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialRequirementRepository rawMaterialRequirementRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository productionCenterRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.MiniStoreRepository miniStoreRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.MiniStoreItemRepository miniStoreItemRepository;
    @org.springframework.context.annotation.Lazy
    private final com.plover.backerymanagmentsystem.manager.service.ProductionPlanningService productionPlanningService;

    @Override
    public IngredientRequestDto create(CreateIngredientRequestDto dto) {
        AuthModel user = currentUser();
        Long pcId = resolvePcId(user);

        IngredientRequest request = IngredientRequest.builder()
                .productionPlanId(dto.getProductionPlanId())
                .productionCenterId(pcId)
                .status(IngredientRequestStatus.PENDING)
                .notes(dto.getNotes())
                .build();

        List<IngredientRequestItem> items = dto.getItems().stream().map(input -> {
            RawMaterial rm = rawMaterialRepository.findById(input.getRawMaterialId())
                    .orElseThrow(() -> new RuntimeException("Raw material not found: " + input.getRawMaterialId()));
            
            String displayName = rm.getMaterialName();
            if (rm.getBrand() != null && rm.getBrand().getGenericMaterial() != null) {
                displayName = rm.getBrand().getGenericMaterial().getName() + " (" + rm.getBrand().getName() + ")";
            }

            return IngredientRequestItem.builder()
                    .request(request)
                    .rawMaterialId(rm.getId())
                    .rawMaterialName(displayName)
                    .requestedQty(input.getRequestedQty())
                    .unitOfMeasure(rm.getUnitOfMeasure())
                    .build();
        }).collect(Collectors.toList());

        request.setItems(items);
        IngredientRequest saved = ingredientRequestRepository.save(request);
        log.info("Worker {} created ingredient request {} for PC {}", user.getUsername(), saved.getId(), pcId);
        return toDto(saved);
    }

    @Override
    public List<IngredientRequestDto> listMyRequests() {
        AuthModel user = currentUser();
        Long pcId = resolvePcId(user);

        List<IngredientRequest> requests = ingredientRequestRepository.findByProductionCenterIdOrderByCreatedAtDesc(pcId);

        for (IngredientRequest req : requests) {
            if (req.getStatus() == IngredientRequestStatus.PENDING && req.getProductionPlanId() != null) {
                boolean isPlanIssued = rawMaterialRequirementRepository.existsByProductionOrder_ProductionPlan_Id(req.getProductionPlanId());
                if (isPlanIssued) {
                    req.setStatus(IngredientRequestStatus.ISSUED);
                    if (req.getIssuedAt() == null) req.setIssuedAt(LocalDateTime.now());
                    if (req.getItems() != null) {
                        for (IngredientRequestItem item : req.getItems()) {
                            if (item.getIssuedQty() == null || item.getIssuedQty() <= 0) {
                                item.setIssuedQty(item.getRequestedQty());
                            }
                        }
                    }
                    ingredientRequestRepository.save(req);
                }
            }
        }

        return requests.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public IngredientRequestDto confirmReceipt(Long requestId) {
        AuthModel user = currentUser();
        Long pcId = user.getMpcId() != null ? user.getMpcId() : user.getProductionCenterId();

        IngredientRequest request = ingredientRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Ingredient request not found: " + requestId));

        if (pcId != null && request.getProductionCenterId() != null && !request.getProductionCenterId().equals(pcId)) {
            log.warn("Worker PC ID {} attempted to confirm receipt for request belonging to PC ID {}", pcId, request.getProductionCenterId());
        }

        boolean wasAlreadyReceived = request.getStatus() == IngredientRequestStatus.RECEIVED;

        request.setStatus(IngredientRequestStatus.RECEIVED);
        request.setReceivedAt(LocalDateTime.now());
        IngredientRequest saved = ingredientRequestRepository.save(request);
        log.info("Worker {} confirmed receipt for request {}", user.getUsername(), requestId);

        // Ensure MiniStore of this Production Center exists and upsert received items into mini_store_items
        if (request.getProductionCenterId() != null) {
            com.plover.backerymanagmentsystem.manager.model.ProductionCenter center = 
                    productionCenterRepository.findById(request.getProductionCenterId()).orElse(null);
            if (center != null) {
                com.plover.backerymanagmentsystem.manager.model.MiniStore miniStore = center.getMiniStore();
                if (miniStore == null) {
                    miniStore = miniStoreRepository.save(com.plover.backerymanagmentsystem.manager.model.MiniStore.builder()
                            .name(center.getCenterName() + " Store")
                            .storeDate(java.time.LocalDate.now())
                            .build());
                    center.setMiniStore(miniStore);
                    productionCenterRepository.save(center);
                }

                if (!wasAlreadyReceived && request.getItems() != null) {
                    List<com.plover.backerymanagmentsystem.manager.model.MiniStoreItem> existingItems = 
                            miniStoreItemRepository.findByMiniStore_MiniStoreId(miniStore.getMiniStoreId());

                    for (IngredientRequestItem item : request.getItems()) {
                        Double qty = item.getIssuedQty() != null && item.getIssuedQty() > 0 
                                ? item.getIssuedQty() 
                                : item.getRequestedQty();

                        if (qty != null && qty > 0 && item.getRawMaterialId() != null) {
                            RawMaterial rm = rawMaterialRepository.findById(item.getRawMaterialId()).orElse(null);
                            String name = rm != null ? rm.getDisplayName() : item.getRawMaterialName();

                            com.plover.backerymanagmentsystem.manager.model.MiniStoreItem existing = existingItems.stream()
                                    .filter(i -> i.getRawMaterialId() != null && i.getRawMaterialId().equals(item.getRawMaterialId()))
                                    .findFirst().orElse(null);

                            java.math.BigDecimal qtyBD = java.math.BigDecimal.valueOf(qty);
                            if (existing != null) {
                                existing.setSystemQty(existing.getSystemQty().add(qtyBD));
                                existing.setPhysicalQty(existing.getPhysicalQty().add(qtyBD));
                                if (name != null) existing.setName(name);
                                miniStoreItemRepository.save(existing);
                            } else {
                                com.plover.backerymanagmentsystem.manager.model.MiniStoreItem newItem = 
                                        com.plover.backerymanagmentsystem.manager.model.MiniStoreItem.builder()
                                        .name(name)
                                        .rawMaterialId(item.getRawMaterialId())
                                        .systemQty(qtyBD)
                                        .physicalQty(qtyBD)
                                        .miniStore(miniStore)
                                        .build();
                                miniStoreItemRepository.save(newItem);
                            }
                        }
                    }
                }
            }
        }

        return toDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<com.plover.backerymanagmentsystem.admin.dto.RawMaterialDto> getRawMaterials() {
        List<RawMaterial> materials = rawMaterialRepository.findAll();
        return materials.stream()
                .filter(rm -> rm.getIsActive() != null && rm.getIsActive())
                .map(rm -> com.plover.backerymanagmentsystem.admin.dto.RawMaterialDto.builder()
                        .id(rm.getId())
                        .materialName(rm.getDisplayName())
                        .materialCode(rm.getMaterialCode())
                        .unitOfMeasure(rm.getUnitOfMeasure())
                        .currentStock(rm.getCurrentStock())
                        .minimumStockLevel(rm.getMinimumStockLevel())
                        .category(rm.getCategory())
                        .brand(rm.getBrandName())
                        .genericMaterialName(rm.getGenericMaterialName())
                        .unitCost(rm.getUnitCost())
                        .isActive(rm.getIsActive())
                        .build())
                .collect(Collectors.toList());
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private AuthModel currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthModel)) {
            throw new RuntimeException("Not authenticated");
        }
        return (AuthModel) auth.getPrincipal();
    }

    private Long resolvePcId(AuthModel user) {
        Long pcId = user.getMpcId() != null ? user.getMpcId() : user.getProductionCenterId();
        if (pcId == null) {
            log.warn("Access denied for worker {}: No production center assigned", user.getUsername());
            throw new RuntimeException("ACCESS_DENIED: Your user account is not assigned to any Production Center. Please contact your administrator.");
        }

        try {
            com.plover.backerymanagmentsystem.manager.model.ProductionCenter pc = productionCenterRepository.findById(pcId).orElse(null);
            if (pc != null && (pc.getIsActive() == null || !pc.getIsActive())) {
                List<com.plover.backerymanagmentsystem.manager.model.ProductionCenter> activeCenters = 
                        productionCenterRepository.findByTypeAndIsActiveTrue(pc.getType());
                if (!activeCenters.isEmpty()) {
                    log.info("Redirecting worker {} from inactive center {} ({}) to active center {} ({})",
                            user.getUsername(), pcId, pc.getCenterName(), activeCenters.get(0).getId(), activeCenters.get(0).getCenterName());
                    return activeCenters.get(0).getId();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to check center active status for pcId {}: {}", pcId, e.getMessage());
        }

        return pcId;
    }

    private byte[] uuidToBytes(UUID uuid) {
        if (uuid == null) return null;
        long msb = uuid.getMostSignificantBits();
        long lsb = uuid.getLeastSignificantBits();
        byte[] bytes = new byte[16];
        for (int i = 7; i >= 0; i--) {
            bytes[i] = (byte) (msb & 0xFF);
            msb >>= 8;
        }
        for (int i = 15; i >= 8; i--) {
            bytes[i] = (byte) (lsb & 0xFF);
            lsb >>= 8;
        }
        return bytes;
    }

    private IngredientRequestDto toDto(IngredientRequest r) {
        List<IngredientRequestItemDto> itemDtos = r.getItems() == null ? List.of() :
                r.getItems().stream().map(i -> IngredientRequestItemDto.builder()
                        .id(i.getId())
                        .rawMaterialId(i.getRawMaterialId())
                        .rawMaterialName(i.getRawMaterialName())
                        .requestedQty(i.getRequestedQty())
                        .issuedQty(i.getIssuedQty())
                        .unitOfMeasure(i.getUnitOfMeasure())
                        .build()).collect(Collectors.toList());

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
