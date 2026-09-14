package com.plover.backerymanagmentsystem.pos.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.pos.dto.DayEndClosingRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.DayEndClosingResponseDto;
import com.plover.backerymanagmentsystem.pos.exception.DayEndException;
import com.plover.backerymanagmentsystem.pos.model.DayEndClosing;
import com.plover.backerymanagmentsystem.pos.repository.DayEndClosingItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayEndClosingRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionRepository;
import com.plover.backerymanagmentsystem.pos.service.DayProductionService;

@ExtendWith(MockitoExtension.class)
public class DayEndClosingServiceImplTest {

    @Mock
    private DayEndClosingRepository dayEndClosingRepository;
    @Mock
    private DayEndClosingItemRepository dayEndClosingItemRepository;
    @Mock
    private DayProductionRepository dayProductionRepository;
    @Mock
    private DayProductionItemRepository dayProductionItemRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private DayProductionService dayProductionService;

    @InjectMocks
    private DayEndClosingServiceImpl dayEndClosingService;

    private Long outletId = 1L;
    private UUID cashierId = UUID.randomUUID();

    @Test
    void testSubmitClosing_Success() {
        DayEndClosingRequestDto request = DayEndClosingRequestDto.builder()
                .outletId(outletId)
                .cashierId(cashierId)
                .items(List.of(
                        DayEndClosingRequestDto.ClosingItemRequestDto.builder()
                                .productId(1L)
                                .systemQty(10)
                                .physicalQty(10)
                                .carryForwardQty(8)
                                .wastageQty(2)
                                .build()
                ))
                .build();

        Product product = new Product();
        product.setId(1L);
        product.setProductName("Test Product");

        when(dayEndClosingRepository.findByOutletIdAndClosingDate(any(), any())).thenReturn(Optional.empty());
        when(dayEndClosingRepository.save(any())).thenAnswer(invocation -> {
            DayEndClosing closing = invocation.getArgument(0);
            closing.setId(100);
            return closing;
        });
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(dayProductionRepository.findByOrderedDateAndOutletIdAndIsActiveTrue(any(), any())).thenReturn(new ArrayList<>());
        when(dayProductionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(dayProductionItemRepository.findFirstByProduct_IdAndOutletIdOrderByDayProductionItemIdDesc(any(), any())).thenReturn(List.of());

        DayEndClosingResponseDto response = dayEndClosingService.submitClosing(request);

        assertTrue(response.isSuccess());
        assertEquals(100, response.getClosingId());
        verify(dayEndClosingRepository).save(any());
        verify(dayEndClosingItemRepository).saveAll(any());
    }

    @Test
    void testSubmitClosing_ValidationFail_Discrepancy() {
        DayEndClosingRequestDto request = DayEndClosingRequestDto.builder()
                .outletId(outletId)
                .cashierId(cashierId)
                .items(List.of(
                        DayEndClosingRequestDto.ClosingItemRequestDto.builder()
                                .productId(1L)
                                .systemQty(10)
                                .physicalQty(10)
                                .carryForwardQty(5)
                                .wastageQty(2) // 5 + 2 != 10
                                .build()
                ))
                .build();

        when(dayEndClosingRepository.findByOutletIdAndClosingDate(any(), any())).thenReturn(Optional.empty());
        when(dayEndClosingRepository.save(any())).thenReturn(new DayEndClosing());

        assertThrows(DayEndException.class, () -> dayEndClosingService.submitClosing(request));
    }
}
