package com.plover.backerymanagmentsystem.worker.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.plover.backerymanagmentsystem.manager.model.Outlet;
import com.plover.backerymanagmentsystem.manager.model.ProductionCenter;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;
import com.plover.backerymanagmentsystem.worker.dto.CreateMpcMaterialRequestDto;
import com.plover.backerymanagmentsystem.worker.dto.MpcMaterialRequestResponseDto;
import com.plover.backerymanagmentsystem.worker.service.WorkerMpcMaterialRequestService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkerMpcMaterialRequestServiceImpl implements WorkerMpcMaterialRequestService {

    private final ProductionCenterRepository productionCenterRepository;
    private final OutletRepository outletRepository;

    private final Map<Long, MpcMaterialRequestResponseDto> requestStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(100);

    @Override
    public MpcMaterialRequestResponseDto createRequest(CreateMpcMaterialRequestDto dto) {
        String username = getCurrentUsername();
        log.info("Creating MPC material request by user {}: {}", username, dto);

        long newId = idGenerator.incrementAndGet();
        String code = "MPC-REQ-" + newId;

        String mpcName = "MPC Center";
        if (dto.getMpcId() != null) {
            mpcName = productionCenterRepository.findById(dto.getMpcId())
                    .map(ProductionCenter::getCenterName)
                    .orElse("MPC Center #" + dto.getMpcId());
        }

        String outletName = "Outlet";
        if (dto.getOutletId() != null) {
            outletName = outletRepository.findById(dto.getOutletId())
                    .map(Outlet::getName)
                    .orElse("Outlet #" + dto.getOutletId());
        }

        List<MpcMaterialRequestResponseDto.ItemDto> items = new ArrayList<>();
        if (dto.getItems() != null) {
            for (CreateMpcMaterialRequestDto.Item item : dto.getItems()) {
                items.add(MpcMaterialRequestResponseDto.ItemDto.builder()
                        .rawMaterialId(item.getRawMaterialId())
                        .rawMaterialName(item.getRawMaterialName())
                        .requestedQty(item.getRequestedQty())
                        .issuedQty(item.getRequestedQty())
                        .unitOfMeasure(item.getUnitOfMeasure())
                        .build());
            }
        }

        MpcMaterialRequestResponseDto response = MpcMaterialRequestResponseDto.builder()
                .id(newId)
                .requestCode(code)
                .mpcName(mpcName)
                .outletName(outletName)
                .status("PENDING_MANAGER")
                .createdAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .notes(dto.getNotes())
                .items(items)
                .build();

        requestStore.put(newId, response);
        return response;
    }

    @Override
    public List<MpcMaterialRequestResponseDto> listRequests() {
        List<MpcMaterialRequestResponseDto> list = new ArrayList<>(requestStore.values());
        list.sort((a, b) -> Long.compare(b.getId(), a.getId()));
        return list;
    }

    @Override
    public MpcMaterialRequestResponseDto acceptMaterials(Long id) {
        MpcMaterialRequestResponseDto request = requestStore.get(id);
        if (request != null) {
            request.setStatus("RECEIVED");
            requestStore.put(id, request);
            return request;
        }
        throw new RuntimeException("Material request not found with ID: " + id);
    }

    @Override
    public MpcMaterialRequestResponseDto approveRequest(Long id) {
        MpcMaterialRequestResponseDto request = requestStore.get(id);
        if (request != null) {
            request.setStatus("APPROVED_MANAGER");
            requestStore.put(id, request);
            return request;
        }
        throw new RuntimeException("Material request not found with ID: " + id);
    }

    @Override
    public MpcMaterialRequestResponseDto rejectRequest(Long id) {
        MpcMaterialRequestResponseDto request = requestStore.get(id);
        if (request != null) {
            request.setStatus("CANCELLED");
            requestStore.put(id, request);
            return request;
        }
        throw new RuntimeException("Material request not found with ID: " + id);
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : "MPC Worker";
    }
}
