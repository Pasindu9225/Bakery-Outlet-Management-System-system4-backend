package com.plover.backerymanagmentsystem.admin.service;

import java.util.List;
import com.plover.backerymanagmentsystem.admin.dto.BrandDto;

public interface BrandService {
    List<BrandDto> getBrandsByGenericMaterial(Long genericMaterialId);
    BrandDto createBrand(BrandDto dto);
    BrandDto updateBrand(Long id, BrandDto dto);
}
