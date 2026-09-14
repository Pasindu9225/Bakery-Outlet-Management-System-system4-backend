package com.plover.backerymanagmentsystem.worker.service.impl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.manager.model.KitchenReturn;
import com.plover.backerymanagmentsystem.manager.model.KitchenReturnItem;
import com.plover.backerymanagmentsystem.manager.model.KitchenReturnStatus;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.model.TransferNote;
import com.plover.backerymanagmentsystem.manager.model.TransferNoteItem;
import com.plover.backerymanagmentsystem.manager.model.TransferNoteStatus;
import com.plover.backerymanagmentsystem.manager.repository.KitchenReturnRepository;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.manager.repository.TransferNoteRepository;
import com.plover.backerymanagmentsystem.worker.dto.CreateKitchenReturnDto;
import com.plover.backerymanagmentsystem.worker.dto.CreateTransferNoteDto;
import com.plover.backerymanagmentsystem.worker.dto.KitchenReturnDto;
import com.plover.backerymanagmentsystem.worker.dto.KitchenReturnItemDto;
import com.plover.backerymanagmentsystem.worker.dto.TransferNoteDto;
import com.plover.backerymanagmentsystem.worker.dto.TransferNoteItemDto;
import com.plover.backerymanagmentsystem.worker.service.WorkerKitchenFlowService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class WorkerKitchenFlowServiceImpl implements WorkerKitchenFlowService {

    private final TransferNoteRepository transferNoteRepository;
    private final KitchenReturnRepository kitchenReturnRepository;
    private final RawMaterialRepository rawMaterialRepository;

    // ── Transfer Notes ────────────────────────────────────────────────────────

    @Override
    public TransferNoteDto createTransferNote(CreateTransferNoteDto dto) {
        AuthModel user = currentUser();
        Long pcId = resolvePcId(user);

        if (dto.getDestinationOutletId() == null && dto.getDestinationMpcId() == null) {
            throw new RuntimeException("At least one destination (outlet or MPC) must be specified");
        }

        String transferNumber = "TN-" + System.currentTimeMillis() + "-" + pcId;

        TransferNote note = TransferNote.builder()
                .transferNumber(transferNumber)
                .sourceProductionCenterId(pcId)
                .destinationOutletId(dto.getDestinationOutletId())
                .destinationMpcId(dto.getDestinationMpcId())
                .transferredBy(user.getId())
                .status(TransferNoteStatus.SENT)
                .notes(dto.getNotes())
                .build();

        List<TransferNoteItem> items = dto.getItems().stream().map(input ->
                TransferNoteItem.builder()
                        .transferNote(note)
                        .productId(input.getProductId())
                        .productName(input.getProductName())
                        .quantity(input.getQuantity())
                        .unit(input.getUnit())
                        .build()
        ).collect(Collectors.toList());

        note.setItems(items);
        TransferNote saved = transferNoteRepository.save(note);
        log.info("Worker {} created transfer note {} for PC {}", user.getUsername(), saved.getTransferNumber(), pcId);
        return toTransferNoteDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransferNoteDto> listMyTransferNotes() {
        AuthModel user = currentUser();
        Long pcId = resolvePcId(user);
        return transferNoteRepository.findBySourceProductionCenterIdOrderByCreatedAtDesc(pcId)
                .stream().map(this::toTransferNoteDto).collect(Collectors.toList());
    }

    @Override
    public TransferNoteDto markTransferReceived(Long id) {
        TransferNote note = transferNoteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transfer note not found: " + id));
        note.setStatus(TransferNoteStatus.RECEIVED);
        note.setReceivedAt(LocalDateTime.now());
        TransferNote saved = transferNoteRepository.save(note);
        log.info("Transfer note {} marked as RECEIVED", id);
        return toTransferNoteDto(saved);
    }

    // ── Kitchen Returns ───────────────────────────────────────────────────────

    @Override
    public KitchenReturnDto createReturn(CreateKitchenReturnDto dto) {
        AuthModel user = currentUser();
        Long pcId = resolvePcId(user);

        String returnNumber = "RTN-" + System.currentTimeMillis() + "-" + pcId;

        KitchenReturn kitchenReturn = KitchenReturn.builder()
                .returnNumber(returnNumber)
                .sourceProductionCenterId(pcId)
                .returnedBy(user.getId())
                .status(KitchenReturnStatus.PENDING)
                .notes(dto.getNotes())
                .build();

        List<KitchenReturnItem> items = dto.getItems().stream().map(input -> {
            RawMaterial rm = rawMaterialRepository.findById(input.getRawMaterialId())
                    .orElseThrow(() -> new RuntimeException("Raw material not found: " + input.getRawMaterialId()));
            return KitchenReturnItem.builder()
                    .kitchenReturn(kitchenReturn)
                    .rawMaterialId(rm.getId())
                    .rawMaterialName(rm.getMaterialName())
                    .quantity(input.getQuantity())
                    .unit(rm.getUnitOfMeasure())
                    .reason(input.getReason())
                    .build();
        }).collect(Collectors.toList());

        kitchenReturn.setItems(items);
        KitchenReturn saved = kitchenReturnRepository.save(kitchenReturn);
        log.info("Worker {} created kitchen return {} for PC {}", user.getUsername(), saved.getReturnNumber(), pcId);
        return toKitchenReturnDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KitchenReturnDto> listMyReturns() {
        AuthModel user = currentUser();
        Long pcId = resolvePcId(user);
        return kitchenReturnRepository.findBySourceProductionCenterIdOrderByCreatedAtDesc(pcId)
                .stream().map(this::toKitchenReturnDto).collect(Collectors.toList());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AuthModel currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthModel)) {
            throw new RuntimeException("Not authenticated");
        }
        return (AuthModel) auth.getPrincipal();
    }

    private Long resolvePcId(AuthModel user) {
        Long pcId = user.getMpcId() != null ? user.getMpcId() : user.getProductionCenterId();
        if (pcId == null) {
            throw new RuntimeException("Worker is not assigned to a production center");
        }
        return pcId;
    }

    private TransferNoteDto toTransferNoteDto(TransferNote note) {
        List<TransferNoteItemDto> itemDtos = note.getItems() == null ? List.of() :
                note.getItems().stream().map(i -> TransferNoteItemDto.builder()
                        .id(i.getId())
                        .productId(i.getProductId())
                        .productName(i.getProductName())
                        .quantity(i.getQuantity())
                        .unit(i.getUnit())
                        .build()).collect(Collectors.toList());

        return TransferNoteDto.builder()
                .id(note.getId())
                .transferNumber(note.getTransferNumber())
                .sourceProductionCenterId(note.getSourceProductionCenterId())
                .destinationOutletId(note.getDestinationOutletId())
                .destinationMpcId(note.getDestinationMpcId())
                .status(note.getStatus() != null ? note.getStatus().name() : null)
                .notes(note.getNotes())
                .createdAt(note.getCreatedAt())
                .receivedAt(note.getReceivedAt())
                .items(itemDtos)
                .build();
    }

    private KitchenReturnDto toKitchenReturnDto(KitchenReturn kr) {
        List<KitchenReturnItemDto> itemDtos = kr.getItems() == null ? List.of() :
                kr.getItems().stream().map(i -> KitchenReturnItemDto.builder()
                        .id(i.getId())
                        .rawMaterialId(i.getRawMaterialId())
                        .rawMaterialName(i.getRawMaterialName())
                        .quantity(i.getQuantity())
                        .unit(i.getUnit())
                        .reason(i.getReason())
                        .build()).collect(Collectors.toList());

        return KitchenReturnDto.builder()
                .id(kr.getId())
                .returnNumber(kr.getReturnNumber())
                .sourceProductionCenterId(kr.getSourceProductionCenterId())
                .status(kr.getStatus() != null ? kr.getStatus().name() : null)
                .notes(kr.getNotes())
                .createdAt(kr.getCreatedAt())
                .approvedAt(kr.getApprovedAt())
                .items(itemDtos)
                .build();
    }
}
