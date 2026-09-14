package com.plover.backerymanagmentsystem.admin.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.plover.backerymanagmentsystem.admin.dto.GenericMaterialDto;
import com.plover.backerymanagmentsystem.admin.service.GenericMaterialService;
import com.plover.backerymanagmentsystem.manager.model.GenericMaterial;
import com.plover.backerymanagmentsystem.manager.repository.GenericMaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GenericMaterialServiceImpl implements GenericMaterialService {

    private final GenericMaterialRepository genericMaterialRepository;
    private final com.plover.backerymanagmentsystem.manager.repository.CategoryRepository categoryRepository;

    @Override
    public List<GenericMaterialDto> getAllGenericMaterials() {
        return genericMaterialRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<GenericMaterialDto> getBySubcategory(String subcategory) {
        return genericMaterialRepository.findByCategory(subcategory).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public GenericMaterialDto createGenericMaterial(GenericMaterialDto dto) {
        String uom = dto.getUnitOfMeasure();
        if (uom == null || uom.isBlank()) {
            uom = "units"; // Default UOM
        }

        String categoryName = dto.getCategory();
        // If category is null but categoryId is provided, try to find the name
        if ((categoryName == null || categoryName.isBlank()) && dto.getCategoryId() != null) {
            categoryName = categoryRepository.findById(dto.getCategoryId())
                    .map(com.plover.backerymanagmentsystem.manager.model.Category::getName)
                    .orElse(null);
        }
        
        GenericMaterial gm = GenericMaterial.builder()
                .name(dto.getName())
                .category(categoryName)
                .unitOfMeasure(uom)
                .minimumStockLevel(dto.getMinimumStockLevel())
                .maxStockLevel(dto.getMaxStockLevel())
                .description(dto.getDescription())
                .build();
        return mapToDto(genericMaterialRepository.save(gm));
    }

    @Override
    @Transactional
    public GenericMaterialDto updateGenericMaterial(Long id, GenericMaterialDto dto) {
        GenericMaterial gm = genericMaterialRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Generic material not found with id: " + id));

        if (dto.getName() != null && !dto.getName().equals(gm.getName())) {
            // Ensure new name is unique if changed
            if (genericMaterialRepository.findByName(dto.getName()).isPresent()) {
                throw new RuntimeException("Generic material already exists with name: " + dto.getName());
            }
            gm.setName(dto.getName());
        }

        if (dto.getUnitOfMeasure() != null) {
            gm.setUnitOfMeasure(dto.getUnitOfMeasure());
        }
        if (dto.getMinimumStockLevel() != null) {
            gm.setMinimumStockLevel(dto.getMinimumStockLevel());
        }
        if (dto.getMaxStockLevel() != null) {
            gm.setMaxStockLevel(dto.getMaxStockLevel());
        }
        if (dto.getCategory() != null) {
            gm.setCategory(dto.getCategory());
        }
        if (dto.getDescription() != null) {
            gm.setDescription(dto.getDescription());
        }

        return mapToDto(genericMaterialRepository.save(gm));
    }

    private GenericMaterialDto mapToDto(GenericMaterial gm) {
        return GenericMaterialDto.builder()
                .id(gm.getId())
                .name(gm.getName())
                .category(gm.getCategory())
                .unitOfMeasure(gm.getUnitOfMeasure())
                .minimumStockLevel(gm.getMinimumStockLevel())
                .maxStockLevel(gm.getMaxStockLevel())
                .description(gm.getDescription())
                .build();
    }
}
