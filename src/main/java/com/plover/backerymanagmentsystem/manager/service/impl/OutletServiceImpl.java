package com.plover.backerymanagmentsystem.manager.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.dto.OutletDto;
import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.service.OutletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OutletServiceImpl implements OutletService {

    private final OutletRepository outletRepository;

    @Override
    public List<OutletDto> getAllOutlets() {
        log.info("Fetching all outlets for manager view");
        List<Outlet> outlets = outletRepository.findAllByOrderByNameAsc();
        return outlets.stream()
                .map(this::toDto)
                .toList();
    }

    private OutletDto toDto(Outlet outlet) {
        return OutletDto.builder()
                .outletId(outlet.getOutletId())
                .name(outlet.getName())
                .address(outlet.getAddress())
                .build();
    }
}
