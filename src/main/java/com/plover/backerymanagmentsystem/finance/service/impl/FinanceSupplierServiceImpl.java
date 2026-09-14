package com.plover.backerymanagmentsystem.finance.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.finance.dto.SupplierForLedgerDto;
import com.plover.backerymanagmentsystem.finance.service.FinanceSupplierService;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Default implementation of {@link FinanceSupplierService}. Reads suppliers
 * from the existing {@link SupplierRepository} and projects them into a small
 * DTO with a derived 3-letter code.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class FinanceSupplierServiceImpl implements FinanceSupplierService {

    private final SupplierRepository supplierRepository;

    @Override
    public List<SupplierForLedgerDto> getAllSuppliersForLedger() {
        log.info("Finance: fetching all suppliers for ledger picker");
        List<Supplier> suppliers = supplierRepository.findAll();
        return suppliers.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private SupplierForLedgerDto toDto(Supplier s) {
        String code = deriveCode(s.getName());
        String displayId = String.format("SUP-%03d", s.getSupplierId());
        return SupplierForLedgerDto.builder()
                .id(displayId)
                .supplierId(s.getSupplierId())
                .name(s.getName())
                .code(code)
                .build();
    }

    /**
     * Derive a 3-letter uppercase code from the supplier name. Takes the first
     * letter of up to the first three whitespace-separated words. Falls back to
     * the first three letters of the name if there is only one word.
     *
     * @param name the supplier name
     * @return up to 3-character uppercase code, never {@code null}
     */
    static String deriveCode(String name) {
        if (name == null || name.isBlank()) {
            return "SUP";
        }
        String[] parts = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (sb.length() >= 3) {
                break;
            }
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0)));
            }
        }
        if (sb.length() < 3 && parts.length == 1 && parts[0].length() > 1) {
            // Single-word name: pad with subsequent letters of that word.
            String only = parts[0].toUpperCase();
            for (int i = 1; i < only.length() && sb.length() < 3; i++) {
                sb.append(only.charAt(i));
            }
        }
        return sb.toString();
    }
}
