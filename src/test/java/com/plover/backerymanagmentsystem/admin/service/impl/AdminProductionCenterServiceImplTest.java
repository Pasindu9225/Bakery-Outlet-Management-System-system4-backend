package com.plover.backerymanagmentsystem.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plover.backerymanagmentsystem.admin.dto.AdminProductionCenterResponse;
import com.plover.backerymanagmentsystem.admin.dto.CreateProductionCenterRequest;
import com.plover.backerymanagmentsystem.admin.dto.UpdateProductionCenterRequest;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenterType;
import com.plover.backerymanagmentsystem.manager.repository.MiniStoreRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;

@ExtendWith(MockitoExtension.class)
class AdminProductionCenterServiceImplTest {

    @Mock private ProductionCenterRepository productionCenterRepository;
    @Mock private MiniStoreRepository miniStoreRepository;

    @InjectMocks private AdminProductionCenterServiceImpl service;

    @Test
    void createPersistsType() {
        CreateProductionCenterRequest req = CreateProductionCenterRequest.builder()
                .productionCenterName("Bread Center")
                .type(ProductionCenterType.KITCHEN)
                .isActive(true)
                .build();
        when(productionCenterRepository.save(any(ProductionCenter.class)))
                .thenAnswer(inv -> {
                    ProductionCenter pc = inv.getArgument(0);
                    pc.setId(7L);
                    return pc;
                });

        AdminProductionCenterResponse res = service.createProductionCenter(req);

        assertThat(res.getType()).isEqualTo(ProductionCenterType.KITCHEN);
    }

    @Test
    void createDefaultsToBakeryWhenTypeNull() {
        CreateProductionCenterRequest req = CreateProductionCenterRequest.builder()
                .productionCenterName("Cake Center")
                .isActive(true)
                .build();
        when(productionCenterRepository.save(any(ProductionCenter.class)))
                .thenAnswer(inv -> {
                    ProductionCenter pc = inv.getArgument(0);
                    pc.setId(8L);
                    return pc;
                });

        AdminProductionCenterResponse res = service.createProductionCenter(req);

        assertThat(res.getType()).isEqualTo(ProductionCenterType.BAKERY);
    }

    @Test
    void updateChangesType() {
        ProductionCenter existing = ProductionCenter.builder()
                .id(5L)
                .centerName("Existing")
                .type(ProductionCenterType.BAKERY)
                .isActive(true)
                .build();
        when(productionCenterRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(productionCenterRepository.save(any(ProductionCenter.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UpdateProductionCenterRequest req = UpdateProductionCenterRequest.builder()
                .productionCenterName("Existing")
                .type(ProductionCenterType.KITCHEN)
                .isActive(true)
                .build();

        AdminProductionCenterResponse res = service.updateProductionCenter(5L, req);

        assertThat(res.getType()).isEqualTo(ProductionCenterType.KITCHEN);
    }

    @Test
    void deleteThrowsWhenNotFound() {
        when(productionCenterRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.deleteProductionCenter(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }
}
