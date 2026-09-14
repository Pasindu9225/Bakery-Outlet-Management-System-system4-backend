package com.plover.backerymanagmentsystem.admin.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plover.backerymanagmentsystem.admin.dto.CreateUserRequestDto;
import com.plover.backerymanagmentsystem.admin.dto.UpdateUserRequestDto;
import com.plover.backerymanagmentsystem.admin.model.BmsAuth;
import com.plover.backerymanagmentsystem.admin.repository.BmsAuthRepository;
import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.model.OutletProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenterType;
import com.plover.backerymanagmentsystem.manager.repository.OutletProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    @Mock private BmsAuthRepository bmsAuthRepository;
    @Mock private OutletRepository outletRepository;
    @Mock private ProductionCenterRepository productionCenterRepository;
    @Mock private OutletProductionCenterRepository outletProductionCenterRepository;

    @InjectMocks private AdminUserServiceImpl service;

    private CreateUserRequestDto base() {
        return CreateUserRequestDto.builder()
                .username("alice")
                .password("Password1!")
                .firstName("A")
                .lastName("L")
                .email("a@l.com")
                .phone("0700000000")
                .isActive(true)
                .build();
    }

    private void mockSave() {
        when(bmsAuthRepository.save(any(BmsAuth.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createBakeryWorker_requiresProductionCenter() {
        CreateUserRequestDto req = base();
        req.setRoleId("12");
        assertThatThrownBy(() -> service.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Production center is required");
    }

    @Test
    void createBakeryWorker_rejectsKitchenTypePC() {
        CreateUserRequestDto req = base();
        req.setRoleId("12");
        req.setProductionCenterId(7L);
        when(productionCenterRepository.findById(7L)).thenReturn(Optional.of(
                ProductionCenter.builder().id(7L).type(ProductionCenterType.KITCHEN).build()));
        assertThatThrownBy(() -> service.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("type mismatch");
    }

    @Test
    void createBakeryWorker_rejectsOutletId() {
        CreateUserRequestDto req = base();
        req.setRoleId("12");
        req.setProductionCenterId(7L);
        req.setOutletId(3L);
        assertThatThrownBy(() -> service.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Outlet must not be set");
    }

    @Test
    void createBakeryWorker_succeedsWithBakeryPC() {
        CreateUserRequestDto req = base();
        req.setRoleId("12");
        req.setProductionCenterId(7L);
        when(productionCenterRepository.findById(7L)).thenReturn(Optional.of(
                ProductionCenter.builder().id(7L).type(ProductionCenterType.BAKERY).build()));
        mockSave();
        service.createUser(req);
    }

    @Test
    void createKitchenWorker_rejectsBakeryTypePC() {
        CreateUserRequestDto req = base();
        req.setRoleId("13");
        req.setProductionCenterId(8L);
        when(productionCenterRepository.findById(8L)).thenReturn(Optional.of(
                ProductionCenter.builder().id(8L).type(ProductionCenterType.BAKERY).build()));
        assertThatThrownBy(() -> service.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("type mismatch");
    }

    @Test
    void createMpcWorker_requiresOutletAndMpc() {
        CreateUserRequestDto req = base();
        req.setRoleId("14");
        assertThatThrownBy(() -> service.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Outlet is required");
    }

    @Test
    void createMpcWorker_requiresMpc() {
        CreateUserRequestDto req = base();
        req.setRoleId("14");
        req.setOutletId(3L);
        when(outletRepository.findById(3L)).thenReturn(Optional.of(Outlet.builder().outletId(3L).build()));
        assertThatThrownBy(() -> service.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("MPC is required");
    }

    @Test
    void createMpcWorker_rejectsMpcOutsideOutlet() {
        CreateUserRequestDto req = base();
        req.setRoleId("14");
        req.setOutletId(3L);
        req.setMpcId(99L);
        when(outletRepository.findById(3L)).thenReturn(Optional.of(Outlet.builder().outletId(3L).build()));
        when(outletProductionCenterRepository.findById(99L)).thenReturn(Optional.of(
                OutletProductionCenter.builder()
                        .id(99L)
                        .outlet(Outlet.builder().outletId(4L).build())
                        .build()));
        assertThatThrownBy(() -> service.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void createMpcWorker_rejectsProductionCenterId() {
        CreateUserRequestDto req = base();
        req.setRoleId("14");
        req.setOutletId(3L);
        req.setMpcId(99L);
        req.setProductionCenterId(7L);
        when(outletRepository.findById(3L)).thenReturn(Optional.of(Outlet.builder().outletId(3L).build()));
        assertThatThrownBy(() -> service.createUser(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Production center must not be set");
    }

    @Test
    void updateBakeryWorker_requiresProductionCenter() {
        byte[] idBytes = new byte[16];
        when(bmsAuthRepository.findById(any())).thenReturn(Optional.of(BmsAuth.builder().id(idBytes).build()));
        UpdateUserRequestDto req = UpdateUserRequestDto.builder()
                .roleId("12")
                .firstName("A").lastName("L").email("a@l.com").phone("0700000000")
                .isActive(true)
                .build();
        assertThatThrownBy(() -> service.updateUser(new java.util.UUID(0L,0L).toString(), req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Production center is required");
    }
}
