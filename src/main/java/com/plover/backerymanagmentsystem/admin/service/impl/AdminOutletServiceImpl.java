package com.plover.backerymanagmentsystem.admin.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.admin.repository.BmsAuthRepository;
import com.plover.backerymanagmentsystem.admin.service.AdminOutletService;
import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.repository.OutletProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.plover.backerymanagmentsystem.admin.dto.CreateOutletRequest;
import com.plover.backerymanagmentsystem.admin.dto.UpdateOutletRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOutletServiceImpl implements AdminOutletService {

    private final OutletRepository outletRepository;
    private final OutletProductionCenterRepository outletProductionCenterRepository;
    private final BmsAuthRepository bmsAuthRepository;

    @Override
    public List<Outlet> getAllOutlets() {
        log.info("Fetching all outlets");
        return outletRepository.findAll();
    }

    @Override
    public Outlet createOutlet(CreateOutletRequest request) {
        log.info("Creating new outlet: {}", request.getName());
        Outlet outlet = Outlet.builder()
                .name(request.getName())
                .location(request.getLocation())
                .mainBranch(request.getMainBranch())
                .address(request.getAddress())
                .status(request.getStatus())
                .build();
        return outletRepository.save(outlet);
    }

    @Override
    public Outlet updateOutlet(Long id, UpdateOutletRequest request) {
        log.info("Updating outlet: {}", id);
        Outlet outlet = outletRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Outlet not found with id: " + id));

        outlet.setName(request.getName());
        outlet.setLocation(request.getLocation());
        outlet.setMainBranch(request.getMainBranch());
        outlet.setAddress(request.getAddress());
        outlet.setStatus(request.getStatus());

        return outletRepository.save(outlet);
    }

    @Transactional
    @Override
    public void deleteOutlet(Long id) {
        log.info("Deleting outlet: {}", id);
        if (!outletRepository.existsById(id)) {
            throw new RuntimeException("Outlet not found with id: " + id);
        }
        long mpcs = outletProductionCenterRepository.countByOutlet_OutletId(id);
        long cashiers = bmsAuthRepository.countByOutletId(id);
        if (mpcs > 0 || cashiers > 0) {
            throw new RuntimeException(
                "Outlet has " + mpcs + " production centers and " + cashiers
                + " users; remove or reassign first");
        }
        outletRepository.deleteById(id);
    }
}
