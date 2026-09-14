package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.GrnReceiveRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.dto.GrnReceiveResponseDto;
import com.plover.backerymanagmentsystem.store_keeper.model.Grn;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnItem;
import com.plover.backerymanagmentsystem.store_keeper.model.GrnStatus;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnItemRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.GrnRepository;
import com.plover.backerymanagmentsystem.store_keeper.repository.PurchaseOrderRepository;
import com.plover.backerymanagmentsystem.store_keeper.service.RawMaterialBatchService;

@ExtendWith(MockitoExtension.class)
class GrnServiceImplTest {

    @Mock
    private GrnRepository grnRepository;

    @Mock
    private GrnItemRepository grnItemRepository;

    @Mock
    private PurchaseOrderRepository purchaseOrderRepository;

    @Mock
    private RawMaterialBatchService rawMaterialBatchService;

    @Mock
    private RawMaterialRepository rawMaterialRepository;

    @InjectMocks
    private GrnServiceImpl grnService;

    private Grn mockGrn;
    private GrnItem mockGrnItem;
    private RawMaterial mockMaterial;

    @BeforeEach
    void setUp() {
        mockGrn = Grn.builder()
                .grnId(1L)
                .poId(100L)
                .supplierId(5L)
                .total(new BigDecimal("500.00"))
                .grnStatus(GrnStatus.PENDING)
                .isReceived(false)
                .build();

        mockGrnItem = GrnItem.builder()
                .grnItemId(10L)
                .grnId(1L)
                .rawMaterialId(127L)
                .uom("pack")
                .pricePerUnit(new BigDecimal("50.00"))
                .receivedQuantity(BigDecimal.ZERO)
                .build();

        mockMaterial = RawMaterial.builder()
                .id(127L)
                .materialCode("RM127")
                .materialName("Brown Sugar 50Kg Pack")
                .build();
    }

    @Test
    void receiveGoods_AutoGeneratesBatchNo_WhenBatchNoNotProvided() {
        when(grnRepository.findByGrnIdForUpdate(1L)).thenReturn(Optional.of(mockGrn));
        when(grnItemRepository.findByGrnIdWithRawMaterial(1L)).thenReturn(List.of(mockGrnItem));
        when(rawMaterialRepository.findById(127L)).thenReturn(Optional.of(mockMaterial));
        when(rawMaterialRepository.findByMaterialCodeAndBatchNo(anyString(), anyString())).thenReturn(Optional.empty());
        when(rawMaterialBatchService.createNewBatch(eq(127L), anyString(), any(), anyString(), any(), any()))
                .thenReturn(999L);

        GrnReceiveRequestDto.GrnReceiveItemDto itemDto = GrnReceiveRequestDto.GrnReceiveItemDto.builder()
                .grnItemId(10L)
                .receivedQuantity(new BigDecimal("10.000"))
                .batchNo(null) // omitted/null batch number
                .expireDate(LocalDate.of(2027, 12, 31))
                .build();

        GrnReceiveRequestDto request = GrnReceiveRequestDto.builder()
                .grnStatus(GrnStatus.RECEIVED)
                .invoiceNumber("INV-2026-001")
                .items(List.of(itemDto))
                .build();

        GrnReceiveResponseDto response = grnService.receiveGoods(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.getSuccess()).isTrue();
        assertThat(mockGrnItem.getBatchNo()).startsWith("BAT-");
        assertThat(mockGrnItem.getBatchNo()).contains("-RM127-");

        verify(rawMaterialBatchService).createNewBatch(
                eq(127L),
                eq(mockGrnItem.getBatchNo()),
                eq(new BigDecimal("10.000")),
                anyString(),
                eq(new BigDecimal("50.00")),
                eq(LocalDate.of(2027, 12, 31))
        );
    }
}
