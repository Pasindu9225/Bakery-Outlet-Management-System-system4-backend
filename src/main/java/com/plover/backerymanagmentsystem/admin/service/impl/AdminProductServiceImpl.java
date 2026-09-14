package com.plover.backerymanagmentsystem.admin.service.impl;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.plover.backerymanagmentsystem.admin.dto.AdminProductDto;
import com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto;
import com.plover.backerymanagmentsystem.admin.dto.ProductionStageResponseDto;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.admin.service.AdminProductService;
import com.plover.backerymanagmentsystem.admin.service.AdminBOMService;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionStageRepository;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.BillOfMaterialRepository;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.model.BillOfMaterial;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.model.GenericMaterial;
import com.plover.backerymanagmentsystem.manager.model.Brand;
import com.plover.backerymanagmentsystem.manager.repository.GenericMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.BrandRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class AdminProductServiceImpl implements AdminProductService {

        private final ProductRepository productRepository;
        private final ProductionCenterRepository productionCenterRepository;
        private final com.plover.backerymanagmentsystem.manager.repository.CategoryRepository categoryRepository;
        private final ProductionStageRepository productionStageRepository;
        private final RawMaterialRepository rawMaterialRepository;
        private final GenericMaterialRepository genericMaterialRepository;
        private final BrandRepository brandRepository;
        private final AdminBOMService adminBOMService;
        private final BillOfMaterialRepository billOfMaterialRepository;

        @Override
        public List<CategoryResponseDto> getAllCategories() {
                log.info("Fetching all categories for admin view");
                return categoryRepository.findAll().stream()
                                .map(category -> CategoryResponseDto.builder()
                                                .id(category.getId())
                                                .name(category.getName())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Override
        public List<ProductionStageResponseDto> getAllProductionStages() {
                log.info("Fetching all production stages for admin view");
                return productionStageRepository.findAll().stream()
                                .map(stage -> ProductionStageResponseDto.builder()
                                                .id(stage.getId())
                                                .productionStage(stage.getProductionStage())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Override
        public List<com.plover.backerymanagmentsystem.admin.dto.ProductionCenterResponseDto> getAllProductionCenters() {
                log.info("Fetching all production centers for admin view");
                return productionCenterRepository.findActiveAndEstablished().stream()
                                .map(center -> com.plover.backerymanagmentsystem.admin.dto.ProductionCenterResponseDto
                                                .builder()
                                                .id(center.getId())
                                                .centerName(center.getCenterName())
                                                .build())
                                .collect(Collectors.toList());
        }

        @Override
        public List<AdminProductDto> getAllProducts() {
                log.info("Fetching all products for admin view");

                // Fetch all products
                List<Product> products = productRepository.findAll();

                // Fetch all production centers and map by ID for efficient lookup
                Map<Long, ProductionCenter> productionCenterMap = productionCenterRepository.findAll().stream()
                                .collect(Collectors.toMap(ProductionCenter::getId, Function.identity()));

                // Convert to DTOs
                return products.stream()
                                .map(product -> convertToDto(product, productionCenterMap))
                                .collect(Collectors.toList());
        }

        @Override
        @Transactional
        public AdminProductDto createProduct(
                        com.plover.backerymanagmentsystem.admin.dto.CreateProductRequestDto request) {
                log.info("Creating new product: {}", request.getProductName());

                // Validate production center
                if (request.getProductionCenterId() != null
                                && !productionCenterRepository.existsById(request.getProductionCenterId())) {
                        throw new IllegalArgumentException(
                                        "Production Center not found with ID: " + request.getProductionCenterId());
                }

                // Handle Category
                com.plover.backerymanagmentsystem.manager.model.Category category = null;
                if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
                        String categoryInput = request.getCategory().trim();
                        try {
                                // Try finding category by ID first if input is numeric
                                Integer categoryId = Integer.parseInt(categoryInput);
                                category = categoryRepository.findById(categoryId).orElse(null);
                        } catch (NumberFormatException e) {
                                // Input is not a number, find by name
                                category = categoryRepository.findByName(categoryInput).orElse(null);
                        }

                        // Fallback: If category not found, create a new one by name
                        if (category == null) {
                                log.info("Category '{}' not found, creating new one.", categoryInput);
                                com.plover.backerymanagmentsystem.manager.model.Category newCategory = com.plover.backerymanagmentsystem.manager.model.Category
                                                .builder()
                                                .name(categoryInput)
                                                .description("Auto-created during product creation")
                                                .build();
                                category = categoryRepository.save(newCategory);
                        }
                }

                // Create Product Entity
                Product product = Product.builder()
                                .productName(request.getProductName())
                                .productCode(request.getProductCode())
                                .description(request.getDescription())
                                .unitPrice(request.getUnitPrice())
                                .categoryRef(category)
                                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                                .productionCenterId(request.getProductionCenterId())
                                .gbMargin(request.getGbMargin())
                                .vatStatus(request.getVatStatus())
                                .brand(request.getBrand())
                                .productionStageRef(request.getProductionStageId() != null
                                                ? productionStageRepository.findById(request.getProductionStageId())
                                                                .orElseThrow(() -> new IllegalArgumentException(
                                                                                "Production Stage not found with ID: "
                                                                                                + request.getProductionStageId()))
                                                : null)
                                .salePrice(request.getSalePrice())
                                .actualGP(request.getActualGP())
                                .maxOrderQty(request.getMaxOrderQty())
                                .minOrderQty(request.getMinOrderQty())
                                .unitOfMeasure(request.getUnitOfMeasure())
                                .isKotEnabled(request.getIsKotEnabled() != null ? request.getIsKotEnabled() : false)
                                .isFastMoving(request.getIsFastMoving() != null ? request.getIsFastMoving() : false)
                                .shelfLifeDays(request.getShelfLifeDays())
                                .build();

                Product savedProduct = productRepository.save(product);
                log.info("Product created successfully with ID: {}", savedProduct.getId());

                // If product category is 'Oilman', attempt secondary raw_materials sync safely without failing product creation
                try {
                        boolean isOilman = false;
                        if (request.getCategory() != null && "Oilman".equalsIgnoreCase(request.getCategory().trim())) {
                                isOilman = true;
                        } else if (savedProduct.getCategoryRef() != null
                                        && Integer.valueOf(3).equals(savedProduct.getCategoryRef().getId())) {
                                isOilman = true;
                        }

                        if (isOilman) {
                                log.info("Product category is identified as oilman, processing optional raw_materials sync.");

                                String materialName = request.getProductName();
                                String brandName = request.getBrand();
                                String categoryName = savedProduct.getCategoryRef() != null
                                                ? savedProduct.getCategoryRef().getName()
                                                : "Oilman";

                                // Find or Create GenericMaterial
                                GenericMaterial gm = genericMaterialRepository.findByName(materialName)
                                                .orElseGet(() -> genericMaterialRepository.save(GenericMaterial.builder()
                                                                .name(materialName)
                                                                .unitOfMeasure(request.getUnitOfMeasure() != null && !request.getUnitOfMeasure().trim().isEmpty() ? request.getUnitOfMeasure() : "NOS")
                                                                .category(categoryName)
                                                                .build()));

                                String validBrandName = (brandName != null && !brandName.trim().isEmpty()) ? brandName.trim() : "Unbranded";

                                // Find or Create Brand
                                Brand b = brandRepository.findByNameAndGenericMaterialId(validBrandName, gm.getId())
                                                .stream().findFirst()
                                                .orElseGet(() -> brandRepository.save(Brand.builder()
                                                                .name(validBrandName)
                                                                .genericMaterial(gm)
                                                                .build()));

                                boolean rmExists = false;
                                if (request.getProductCode() != null && !request.getProductCode().isBlank()) {
                                    rmExists = rawMaterialRepository.findByMaterialCode(request.getProductCode()).isPresent();
                                }

                                if (!rmExists) {
                                    RawMaterial rawMaterial = RawMaterial.builder()
                                                    .brand(b)
                                                    .materialName(materialName)
                                                    .materialCode(request.getProductCode())
                                                    .unitOfMeasure(request.getUnitOfMeasure() != null && !request.getUnitOfMeasure().trim().isEmpty() ? request.getUnitOfMeasure() : "NOS")
                                                    .unitCost(request.getUnitPrice() != null ? request.getUnitPrice() : 0.0)
                                                    .vatIncluded(request.getVatStatus())
                                                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                                                    .currentStock(0.0)
                                                    .minimumStockLevel(0.0)
                                                    .maxStockLevel(0.0)
                                                    .build();
                                    rawMaterialRepository.save(rawMaterial);
                                    log.info("Raw material created successfully for product: {}", request.getProductName());
                                }
                        }
                } catch (Exception e) {
                        log.warn("Secondary Oilman raw material sync skipped/failed: {}", e.getMessage());
                }

                // For the return DTO, we can re-use the map logic or just fetch simple details.
                // Since we have the ID and just saved it, let's create a map with just the
                // single production center if available (to reuse convertToDto safely)
                Map<Long, ProductionCenter> productionCenterMap = new java.util.HashMap<>();
                if (savedProduct.getProductionCenterId() != null) {
                        productionCenterRepository.findById(savedProduct.getProductionCenterId())
                                        .ifPresent(pc -> productionCenterMap.put(pc.getId(), pc));
                }

                return convertToDto(savedProduct, productionCenterMap);
        }

        @Override
        @Transactional
        public AdminProductDto updateProduct(Long id,
                        com.plover.backerymanagmentsystem.admin.dto.UpdateProductRequestDto request) {
                log.info("Updating product with ID: {}", id);

                Product product = productRepository.findById(id)
                                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));

                // Validate production center
                if (request.getProductionCenterId() != null
                                && !productionCenterRepository.existsById(request.getProductionCenterId())) {
                        throw new IllegalArgumentException(
                                        "Production Center not found with ID: " + request.getProductionCenterId());
                }

                // Handle Category
                com.plover.backerymanagmentsystem.manager.model.Category category = null;
                if (request.getCategory() != null && !request.getCategory().trim().isEmpty()) {
                        category = categoryRepository.findByName(request.getCategory())
                                        .orElseGet(() -> {
                                                log.info("Category '{}' not found, creating new one.",
                                                                request.getCategory());
                                                com.plover.backerymanagmentsystem.manager.model.Category newCategory = com.plover.backerymanagmentsystem.manager.model.Category
                                                                .builder()
                                                                .name(request.getCategory())
                                                                .description("Auto-created during product update")
                                                                .build();
                                                return categoryRepository.save(newCategory);
                                        });
                        product.setCategoryRef(category);
                }

                // Update fields with null checks to support partial updates and prevent constraint violations
                if (request.getProductName() != null)
                        product.setProductName(request.getProductName());

                if (request.getProductCode() != null)
                        product.setProductCode(request.getProductCode());

                if (request.getDescription() != null)
                        product.setDescription(request.getDescription());

                if (request.getUnitPrice() != null)
                        product.setUnitPrice(request.getUnitPrice());

                if (request.getIsActive() != null)
                        product.setIsActive(request.getIsActive());

                if (request.getProductionCenterId() != null)
                        product.setProductionCenterId(request.getProductionCenterId());

                if (request.getGbMargin() != null)
                        product.setGbMargin(request.getGbMargin());

                if (request.getVatStatus() != null)
                        product.setVatStatus(request.getVatStatus());

                if (request.getBrand() != null)
                        product.setBrand(request.getBrand());

                if (request.getSalePrice() != null)
                        product.setSalePrice(request.getSalePrice());

                if (request.getActualGP() != null)
                        product.setActualGP(request.getActualGP());

                if (request.getMaxOrderQty() != null)
                        product.setMaxOrderQty(request.getMaxOrderQty());

                if (request.getMinOrderQty() != null)
                        product.setMinOrderQty(request.getMinOrderQty());

                if (request.getUnitOfMeasure() != null)
                        product.setUnitOfMeasure(request.getUnitOfMeasure());

                if (request.getIsKotEnabled() != null)
                        product.setIsKotEnabled(request.getIsKotEnabled());

                if (request.getIsFastMoving() != null)
                        product.setIsFastMoving(request.getIsFastMoving());

                if (request.getShelfLifeDays() != null)
                        product.setShelfLifeDays(request.getShelfLifeDays());

                if (request.getProductionStageId() != null) {
                        com.plover.backerymanagmentsystem.manager.model.ProductionStage stage = productionStageRepository
                                        .findById(request.getProductionStageId())
                                        .orElseThrow(() -> new IllegalArgumentException(
                                                        "Production Stage not found with ID: "
                                                                        + request.getProductionStageId()));
                        product.setProductionStageRef(stage);
                } else {
                        product.setProductionStageRef(null);
                }

                Product savedProduct = productRepository.save(product);
                log.info("Product updated successfully with ID: {}", savedProduct.getId());

                // Trigger BOM recalculation if sale price might have changed
                if (request.getSalePrice() != null) {
                        try {
                                adminBOMService.recalculateBOMMetrics(savedProduct.getId());
                        } catch (Exception e) {
                                log.warn("Failed to recalculate BOM metrics for product {}: {}", savedProduct.getId(), e.getMessage());
                        }
                }

                // Map for DTO
                Map<Long, ProductionCenter> productionCenterMap = new java.util.HashMap<>();
                if (savedProduct.getProductionCenterId() != null) {
                        productionCenterRepository.findById(savedProduct.getProductionCenterId())
                                        .ifPresent(pc -> productionCenterMap.put(pc.getId(), pc));
                }

                return convertToDto(savedProduct, productionCenterMap);
        }

        @Override
        @Transactional
        public void deleteProduct(Long id) {
                log.info("Deleting product with ID: {}", id);
                if (!productRepository.existsById(id)) {
                        throw new IllegalArgumentException("Product not found with ID: " + id);
                }

                // Check if product is used as a parent in a BOM
                if (billOfMaterialRepository.existsByParentProductId(id)) {
                        throw new IllegalArgumentException("Cannot delete this product because it has an active Bill of Materials (BOM). Please remove its BOM first.");
                }

                // Check if product is used as a child item in any BOM
                if (billOfMaterialRepository.existsByChildItemIdAndChildType(id, BillOfMaterial.ChildType.product)) {
                        throw new IllegalArgumentException("Cannot delete this product because it is used as an ingredient/item in a Bill of Materials. Please remove it from the BOM first.");
                }

                productRepository.deleteById(id);
                log.info("Product deleted successfully with ID: {}", id);
        }

        private AdminProductDto convertToDto(Product product, Map<Long, ProductionCenter> productionCenterMap) {
                String productionCenterName = null;
                if (product.getProductionCenterId() != null) {
                        ProductionCenter pc = productionCenterMap.get(product.getProductionCenterId());
                        if (pc != null) {
                                productionCenterName = pc.getCenterName();
                        }
                }

                return AdminProductDto.builder()
                                .id(product.getId())
                                .productName(product.getProductName())
                                .productCode(product.getProductCode())
                                .description(product.getDescription())
                                .unitPrice(product.getUnitPrice())
                                .categoryName(product.getCategory())
                                .isActive(product.getIsActive())
                                .productionCenterId(product.getProductionCenterId())
                                .productionCenterName(productionCenterName)
                                .gbMargin(product.getGbMargin())
                                .vatStatus(product.getVatStatus())
                                .brand(product.getBrand())
                                .productionStageId(
                                                product.getProductionStageRef() != null
                                                                ? product.getProductionStageRef().getId()
                                                                : null)
                                .productionStageName(
                                                product.getProductionStageRef() != null
                                                                ? product.getProductionStageRef().getProductionStage()
                                                                : null)
                                .salePrice(product.getSalePrice())
                                .actualGP(product.getActualGP())
                                .maxOrderQty(product.getMaxOrderQty())
                                .minOrderQty(product.getMinOrderQty())
                                .unitOfMeasure(product.getUnitOfMeasure())
                                .isKotEnabled(product.getIsKotEnabled())
                                .isFastMoving(product.getIsFastMoving())
                                .shelfLifeDays(product.getShelfLifeDays())
                                .createdAt(product.getCreatedAt())
                                .updatedAt(product.getUpdatedAt())
                                .build();
        }

        @Override
        @Transactional
        public ProductionStageResponseDto createProductionStage(String name) {
                log.info("Admin: creating new production stage: {}", name);
                if (productionStageRepository.findByProductionStageIgnoreCase(name.trim()).isPresent()) {
                        throw new RuntimeException("Production stage already exists: " + name);
                }
                com.plover.backerymanagmentsystem.manager.model.ProductionStage stage = com.plover.backerymanagmentsystem.manager.model.ProductionStage.builder()
                                .productionStage(name.trim())
                                .build();
                com.plover.backerymanagmentsystem.manager.model.ProductionStage saved = productionStageRepository.save(stage);
                return ProductionStageResponseDto.builder()
                                .id(saved.getId())
                                .productionStage(saved.getProductionStage())
                                .build();
        }

        @Override
        @Transactional
        public ProductionStageResponseDto updateProductionStage(Integer id, String name) {
                log.info("Admin: updating production stage {} with new name: {}", id, name);
                com.plover.backerymanagmentsystem.manager.model.ProductionStage stage = productionStageRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Production stage not found with id: " + id));
                if (productionStageRepository.findByProductionStageIgnoreCase(name.trim()).isPresent() 
                                && !stage.getProductionStage().equalsIgnoreCase(name.trim())) {
                        throw new RuntimeException("Production stage already exists: " + name);
                }
                stage.setProductionStage(name.trim());
                com.plover.backerymanagmentsystem.manager.model.ProductionStage saved = productionStageRepository.save(stage);
                return ProductionStageResponseDto.builder()
                                .id(saved.getId())
                                .productionStage(saved.getProductionStage())
                                .build();
        }
}
