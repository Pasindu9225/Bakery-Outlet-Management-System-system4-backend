package com.plover.backerymanagmentsystem.admin.service;

import java.util.List;
import com.plover.backerymanagmentsystem.admin.dto.GenericMaterialDto;

public interface GenericMaterialService {
    List<GenericMaterialDto> getAllGenericMaterials();
    List<GenericMaterialDto> getBySubcategory(String subcategory);
    GenericMaterialDto createGenericMaterial(GenericMaterialDto dto);
    GenericMaterialDto updateGenericMaterial(Long id, GenericMaterialDto dto);
}
