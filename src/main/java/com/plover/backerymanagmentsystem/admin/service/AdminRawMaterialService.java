package com.plover.backerymanagmentsystem.admin.service;

import com.plover.backerymanagmentsystem.admin.dto.CreateRawMaterialRequestDto;
import com.plover.backerymanagmentsystem.admin.dto.RawMaterialDto;
import com.plover.backerymanagmentsystem.admin.dto.UpdateRawMaterialRequestDto;

import java.util.List;

public interface AdminRawMaterialService {
    List<RawMaterialDto> getAllRawMaterials();

    RawMaterialDto createRawMaterial(CreateRawMaterialRequestDto request);

    RawMaterialDto updateRawMaterial(Long id, UpdateRawMaterialRequestDto request);

    void deleteRawMaterial(Long id);

    List<com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto> getAllCategories();

    List<String> getAllBrands();

    com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto createCategory(String name);

    com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto updateCategory(Integer id, String name);
}
