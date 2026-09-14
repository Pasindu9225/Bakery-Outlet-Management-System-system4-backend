package com.plover.backerymanagmentsystem.admin.service;

import java.util.List;

import com.plover.backerymanagmentsystem.admin.dto.AdminProductDto;
import com.plover.backerymanagmentsystem.admin.dto.CategoryResponseDto;
import com.plover.backerymanagmentsystem.admin.dto.ProductionStageResponseDto;

public interface AdminProductService {
    List<AdminProductDto> getAllProducts();

    List<CategoryResponseDto> getAllCategories();

    List<ProductionStageResponseDto> getAllProductionStages();

    List<com.plover.backerymanagmentsystem.admin.dto.ProductionCenterResponseDto> getAllProductionCenters();

    AdminProductDto createProduct(com.plover.backerymanagmentsystem.admin.dto.CreateProductRequestDto request);

    AdminProductDto updateProduct(Long id, com.plover.backerymanagmentsystem.admin.dto.UpdateProductRequestDto request);

    void deleteProduct(Long id);

    ProductionStageResponseDto createProductionStage(String name);

    ProductionStageResponseDto updateProductionStage(Integer id, String name);
}
