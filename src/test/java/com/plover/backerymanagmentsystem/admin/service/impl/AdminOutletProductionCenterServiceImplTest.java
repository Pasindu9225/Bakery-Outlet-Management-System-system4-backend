package com.plover.backerymanagmentsystem.admin.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plover.backerymanagmentsystem.admin.dto.CreateOutletProductionCenterRequest;
import com.plover.backerymanagmentsystem.admin.dto.OutletProductionCenterResponse;
import com.plover.backerymanagmentsystem.admin.dto.UpdateOutletProductionCenterRequest;
import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.model.OutletProductionCenter;
import com.plover.backerymanagmentsystem.manager.repository.OutletProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionOrderItemRepository;

@ExtendWith(MockitoExtension.class)
public class AdminOutletProductionCenterServiceImplTest {

    @Mock
    private OutletProductionCenterRepository mpcRepository;

    @Mock
    private OutletRepository outletRepository;

    @Mock
    private ProductionOrderItemRepository productionOrderItemRepository;

    @InjectMocks
    private AdminOutletProductionCenterServiceImpl service;

    private Outlet outlet() {
        return Outlet.builder().outletId(1L).name("Main").build();
    }

    @Test
    void create_succeeds_with_valid_outlet() {
        when(outletRepository.findById(1L)).thenReturn(Optional.of(outlet()));
        when(mpcRepository.existsByOutlet_OutletIdAndNameIgnoreCase(1L, "Hot Kitchen")).thenReturn(false);
        when(mpcRepository.save(any(OutletProductionCenter.class)))
                .thenAnswer(inv -> {
                    OutletProductionCenter mpc = inv.getArgument(0);
                    mpc.setId(99L);
                    return mpc;
                });

        OutletProductionCenterResponse resp = service.create(1L,
                CreateOutletProductionCenterRequest.builder().name("Hot Kitchen").isActive(true).build());

        assertEquals(99L, resp.getId());
        assertEquals(1L, resp.getOutletId());
        assertEquals("Hot Kitchen", resp.getName());
        assertTrue(resp.getIsActive());
    }

    @Test
    void create_fails_when_outlet_missing() {
        when(outletRepository.findById(42L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.create(42L, CreateOutletProductionCenterRequest.builder().name("X").build()));

        assertTrue(ex.getMessage().toLowerCase().contains("outlet"));
        verify(mpcRepository, never()).save(any());
    }

    @Test
    void create_fails_on_duplicate_name() {
        when(outletRepository.findById(1L)).thenReturn(Optional.of(outlet()));
        when(mpcRepository.existsByOutlet_OutletIdAndNameIgnoreCase(1L, "Bar")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.create(1L, CreateOutletProductionCenterRequest.builder().name("Bar").build()));

        assertTrue(ex.getMessage().toLowerCase().contains("already exists"));
        verify(mpcRepository, never()).save(any());
    }

    @Test
    void listForOutlet_returns_mapped_list() {
        Outlet o = outlet();
        when(mpcRepository.findByOutlet_OutletId(1L)).thenReturn(List.of(
                OutletProductionCenter.builder().id(10L).outlet(o).name("A").isActive(true).build(),
                OutletProductionCenter.builder().id(11L).outlet(o).name("B").isActive(false).build()
        ));

        List<OutletProductionCenterResponse> resp = service.listForOutlet(1L);

        assertEquals(2, resp.size());
        assertEquals("A", resp.get(0).getName());
        assertEquals(false, resp.get(1).getIsActive());
    }

    @Test
    void update_renames_and_toggles_active() {
        Outlet o = outlet();
        OutletProductionCenter existing = OutletProductionCenter.builder()
                .id(10L).outlet(o).name("Old").isActive(true).build();
        when(mpcRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(mpcRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        OutletProductionCenterResponse resp = service.update(10L,
                UpdateOutletProductionCenterRequest.builder().name("New").isActive(false).build());

        assertEquals("New", resp.getName());
        assertFalse(resp.getIsActive());
    }

    @Test
    void delete_soft_deactivates_when_kot_history_exists() {
        Outlet o = outlet();
        OutletProductionCenter mpc = OutletProductionCenter.builder()
                .id(10L).outlet(o).name("X").isActive(true).build();
        when(mpcRepository.findById(10L)).thenReturn(Optional.of(mpc));
        when(productionOrderItemRepository.countByProductionCenterId(10L)).thenReturn(3L);

        service.delete(10L);

        assertFalse(mpc.getIsActive());
        verify(mpcRepository).save(mpc);
        verify(mpcRepository, never()).delete(any());
    }

    @Test
    void delete_hard_deletes_when_no_kot_history() {
        Outlet o = outlet();
        OutletProductionCenter mpc = OutletProductionCenter.builder()
                .id(10L).outlet(o).name("X").isActive(true).build();
        when(mpcRepository.findById(10L)).thenReturn(Optional.of(mpc));
        when(productionOrderItemRepository.countByProductionCenterId(10L)).thenReturn(0L);

        service.delete(10L);

        verify(mpcRepository).delete(mpc);
        verify(mpcRepository, never()).save(any());
    }

    @Test
    void update_fails_when_mpc_not_found() {
        when(mpcRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.update(999L, UpdateOutletProductionCenterRequest.builder().name("X").build()));

        assertTrue(ex.getMessage().toLowerCase().contains("not found"));
        verify(mpcRepository, never()).save(any());
    }

    @Test
    void delete_fails_when_mpc_not_found() {
        when(mpcRepository.findById(999L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                service.delete(999L));

        assertTrue(ex.getMessage().toLowerCase().contains("not found"));
        verify(mpcRepository, never()).save(any());
        verify(mpcRepository, never()).delete(any());
    }
}
