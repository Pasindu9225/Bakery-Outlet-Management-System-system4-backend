package com.plover.backerymanagmentsystem.admin.service.impl;

import com.plover.backerymanagmentsystem.admin.dto.CreateRawMaterialRequestDto;
import com.plover.backerymanagmentsystem.admin.dto.PackDetailsDto;
import com.plover.backerymanagmentsystem.admin.dto.RawMaterialDto;
import com.plover.backerymanagmentsystem.admin.dto.UpdateRawMaterialRequestDto;
import com.plover.backerymanagmentsystem.admin.service.AdminRawMaterialService;
import com.plover.backerymanagmentsystem.manager.model.GenericMaterial;
import com.plover.backerymanagmentsystem.manager.model.Brand;
import com.plover.backerymanagmentsystem.manager.model.PackDetails;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.GenericMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.BrandRepository;
import com.plover.backerymanagmentsystem.store_keeper.model.RawMaterialSupplier;
import com.plover.backerymanagmentsystem.manager.repository.CategoryRepository;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.RawMaterialSupplierRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminRawMaterialServiceImpl implements AdminRawMaterialService {

    private final RawMaterialRepository rawMaterialRepository;
    private final RawMaterialSupplierRepository rawMaterialSupplierRepository;
    private final GenericMaterialRepository genericMaterialRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RawMaterialDto> getAllRawMaterials() {
        log.info("Admin: fetching all raw materials");

        List<RawMaterial> allMaterials = rawMaterialRepository.findAllByOrderByIdDesc();

        // Group by material code to aggregate batches
        java.util.Map<String, List<RawMaterial>> groupedByCode = allMaterials.stream()
                .filter(rm -> rm.getMaterialCode() != null)
                .collect(Collectors.groupingBy(RawMaterial::getMaterialCode));

        return groupedByCode.values().stream()
                .map(batches -> {
                    try {
                        // Use the latest/base material as the primary record for DTO mapping
                        RawMaterial baseMaterial = batches.get(0);
                        RawMaterialDto dto = toDto(baseMaterial);

                        // Calculate total stock across all batches
                        double totalStock = batches.stream()
                                .map(RawMaterial::getCurrentStock)
                                .filter(java.util.Objects::nonNull)
                                .mapToDouble(Double::doubleValue)
                                .sum();

                        dto.setCurrentStock(totalStock);
                        dto.setBatchNo(null); // Clear batch no for admin view since it's aggregated
                        return dto;
                    } catch (Exception e) {
                        log.error("Error converting aggregated raw material", e);
                        return null;
                    }
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RawMaterialDto createRawMaterial(CreateRawMaterialRequestDto request) {
        try {
            log.info("Admin: creating new raw material with name: {}", request.getName());

            // 1. Resolve Brand and GenericMaterial
            Brand brand = null;
            GenericMaterial genericMaterial = null;

            if (request.getBrandId() != null) {
                brand = brandRepository.findById(request.getBrandId())
                        .orElseThrow(() -> new RuntimeException("Brand not found with id: " + request.getBrandId()));
                genericMaterial = brand.getGenericMaterial();

                // Safety check: if the frontend also sent a genericMaterialId, verify the brand
                // actually belongs to that generic. If not, find/create a brand with the same
                // name under the correct generic material (prevents cross-generic mix-ups).
                if (request.getGenericMaterialId() != null
                        && !genericMaterial.getId().equals(request.getGenericMaterialId())) {
                    log.warn("Brand {} belongs to generic {} but request specified generic {}. Re-linking.",
                            brand.getName(), genericMaterial.getId(), request.getGenericMaterialId());
                    genericMaterial = genericMaterialRepository.findById(request.getGenericMaterialId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Generic Material not found with id: " + request.getGenericMaterialId()));
                    final String brandName = brand.getName();
                    final GenericMaterial correctGeneric = genericMaterial;
                    brand = brandRepository.findByNameAndGenericMaterialId(brandName, correctGeneric.getId())
                            .stream().findFirst()
                            .orElseGet(() -> {
                                Brand newBrand = Brand.builder()
                                        .name(brandName)
                                        .genericMaterial(correctGeneric)
                                        .build();
                                return brandRepository.save(newBrand);
                            });
                }
            } else if (request.getGenericMaterialId() != null) {
                genericMaterial = genericMaterialRepository.findById(request.getGenericMaterialId())
                        .orElseThrow(() -> new RuntimeException("Generic Material not found with id: " + request.getGenericMaterialId()));
                
                // If brandId is null, we try to find/create a default brand for this generic
                String brandName = request.getBrand();
                if (brandName == null || brandName.isBlank()) {
                    brandName = genericMaterial.getName(); // Default brand name to generic name if missing
                }

                final GenericMaterial finalGenericMaterial = genericMaterial;
                final String finalBrandName = brandName;
                brand = brandRepository.findByNameAndGenericMaterialId(brandName, genericMaterial.getId())
                        .stream().findFirst()
                        .orElseGet(() -> {
                            Brand newBrand = Brand.builder()
                                    .name(finalBrandName)
                                    .genericMaterial(finalGenericMaterial)
                                    .build();
                            return brandRepository.save(newBrand);
                        });
            }

            if (brand == null) {
                throw new RuntimeException("Final Brand resolution failed - brand is null");
            }

            // 3. Create raw material linked to Brand
            RawMaterial rawMaterial = RawMaterial.builder()
                    .brand(brand)
                    .materialName(request.getMaterialName())
                    .materialCode(request.getMaterialCode())
                    .unitOfMeasure(request.getUnitOfMeasure())
                    .maxStockLevel(request.getMaxStockLevel() != null ? request.getMaxStockLevel() : 0.0)
                    .minimumStockLevel(request.getMinimumStockLevel() != null ? request.getMinimumStockLevel() : 0.0)
                    .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                    .vatIncluded(request.getVatIncluded() != null ? request.getVatIncluded() : false)
                    .unitCost(request.getUnitCost() != null ? request.getUnitCost() : 0.0)
                    .currentStock(0.0)
                    .build();

            // Add pack details if unit of measure is 'pack' and details are provided
            if (request.getUnitOfMeasure() != null && request.getUnitOfMeasure().equalsIgnoreCase("pack")
                    && request.getPackDetails() != null && !request.getPackDetails().isEmpty()) {
                List<PackDetails> packDetailsList = request.getPackDetails().stream()
                        .map(dto -> PackDetails.builder()
                                .packUom(dto.getPackUom())
                                .packSize(dto.getPackSize())
                                .rawMaterial(rawMaterial)
                                .build())
                        .collect(Collectors.toList());
                rawMaterial.setPackDetails(packDetailsList);
            }

            // Save raw material
            RawMaterial savedRawMaterial = rawMaterialRepository.save(rawMaterial);
            log.info("Created raw material with id: {}", savedRawMaterial.getId());

            // 4. Handle supplier assignment if provided
            if (request.getSupplierId() != null) {
                Supplier supplier = supplierRepository.findById(request.getSupplierId())
                        .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + request.getSupplierId()));
                
                RawMaterialSupplier rms = RawMaterialSupplier.builder()
                        .rawMaterialId(savedRawMaterial.getId())
                        .supplierId(supplier.getSupplierId())
                        .negotiatedUnitCost(savedRawMaterial.getUnitCost())
                        .isPreferred(true)
                        .build();
                rawMaterialSupplierRepository.save(rms);
                log.info("Linked raw material {} to supplier {}", savedRawMaterial.getId(), supplier.getSupplierId());
            }

            // Return created raw material details
            return toDto(savedRawMaterial);
        } catch (Exception e) {
            log.error("Error creating raw material: ", e);
            throw e;
        }
    }

    @Override
    @Transactional
    public RawMaterialDto updateRawMaterial(Long id, UpdateRawMaterialRequestDto request) {
        log.info("Admin: updating raw material with id: {}", id);

        // Find existing raw material to get its materialCode
        RawMaterial baseRawMaterial = rawMaterialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Raw material not found with id: " + id));

        String materialCode = baseRawMaterial.getMaterialCode();
        if (materialCode == null) {
            throw new RuntimeException("Raw material has no material code");
        }

        // Fetch all batches for this material code
        List<RawMaterial> allBatches = rawMaterialRepository.findAllByMaterialCode(materialCode);
        RawMaterial updatedBaseRawMaterial = null;

        for (RawMaterial rawMaterial : allBatches) {
            // Resolve target Brand without mutating old unrelated generic materials
            Brand targetBrand = null;

            if (request.getBrandId() != null) {
                targetBrand = brandRepository.findById(request.getBrandId())
                        .orElseThrow(() -> new RuntimeException("Brand not found with id: " + request.getBrandId()));

                // Safety check: if genericMaterialId was also sent, verify the brand
                // belongs to that generic. If not, find/create a brand with the same
                // name under the correct generic (prevents cross-generic mix-ups).
                if (request.getGenericMaterialId() != null
                        && targetBrand.getGenericMaterial() != null
                        && !targetBrand.getGenericMaterial().getId().equals(request.getGenericMaterialId())) {
                    log.warn("Update: Brand {} belongs to generic {} but request specified generic {}. Re-linking.",
                            targetBrand.getName(), targetBrand.getGenericMaterial().getId(), request.getGenericMaterialId());
                    GenericMaterial correctGeneric = genericMaterialRepository.findById(request.getGenericMaterialId())
                            .orElseThrow(() -> new RuntimeException(
                                    "Generic Material not found with id: " + request.getGenericMaterialId()));
                    final String brandName = targetBrand.getName();
                    final GenericMaterial finalCorrectGeneric = correctGeneric;
                    targetBrand = brandRepository.findByNameAndGenericMaterialId(brandName, correctGeneric.getId())
                            .stream().findFirst()
                            .orElseGet(() -> {
                                Brand b = Brand.builder()
                                        .name(brandName)
                                        .genericMaterial(finalCorrectGeneric)
                                        .build();
                                return brandRepository.save(b);
                            });
                }
            } else if (request.getGenericMaterialId() != null) {
                GenericMaterial genericMaterial = genericMaterialRepository.findById(request.getGenericMaterialId())
                        .orElseThrow(() -> new RuntimeException("Generic Material not found with id: " + request.getGenericMaterialId()));

                String brandName = request.getBrand();
                if (brandName == null || brandName.isBlank()) {
                    brandName = rawMaterial.getBrand() != null ? rawMaterial.getBrand().getName() : genericMaterial.getName();
                }
                final String finalBrandName = brandName;
                final GenericMaterial finalGeneric = genericMaterial;
                targetBrand = brandRepository.findByNameAndGenericMaterialId(finalBrandName, genericMaterial.getId())
                        .stream().findFirst()
                        .orElseGet(() -> {
                            Brand b = Brand.builder()
                                    .name(finalBrandName)
                                    .genericMaterial(finalGeneric)
                                    .build();
                            return brandRepository.save(b);
                        });
            } else if (rawMaterial.getBrand() != null) {
                GenericMaterial genericMaterial = rawMaterial.getBrand().getGenericMaterial();

                if (request.getBrand() != null && !request.getBrand().isBlank() && !request.getBrand().equals(rawMaterial.getBrandName())) {
                    final GenericMaterial finalGeneric = genericMaterial;
                    targetBrand = brandRepository.findByNameAndGenericMaterialId(request.getBrand(), genericMaterial.getId())
                            .stream().findFirst()
                            .orElseGet(() -> {
                                Brand b = Brand.builder()
                                        .name(request.getBrand())
                                        .genericMaterial(finalGeneric)
                                        .build();
                                return brandRepository.save(b);
                            });
                }
            }

            if (targetBrand != null) {
                rawMaterial.setBrand(targetBrand);
            }

            if (request.getMaterialName() != null) {
                rawMaterial.setMaterialName(request.getMaterialName());
            }

            if (request.getUnitOfMeasure() != null) {
                rawMaterial.setUnitOfMeasure(request.getUnitOfMeasure());
            }

            if (request.getMaterialCode() != null)
                rawMaterial.setMaterialCode(request.getMaterialCode());
            if (request.getMaxStockLevel() != null)
                rawMaterial.setMaxStockLevel(request.getMaxStockLevel());
            if (request.getMinimumStockLevel() != null)
                rawMaterial.setMinimumStockLevel(request.getMinimumStockLevel());
            if (request.getIsActive() != null)
                rawMaterial.setIsActive(request.getIsActive());
            if (request.getVatIncluded() != null)
                rawMaterial.setVatIncluded(request.getVatIncluded());
            if (request.getUnitCost() != null)
                rawMaterial.setUnitCost(request.getUnitCost());

            // Handle pack details update
            if (rawMaterial.getUnitOfMeasure() != null && rawMaterial.getUnitOfMeasure().equalsIgnoreCase("pack")) {
                if (request.getPackDetails() != null) {
                    // Initialize or clear the list to maintain Hibernate's tracking
                    if (rawMaterial.getPackDetails() == null) {
                        rawMaterial.setPackDetails(new ArrayList<>());
                    } else {
                        rawMaterial.getPackDetails().clear();
                    }

                    // Map and add new details
                    List<PackDetails> newPackDetails = request.getPackDetails().stream()
                            .map(dto -> PackDetails.builder()
                                    .packUom(dto.getPackUom())
                                    .packSize(dto.getPackSize())
                                    .rawMaterial(rawMaterial)
                                    .build())
                            .collect(Collectors.toList());

                    rawMaterial.getPackDetails().addAll(newPackDetails);
                }
            } else {
                // If UOM is no longer 'pack', clear any existing pack details
                if (rawMaterial.getPackDetails() != null) {
                    rawMaterial.getPackDetails().clear();
                }
            }

            RawMaterial savedMaterial = rawMaterialRepository.save(rawMaterial);
            if (rawMaterial.getId().equals(id)) {
                updatedBaseRawMaterial = savedMaterial;
            }
        }
        rawMaterialRepository.flush();
        log.debug("Updated {} raw material batches for material code: {}", allBatches.size(), materialCode);


        // Handle supplier update if provided
        if (request.getSupplierId() != null) {
            // Remove existing relationships first to maintain "Primary" supplier logic
            // Update this for ALL batches of this material
            for (RawMaterial rm : allBatches) {
                List<RawMaterialSupplier> existingLinks = rawMaterialSupplierRepository.findByRawMaterialId(rm.getId());
                if (!existingLinks.isEmpty()) {
                    rawMaterialSupplierRepository.deleteAll(existingLinks);
                }

                Supplier supplier = supplierRepository.findById(request.getSupplierId())
                        .orElseThrow(() -> new RuntimeException("Supplier not found with id: " + request.getSupplierId()));

                RawMaterialSupplier rms = RawMaterialSupplier.builder()
                        .rawMaterialId(rm.getId())
                        .supplierId(supplier.getSupplierId())
                        .negotiatedUnitCost(rm.getUnitCost())
                        .isPreferred(true)
                        .build();
                rawMaterialSupplierRepository.save(rms);
            }
        }

        // Return updated raw material details
        RawMaterialDto finalDto = toDto(updatedBaseRawMaterial != null ? updatedBaseRawMaterial : allBatches.get(0));
        
        // Sum stock for the response DTO
        double totalStock = allBatches.stream()
                .map(RawMaterial::getCurrentStock)
                .filter(java.util.Objects::nonNull)
                .mapToDouble(Double::doubleValue)
                .sum();
        finalDto.setCurrentStock(totalStock);
        finalDto.setBatchNo(null);
        
        return finalDto;
    }

    @Override
    @Transactional
    public void deleteRawMaterial(Long id) {
        log.info("Admin: deleting raw material with id: {}", id);

        // Find existing raw material
        Optional<RawMaterial> rawMaterialOpt = rawMaterialRepository.findById(id);
        if (rawMaterialOpt.isEmpty()) {
            throw new RuntimeException("Raw material not found with id: " + id);
        }
        
        String materialCode = rawMaterialOpt.get().getMaterialCode();
        if (materialCode == null) {
            throw new RuntimeException("Raw material has no material code");
        }

        List<RawMaterial> allBatches = rawMaterialRepository.findAllByMaterialCode(materialCode);

        for (RawMaterial rm : allBatches) {
            // Delete all related raw material suppliers first
            List<RawMaterialSupplier> rawMaterialSuppliers = rawMaterialSupplierRepository.findByRawMaterialId(rm.getId());
            if (!rawMaterialSuppliers.isEmpty()) {
                rawMaterialSupplierRepository.deleteAll(rawMaterialSuppliers);
                log.debug("Deleted {} raw material supplier relationships for material {}", rawMaterialSuppliers.size(), rm.getId());
            }

            // Delete the raw material batch
            rawMaterialRepository.deleteById(rm.getId());
            log.debug("Deleted raw material batch with id: {}", rm.getId());
        }
    }

    @Override
    @Transactional
    public List<com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto> getAllCategories() {
        log.info("Admin: fetching and seeding raw material categories");
        
        List<String> standardCategories = List.of("INGREDIENT", "PACKAGING", "EQUIPMENT", "GENERAL");
        
        // Ensure standard categories exist in the database
        for (String catName : standardCategories) {
            categoryRepository.findByName(catName).orElseGet(() -> {
                log.info("Seeding category: {}", catName);
                return categoryRepository.save(com.plover.backerymanagmentsystem.manager.model.Category.builder()
                        .name(catName)
                        .description("Standard material category")
                        .build());
            });
        }

        // Return all categories as DTOs
        return categoryRepository.findAll().stream()
                .map(category -> com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto.builder()
                        .id(category.getId())
                        .name(category.getName())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllBrands() {
        log.info("Admin: fetching all raw material brands");
        return rawMaterialRepository.findDistinctBrands();
    }

    private RawMaterialDto toDto(RawMaterial rawMaterial) {
        log.info("toDto mapping - RawMaterial ID: {}, name: {}, genericMaterialName: {}, category: {}", 
                 rawMaterial.getId(), rawMaterial.getMaterialName(), rawMaterial.getGenericMaterialName(), rawMaterial.getCategory());
        String displayUnitOfMeasure = rawMaterial.getUnitOfMeasure();

        // Logic: if unit of measure is 'pack', check pack details for pack UOM
        if (displayUnitOfMeasure != null && displayUnitOfMeasure.equalsIgnoreCase("pack")) {
            if (rawMaterial.getPackDetails() != null && !rawMaterial.getPackDetails().isEmpty()) {
                displayUnitOfMeasure = rawMaterial.getPackDetails().get(0).getPackUom();
            }
        }

        RawMaterialDto dto = RawMaterialDto.builder()
                .id(rawMaterial.getId())
                .brand(rawMaterial.getBrandName())
                .category(rawMaterial.getCategory())
                .materialName(rawMaterial.getMaterialName())
                .unitOfMeasure(displayUnitOfMeasure)
                .description(rawMaterial.getDescription())
                .expireDate(rawMaterial.getExpireDate())
                .isActive(rawMaterial.getIsActive())
                .materialCode(rawMaterial.getMaterialCode())
                .createdAt(rawMaterial.getCreatedAt())
                .currentStock(rawMaterial.getCurrentStock())
                .batchNo(rawMaterial.getBatchNo())
                .minimumStockLevel(rawMaterial.getMinimumStockLevel())
                .unitCost(rawMaterial.getUnitCost())
                .brand(rawMaterial.getBrandName())
                .maxStockLevel(rawMaterial.getMaxStockLevel())
                .vatIncluded(rawMaterial.getVatIncluded())
                .genericMaterialId(rawMaterial.getBrand() != null && rawMaterial.getBrand().getGenericMaterial() != null ? rawMaterial.getBrand().getGenericMaterial().getId() : null)
                .brandId(rawMaterial.getBrand() != null ? rawMaterial.getBrand().getId() : null)
                .genericMaterialName(rawMaterial.getGenericMaterialName())
                .packDetails(rawMaterial.getUnitOfMeasure() != null
                        && rawMaterial.getUnitOfMeasure().equalsIgnoreCase("pack")
                        && rawMaterial.getPackDetails() != null
                                ? rawMaterial.getPackDetails().stream()
                                        .map(pd -> PackDetailsDto.builder()
                                                .id(pd.getId())
                                                .packUom(pd.getPackUom())
                                                .packSize(pd.getPackSize())
                                                .build())
                                        .collect(Collectors.toList())
                                : null)
                .build();

        // Populate supplier info
        try {
            List<RawMaterialSupplier> suppliers = rawMaterialSupplierRepository.findByRawMaterialIdWithDetails(rawMaterial.getId());
            if (!suppliers.isEmpty()) {
                RawMaterialSupplier primary = suppliers.get(0); // Preferred first due to ORDER BY
                dto.setSupplierId(primary.getSupplierId());
                dto.setSupplierName(primary.getSupplier() != null ? primary.getSupplier().getName() : null);
            }
        } catch (Exception e) {
            log.error("Error fetching supplier details for raw material: " + rawMaterial.getId(), e);
        }

        return dto;
    }
    @Override
    @Transactional
    public com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto createCategory(String name) {
        log.info("Admin: creating new category: {}", name);
        
        if (categoryRepository.findByName(name.toUpperCase()).isPresent()) {
            throw new RuntimeException("Category already exists: " + name);
        }

        com.plover.backerymanagmentsystem.manager.model.Category category = com.plover.backerymanagmentsystem.manager.model.Category.builder()
                .name(name.toUpperCase())
                .description("Custom material category")
                .build();
        
        com.plover.backerymanagmentsystem.manager.model.Category saved = categoryRepository.save(category);
        
        return com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .build();
    }

    @Override
    @Transactional
    public com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto updateCategory(Integer id, String name) {
        log.info("Admin: updating category {} with new name: {}", id, name);
        
        com.plover.backerymanagmentsystem.manager.model.Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Category not found with id: " + id));

        String oldName = category.getName();
        String newName = name.toUpperCase();

        if (categoryRepository.findByName(newName).isPresent() && !oldName.equals(newName)) {
            throw new RuntimeException("Category already exists: " + newName);
        }

        category.setName(newName);
        com.plover.backerymanagmentsystem.manager.model.Category saved = categoryRepository.save(category);
        
        // Update generic materials that use this category name
        List<GenericMaterial> generics = genericMaterialRepository.findByCategory(oldName);
        if (generics != null && !generics.isEmpty()) {
            generics.forEach(g -> g.setCategory(newName));
            genericMaterialRepository.saveAll(generics);
        }

        return com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto.builder()
                .id(saved.getId())
                .name(saved.getName())
                .build();
    }
}
