package com.plover.backerymanagmentsystem.finance.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plover.backerymanagmentsystem.finance.dto.SupplierForLedgerDto;
import com.plover.backerymanagmentsystem.store_keeper.model.Supplier;
import com.plover.backerymanagmentsystem.store_keeper.repository.SupplierRepository;

@ExtendWith(MockitoExtension.class)
class FinanceSupplierServiceImplTest {

    @Mock private SupplierRepository supplierRepository;

    @InjectMocks private FinanceSupplierServiceImpl service;

    @Test
    void getAllSuppliersForLedger_returnsProjectedSuppliers() {
        when(supplierRepository.findAll()).thenReturn(List.of(
                Supplier.builder().supplierId(1L).name("Fresh Farms Ltd").build(),
                Supplier.builder().supplierId(2L).name("Pacific Produce Co.").build()));

        List<SupplierForLedgerDto> result = service.getAllSuppliersForLedger();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("SUP-001");
        assertThat(result.get(0).getCode()).isEqualTo("FFL");
        assertThat(result.get(1).getCode()).isEqualTo("PPC");
    }

    @Test
    void deriveCode_singleWordPadsWithLetters() {
        assertThat(FinanceSupplierServiceImpl.deriveCode("Acme")).isEqualTo("ACM");
    }

    @Test
    void deriveCode_blankFallsBackToSup() {
        assertThat(FinanceSupplierServiceImpl.deriveCode(null)).isEqualTo("SUP");
        assertThat(FinanceSupplierServiceImpl.deriveCode("")).isEqualTo("SUP");
    }

    @Test
    void deriveCode_capsAtThreeChars() {
        assertThat(FinanceSupplierServiceImpl.deriveCode("Alpha Beta Gamma Delta")).isEqualTo("ABG");
    }
}
