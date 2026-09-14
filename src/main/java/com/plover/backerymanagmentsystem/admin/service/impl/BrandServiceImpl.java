package com.plover.backerymanagmentsystem.admin.service.impl;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.plover.backerymanagmentsystem.admin.dto.BrandDto;
import com.plover.backerymanagmentsystem.admin.service.BrandService;
import com.plover.backerymanagmentsystem.manager.model.Brand;
import com.plover.backerymanagmentsystem.manager.model.GenericMaterial;
import com.plover.backerymanagmentsystem.manager.repository.BrandRepository;
import com.plover.backerymanagmentsystem.manager.repository.GenericMaterialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private final GenericMaterialRepository genericMaterialRepository;

    @Override
    public List<BrandDto> getBrandsByGenericMaterial(Long genericMaterialId) {
        return brandRepository.findByGenericMaterialId(genericMaterialId).stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BrandDto createBrand(BrandDto dto) {
        GenericMaterial gm = genericMaterialRepository.findById(dto.getGenericMaterialId())
                .orElseThrow(() -> new RuntimeException("Generic material not found"));
        
        Brand brand = Brand.builder()
                .name(dto.getName())
                .genericMaterial(gm)
                .build();
        return mapToDto(brandRepository.save(brand));
    }

    @Override
    @Transactional
    public BrandDto updateBrand(Long id, BrandDto dto) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Brand not found with id: " + id));

        if (dto.getName() != null && !dto.getName().equals(brand.getName())) {
            // Check if another brand with same name exists under the same generic material
            boolean exists = brandRepository.findByNameAndGenericMaterialId(dto.getName(), brand.getGenericMaterial().getId())
                    .stream().anyMatch(b -> !b.getId().equals(id));
            if (exists) {
                throw new RuntimeException("Brand already exists with name: " + dto.getName());
            }
            brand.setName(dto.getName());
        }

        return mapToDto(brandRepository.save(brand));
    }

    private BrandDto mapToDto(Brand brand) {
        return BrandDto.builder()
                .id(brand.getId())
                .name(brand.getName())
                .genericMaterialId(brand.getGenericMaterial().getId())
                .genericMaterialName(brand.getGenericMaterial().getName())
                .build();
    }
}
