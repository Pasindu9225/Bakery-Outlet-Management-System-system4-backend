package com.plover.backerymanagmentsystem.admin.service;

import java.util.List;

import com.plover.backerymanagmentsystem.admin.dto.AdminProductionCenterResponse;
import com.plover.backerymanagmentsystem.admin.dto.MiniStoreResponse;

import com.plover.backerymanagmentsystem.admin.dto.CreateProductionCenterRequest;
import com.plover.backerymanagmentsystem.admin.dto.UpdateProductionCenterRequest;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenterType;

public interface AdminProductionCenterService {
    List<AdminProductionCenterResponse> getAllProductionCenters();

    List<MiniStoreResponse> getAllMiniStores();

    AdminProductionCenterResponse createProductionCenter(CreateProductionCenterRequest request);

    AdminProductionCenterResponse updateProductionCenter(Long id, UpdateProductionCenterRequest request);

    void deleteProductionCenter(Long id);

    List<AdminProductionCenterResponse> getProductionCentersByType(ProductionCenterType type);
}
