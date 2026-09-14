package com.plover.backerymanagmentsystem.admin.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.plover.backerymanagmentsystem.admin.dto.AdminProductionCenterResponse;
import com.plover.backerymanagmentsystem.admin.dto.MiniStoreResponse;
import com.plover.backerymanagmentsystem.admin.service.AdminProductionCenterService;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenterType;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;

import com.plover.backerymanagmentsystem.admin.dto.CreateProductionCenterRequest;
import com.plover.backerymanagmentsystem.admin.dto.UpdateProductionCenterRequest;
import com.plover.backerymanagmentsystem.manager.repository.MiniStoreRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminProductionCenterServiceImpl implements AdminProductionCenterService {

        private final ProductionCenterRepository productionCenterRepository;
        private final MiniStoreRepository miniStoreRepository;

        @Override
        public List<AdminProductionCenterResponse> getAllProductionCenters() {
                log.info("Fetching all production centers");
                List<ProductionCenter> productionCenters = productionCenterRepository.findAll();

                return productionCenters.stream().map(pc -> {
                        String miniStoreName = pc.getMiniStore() != null ? pc.getMiniStore().getName() : null;

                        return AdminProductionCenterResponse.builder()
                                        .id(pc.getId())
                                        .centerName(pc.getCenterName())
                                        .location(pc.getLocation())
                                        .isActive(pc.getIsActive())
                                        .miniStoreName(miniStoreName)
                                        .type(pc.getType())
                                        .establishedDate(pc.getEstablishedDate())
                                        .createdAt(pc.getCreatedAt())
                                        .updatedAt(pc.getUpdatedAt())
                                        .build();
                }).collect(Collectors.toList());
        }

        @Override
        public List<MiniStoreResponse> getAllMiniStores() {
                log.info("Fetching all mini stores");
                return miniStoreRepository.findAll().stream()
                                .map(ms -> MiniStoreResponse.builder()
                                                .miniStoreId(ms.getMiniStoreId())
                                                .name(ms.getName())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Override
        public AdminProductionCenterResponse createProductionCenter(CreateProductionCenterRequest request) {
                log.info("Creating new production center: {}", request.getProductionCenterName());

                com.plover.backerymanagmentsystem.manager.model.MiniStore miniStore = null;
                
                // First check if miniStoreId is provided and exists
                if (request.getMiniStoreId() != null && request.getMiniStoreId() != 0) {
                        miniStore = miniStoreRepository.findById(request.getMiniStoreId()).orElse(null);
                }

                // If not found by ID, check if miniStoreName is provided to find or create
                if (miniStore == null && request.getMiniStoreName() != null && !request.getMiniStoreName().trim().isEmpty()) {
                        miniStore = miniStoreRepository.findByName(request.getMiniStoreName().trim()).orElse(null);

                        if (miniStore == null) {
                                log.info("Creating new MiniStore: {}", request.getMiniStoreName());
                                miniStore = com.plover.backerymanagmentsystem.manager.model.MiniStore.builder()
                                                .name(request.getMiniStoreName().trim())
                                                .storeDate(java.time.LocalDate.now()) // Setting current date as default
                                                .build();
                                miniStore = miniStoreRepository.save(miniStore);
                        }
                }

                // If miniStoreId was provided but not found, and no name provided to create NEW one, throw exception
                if (miniStore == null && request.getMiniStoreId() != null && request.getMiniStoreId() != 0) {
                        throw new RuntimeException("MiniStore not found with id: " + request.getMiniStoreId());
                }

                ProductionCenter productionCenter = ProductionCenter.builder()
                                .centerName(request.getProductionCenterName())
                                .location(request.getLocation())
                                .isActive(request.getIsActive())
                                .miniStore(miniStore)
                                .establishedDate(request.getEstablishedDate())
                                .type(request.getType() != null ? request.getType()
                                                : com.plover.backerymanagmentsystem.manager.model.ProductionCenterType.BAKERY)
                                .build();

                ProductionCenter savedCenter = productionCenterRepository.save(productionCenter);

                return AdminProductionCenterResponse.builder()
                                .id(savedCenter.getId())
                                .centerName(savedCenter.getCenterName())
                                .location(savedCenter.getLocation())
                                .isActive(savedCenter.getIsActive())
                                .miniStoreName(savedCenter.getMiniStore() != null ? savedCenter.getMiniStore().getName()
                                                : null)
                                .type(savedCenter.getType())
                                .establishedDate(savedCenter.getEstablishedDate())
                                .createdAt(savedCenter.getCreatedAt())
                                .updatedAt(savedCenter.getUpdatedAt())
                                .build();
        }

        @Override
        public AdminProductionCenterResponse updateProductionCenter(Long id, UpdateProductionCenterRequest request) {
                log.info("Updating production center: {}", id);

                ProductionCenter productionCenter = productionCenterRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Production center not found with id: " + id));

                productionCenter.setCenterName(request.getProductionCenterName());
                productionCenter.setLocation(request.getLocation());
                productionCenter.setIsActive(request.getIsActive());
                productionCenter.setEstablishedDate(request.getEstablishedDate());

                if (request.getType() != null) {
                        productionCenter.setType(request.getType());
                }

                if (request.getMiniStoreId() != null) {
                        com.plover.backerymanagmentsystem.manager.model.MiniStore miniStore = miniStoreRepository
                                        .findById(request.getMiniStoreId())
                                        .orElseThrow(
                                                        () -> new RuntimeException("MiniStore not found with id: "
                                                                        + request.getMiniStoreId()));
                        productionCenter.setMiniStore(miniStore);
                }

                ProductionCenter savedCenter = productionCenterRepository.save(productionCenter);

                return AdminProductionCenterResponse.builder()
                                .id(savedCenter.getId())
                                .centerName(savedCenter.getCenterName())
                                .location(savedCenter.getLocation())
                                .isActive(savedCenter.getIsActive())
                                .miniStoreName(savedCenter.getMiniStore() != null ? savedCenter.getMiniStore().getName()
                                                : null)
                                .type(savedCenter.getType())
                                .establishedDate(savedCenter.getEstablishedDate())
                                .createdAt(savedCenter.getCreatedAt())
                                .updatedAt(savedCenter.getUpdatedAt())
                                .build();
        }

        @Override
        public void deleteProductionCenter(Long id) {
                log.info("Deleting production center: {}", id);
                if (!productionCenterRepository.existsById(id)) {
                        throw new RuntimeException("Production center not found with id: " + id);
                }
                productionCenterRepository.deleteById(id);
        }

        @Override
        public List<AdminProductionCenterResponse> getProductionCentersByType(ProductionCenterType type) {
                log.info("Fetching production centers of type: {}", type);
                return productionCenterRepository.findActiveAndEstablishedByType(type).stream().map(pc -> {
                        String miniStoreName = pc.getMiniStore() != null ? pc.getMiniStore().getName() : null;
                        return AdminProductionCenterResponse.builder()
                                        .id(pc.getId())
                                        .centerName(pc.getCenterName())
                                        .location(pc.getLocation())
                                        .isActive(pc.getIsActive())
                                        .miniStoreName(miniStoreName)
                                        .type(pc.getType())
                                        .establishedDate(pc.getEstablishedDate())
                                        .createdAt(pc.getCreatedAt())
                                        .updatedAt(pc.getUpdatedAt())
                                        .build();
                }).collect(Collectors.toList());
        }
}
