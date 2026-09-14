package com.plover.backerymanagmentsystem.finance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plover.backerymanagmentsystem.finance.dto.CreateManualAdjustmentRequestDto;
import com.plover.backerymanagmentsystem.finance.dto.ManualAdjustmentResponseDto;
import com.plover.backerymanagmentsystem.finance.model.ManualLedgerAdjustment;
import com.plover.backerymanagmentsystem.finance.repository.ManualLedgerAdjustmentRepository;

@ExtendWith(MockitoExtension.class)
class ManualAdjustmentServiceImplTest {

    @Mock private ManualLedgerAdjustmentRepository repository;

    @InjectMocks private ManualAdjustmentServiceImpl service;

    private CreateManualAdjustmentRequestDto base() {
        return CreateManualAdjustmentRequestDto.builder()
                .supplierId(1L)
                .date(LocalDate.of(2026, 5, 1))
                .amount(new BigDecimal("100.00"))
                .description("Test")
                .remarks("Approved adjustment")
                .build();
    }

    @Test
    void createAdjustment_persistsAndGeneratesRef() {
        when(repository.countByAdjustmentRefPrefix(anyString())).thenReturn(0L);
        when(repository.save(any(ManualLedgerAdjustment.class)))
                .thenAnswer(inv -> {
                    ManualLedgerAdjustment a = inv.getArgument(0);
                    a.setAdjustmentId(7L);
                    return a;
                });

        ManualAdjustmentResponseDto response = service.createAdjustment(base());

        assertThat(response.getAdjustmentRef()).isEqualTo("ADJ-2026-001");
        assertThat(response.getAdjustmentId()).isEqualTo(7L);
        assertThat(response.getSupplierId()).isEqualTo(1L);
        assertThat(response.getAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void createAdjustment_incrementsRefSequence() {
        when(repository.countByAdjustmentRefPrefix("ADJ-2026-")).thenReturn(15L);
        when(repository.save(any(ManualLedgerAdjustment.class))).thenAnswer(inv -> inv.getArgument(0));

        ManualAdjustmentResponseDto response = service.createAdjustment(base());

        assertThat(response.getAdjustmentRef()).isEqualTo("ADJ-2026-016");
    }

    @Test
    void createAdjustment_rejectsBlankRemarks() {
        CreateManualAdjustmentRequestDto req = base();
        req.setRemarks("");

        assertThatThrownBy(() -> service.createAdjustment(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Remarks");
    }

    @Test
    void createAdjustment_rejectsNullRemarks() {
        CreateManualAdjustmentRequestDto req = base();
        req.setRemarks(null);

        assertThatThrownBy(() -> service.createAdjustment(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Remarks");
    }

    @Test
    void createAdjustment_negativeAmountAccepted() {
        when(repository.countByAdjustmentRefPrefix(anyString())).thenReturn(0L);
        when(repository.save(any(ManualLedgerAdjustment.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateManualAdjustmentRequestDto req = base();
        req.setAmount(new BigDecimal("-25.00"));

        ManualAdjustmentResponseDto response = service.createAdjustment(req);

        assertThat(response.getAmount()).isEqualByComparingTo("-25.00");
    }
}
