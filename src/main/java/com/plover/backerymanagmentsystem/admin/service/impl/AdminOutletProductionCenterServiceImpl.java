package com.plover.backerymanagmentsystem.admin.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.admin.dto.CreateOutletProductionCenterRequest;
import com.plover.backerymanagmentsystem.admin.dto.OutletProductionCenterResponse;
import com.plover.backerymanagmentsystem.admin.dto.UpdateOutletProductionCenterRequest;
import com.plover.backerymanagmentsystem.admin.service.AdminOutletProductionCenterService;
import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.model.OutletProductionCenter;
import com.plover.backerymanagmentsystem.manager.repository.OutletProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionOrderItemRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOutletProductionCenterServiceImpl implements AdminOutletProductionCenterService {

    private final OutletProductionCenterRepository mpcRepository;
    private final OutletRepository outletRepository;
    private final ProductionOrderItemRepository productionOrderItemRepository;

    @Transactional
    @Override
    public OutletProductionCenterResponse create(Long outletId, CreateOutletProductionCenterRequest request) {
        log.info("Creating MPC '{}' for outlet {}", request.getName(), outletId);

        Outlet outlet = outletRepository.findById(outletId)
                .orElseThrow(() -> new RuntimeException("Outlet not found with id: " + outletId));

        if (mpcRepository.existsByOutlet_OutletIdAndNameIgnoreCase(outletId, request.getName())) {
            throw new RuntimeException("An MPC with this name already exists in this outlet");
        }

        OutletProductionCenter mpc = OutletProductionCenter.builder()
                .outlet(outlet)
                .name(request.getName())
                .isActive(request.getIsActive() == null ? Boolean.TRUE : request.getIsActive())
                .build();

        OutletProductionCenter saved = mpcRepository.save(mpc);
        return toResponse(saved);
    }

    @Override
    public List<OutletProductionCenterResponse> listForOutlet(Long outletId) {
        return mpcRepository.findByOutlet_OutletId(outletId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OutletProductionCenterResponse update(Long id, UpdateOutletProductionCenterRequest request) {
        OutletProductionCenter mpc = mpcRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Production center not found with id: " + id));

        if (request.getName() != null && !request.getName().isBlank()) {
            mpc.setName(request.getName());
        }
        if (request.getIsActive() != null) {
            mpc.setIsActive(request.getIsActive());
        }
        return toResponse(mpcRepository.save(mpc));
    }

    @Transactional
    @Override
    public void delete(Long id) {
        OutletProductionCenter mpc = mpcRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Production center not found with id: " + id));

        long refs = productionOrderItemRepository.countByProductionCenterId(id);
        if (refs > 0) {
            log.info("MPC {} has {} KOT references - soft deactivating", id, refs);
            mpc.setIsActive(false);
            mpcRepository.save(mpc);
        } else {
            log.info("MPC {} has no KOT references - hard deleting", id);
            mpcRepository.delete(mpc);
        }
    }

    private OutletProductionCenterResponse toResponse(OutletProductionCenter mpc) {
        return OutletProductionCenterResponse.builder()
                .id(mpc.getId())
                .outletId(mpc.getOutlet().getOutletId())
                .name(mpc.getName())
                .isActive(mpc.getIsActive())
                .createdAt(mpc.getCreatedAt())
                .updatedAt(mpc.getUpdatedAt())
                .build();
    }
}
