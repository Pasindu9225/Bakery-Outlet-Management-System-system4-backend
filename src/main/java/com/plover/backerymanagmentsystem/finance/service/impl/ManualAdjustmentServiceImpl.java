package com.plover.backerymanagmentsystem.finance.service.impl;

import java.time.LocalDate;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.finance.dto.CreateManualAdjustmentRequestDto;
import com.plover.backerymanagmentsystem.finance.dto.ManualAdjustmentResponseDto;
import com.plover.backerymanagmentsystem.finance.model.ManualLedgerAdjustment;
import com.plover.backerymanagmentsystem.finance.repository.ManualLedgerAdjustmentRepository;
import com.plover.backerymanagmentsystem.finance.service.ManualAdjustmentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link ManualAdjustmentService}. Generates a
 * sequence-based reference like {@code ADJ-2026-001} per calendar year.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ManualAdjustmentServiceImpl implements ManualAdjustmentService {

    private final ManualLedgerAdjustmentRepository repository;

    @Override
    @Transactional
    public ManualAdjustmentResponseDto createAdjustment(CreateManualAdjustmentRequestDto request) {
        if (request.getRemarks() == null || request.getRemarks().isBlank()) {
            throw new IllegalArgumentException("Remarks are mandatory for manual ledger adjustments");
        }
        LocalDate date = request.getDate() != null ? request.getDate() : LocalDate.now();

        String ref = generateRef(date.getYear());

        ManualLedgerAdjustment entity = ManualLedgerAdjustment.builder()
                .adjustmentRef(ref)
                .adjustmentDate(date)
                .supplierId(request.getSupplierId())
                .amount(request.getAmount())
                .description(request.getDescription())
                .remarks(request.getRemarks())
                .build();

        ManualLedgerAdjustment saved = repository.save(entity);
        log.info("Created manual ledger adjustment {} for supplier {}", saved.getAdjustmentRef(), saved.getSupplierId());

        return ManualAdjustmentResponseDto.builder()
                .adjustmentId(saved.getAdjustmentId())
                .adjustmentRef(saved.getAdjustmentRef())
                .supplierId(saved.getSupplierId())
                .date(saved.getAdjustmentDate())
                .amount(saved.getAmount())
                .description(saved.getDescription())
                .remarks(saved.getRemarks())
                .build();
    }

    /**
     * Generate the next sequential reference for the given year using a count
     * of existing entries plus one. Format: {@code ADJ-YYYY-NNN}.
     *
     * @param year calendar year
     * @return the next reference string
     */
    private String generateRef(int year) {
        String prefix = String.format("ADJ-%d-", year);
        long existing = repository.countByAdjustmentRefPrefix(prefix);
        return String.format("%s%03d", prefix, existing + 1);
    }
}
