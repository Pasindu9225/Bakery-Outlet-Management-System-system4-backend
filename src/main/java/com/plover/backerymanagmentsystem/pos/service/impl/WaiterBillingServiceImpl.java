package com.plover.backerymanagmentsystem.pos.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.admin.model.BmsAuth;
import com.plover.backerymanagmentsystem.admin.repository.BmsAuthRepository;
import com.plover.backerymanagmentsystem.pos.dto.CreateSaleResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.waiter.*;
import com.plover.backerymanagmentsystem.pos.model.*;
import com.plover.backerymanagmentsystem.pos.repository.PosWaiterItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.SaleRepository;
import com.plover.backerymanagmentsystem.pos.repository.SaleItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.PaymentMethodRepository;
import com.plover.backerymanagmentsystem.pos.service.WaiterBillingService;
import com.plover.backerymanagmentsystem.core.login.service.AuthService;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class WaiterBillingServiceImpl implements WaiterBillingService {

    private final BmsAuthRepository bmsAuthRepository;
    private final PosWaiterItemRepository posWaiterItemRepository;
    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final AuthService authService;
    private final ProductRepository productRepository;
    private final DayProductionItemRepository dayProductionItemRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    public List<BmsAuth> getWaitersForOutlet(Long outletId) {
        return bmsAuthRepository.findByRoleIdAndOutletId("11", outletId);
    }

    @Override
    @Transactional
    public PosWaiterItemResponseDto addWaiterItem(AddWaiterItemRequestDto requestDto) {
        try {
            Product product = productRepository.findById(requestDto.getProductId().longValue())
                    .orElseThrow(() -> new RuntimeException("Product not found"));

            byte[] waiterUuidBytes;
            try {
                waiterUuidBytes = com.plover.backerymanagmentsystem.admin.util.IdUtil.uuidToBytes(UUID.fromString(requestDto.getWaiterId()));
            } catch(Exception e) {
                waiterUuidBytes = requestDto.getWaiterId().getBytes();
            }

            Long cashierOutletId = null;
            try {
                cashierOutletId = authService.getCurrentUser().getOutletId();
            } catch (Exception e) {
                log.warn("Failed to get current user's outlet ID: {}", e.getMessage());
            }

            // Fetch the latest dayProductionItemId for the given product (prefer current outlet)
            Integer dayProductionItemId = null;
            if (cashierOutletId != null) {
                dayProductionItemId = dayProductionItemRepository
                        .findFirstByProduct_IdAndOutletIdOrderByDayProductionItemIdDesc(product.getId(), cashierOutletId)
                        .stream().findFirst()
                        .map(DayProductionItem::getDayProductionItemId)
                        .orElse(null);
            }
            if (dayProductionItemId == null) {
                dayProductionItemId = dayProductionItemRepository
                        .findFirstByProduct_IdOrderByDayProductionItemIdDesc(product.getId())
                        .map(DayProductionItem::getDayProductionItemId)
                        .orElse(0);
            }

            PosWaiterItem item = PosWaiterItem.builder()
                    .waiterId(waiterUuidBytes)
                    .productId(product.getId().intValue())
                    .productName(product.getProductName())
                    .qty(requestDto.getQty())
                    .unitPrice(requestDto.getUnitPrice() != null ? requestDto.getUnitPrice() : BigDecimal.valueOf(product.getSalePrice()))
                    .instructions(requestDto.getInstructions())
                    .dayProductionItemId(dayProductionItemId)
                    .isPaid(false)
                    .billId(requestDto.getBillId())
                    .build();

            item = posWaiterItemRepository.save(item);
            
            return mapToResponseDto(item);
        } catch (Exception e) {
            try {
                java.io.StringWriter sw = new java.io.StringWriter();
                e.printStackTrace(new java.io.PrintWriter(sw));
                java.nio.file.Files.writeString(
                    java.nio.file.Paths.get("d:\\spring\\claude\\original\\BMS\\backerymanagmentsystem-Backend\\add_item_error.txt"),
                    sw.toString()
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            throw e;
        }
    }

    @Override
    public WaiterBillingDetailsResponseDto getWaiterBillingDetails(String waiterId) {
        byte[] waiterUuidBytes;
        try {
            waiterUuidBytes = com.plover.backerymanagmentsystem.admin.util.IdUtil.uuidToBytes(UUID.fromString(waiterId));
        } catch(Exception e) {
            waiterUuidBytes = waiterId.getBytes();
        }

        List<PosWaiterItem> unpaidItems = posWaiterItemRepository.findByWaiterIdAndIsPaidFalse(waiterUuidBytes);
        
        List<PosWaiterItemResponseDto> itemDtos = unpaidItems.stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());

        BigDecimal subTotal = itemDtos.stream()
                .map(PosWaiterItemResponseDto::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return WaiterBillingDetailsResponseDto.builder()
                .waiterId(waiterId)
                .unpaidItems(itemDtos)
                .subTotal(subTotal)
                .build();
    }

    @Override
    @Transactional
    public CreateSaleResponseDto finishWaiterBilling(FinishWaiterBillingRequestDto requestDto) {
        byte[] waiterUuidBytes;
        try {
            waiterUuidBytes = com.plover.backerymanagmentsystem.admin.util.IdUtil.uuidToBytes(UUID.fromString(requestDto.getWaiterId()));
        } catch(Exception e) {
            waiterUuidBytes = requestDto.getWaiterId().getBytes();
        }

        List<PosWaiterItem> items = posWaiterItemRepository.findByWaiterIdAndIsPaidFalse(waiterUuidBytes);
        if (requestDto.getItemIdsToPay() != null && !requestDto.getItemIdsToPay().isEmpty()) {
            items = items.stream()
                    .filter(item -> requestDto.getItemIdsToPay().contains(item.getId()))
                    .collect(Collectors.toList());
        }
        
        if (items.isEmpty()) {
            throw new RuntimeException("No unpaid items found for waiter");
        }

        PaymentCategory paymentCategory = PaymentCategory.valueOf(requestDto.getPaymentType());
        List<PaymentMethod> methods = paymentMethodRepository.findAll();
        Integer paymentMethodId = methods.stream()
                .filter(m -> m.getCategory() == paymentCategory)
                .map(PaymentMethod::getPaymentMethodId)
                .findFirst()
                .orElse(1); // Default to 1 (Cash)

        Sale sale = Sale.builder()
                .saleDate(LocalDate.now())
                .saleTime(LocalTime.now())
                .cashierId(authService.getCurrentUser().getUserId())
                .totalAmount(requestDto.getFinalTotal().add(requestDto.getDiscountAmount() != null ? requestDto.getDiscountAmount() : BigDecimal.ZERO))
                .finalTotal(requestDto.getFinalTotal())
                .discountAmount(requestDto.getDiscountAmount())
                .discountReason(requestDto.getDiscountReason())
                .outletId(requestDto.getOutletId())
                .paymentType(paymentCategory)
                .invoicePrinted(requestDto.getInvoicePrinted() != null ? requestDto.getInvoicePrinted() : false)
                .build();

        sale = saleRepository.save(sale);

        for (PosWaiterItem item : items) {
            item.setIsPaid(true);
            posWaiterItemRepository.save(item);

            Integer dayProductionItemId = item.getDayProductionItemId();
            if (dayProductionItemId == null || dayProductionItemId == 0) {
                if (requestDto.getOutletId() != null) {
                    dayProductionItemId = dayProductionItemRepository
                            .findFirstByProduct_IdAndOutletIdOrderByDayProductionItemIdDesc(item.getProductId().longValue(), requestDto.getOutletId())
                            .stream().findFirst()
                            .map(DayProductionItem::getDayProductionItemId)
                            .orElse(null);
                }
                if (dayProductionItemId == null) {
                    dayProductionItemId = dayProductionItemRepository
                            .findFirstByProduct_IdOrderByDayProductionItemIdDesc(item.getProductId().longValue())
                            .map(DayProductionItem::getDayProductionItemId)
                            .orElse(null);
                }
            }

            if (dayProductionItemId == null) {
                throw new RuntimeException("Product '" + item.getProductName() + "' has no active Day Production entry. Please add it to today's production plan first.");
            }

            SaleItem saleItem = SaleItem.builder()
                    .sale(sale)
                    .saleId(sale.getSaleId())
                    .productId(item.getProductId().longValue())
                    .qty(item.getQty())
                    .price(item.getUnitPrice())
                    .dayProductionItemId(dayProductionItemId)
                    .paymentMethodId(paymentMethodId)
                    .cashierId(UUID.fromString(authService.getCurrentUser().getUserId().toString()))
                    .itemStatus(SaleItemStatus.COMPLETED)
                    .build();
            saleItemRepository.save(saleItem);

            // Decrement currentQty of DayProductionItem
            final Integer dpiId = dayProductionItemId;
            dayProductionItemRepository.findById(dpiId).ifPresent(dpi -> {
                if (dpi.getCurrentQty() != null) {
                    dpi.setCurrentQty(dpi.getCurrentQty() - item.getQty());
                    dayProductionItemRepository.save(dpi);
                    log.info("Updated current_qty for day_production_item ID {} (waiter sale): new qty {}",
                            dpi.getDayProductionItemId(), dpi.getCurrentQty());
                }
            });
        }

        CreateSaleResponseDto.SaleDataDto saleData = new CreateSaleResponseDto.SaleDataDto();
        saleData.setSaleId(sale.getSaleId());
        
        return CreateSaleResponseDto.builder()
                .success(true)
                .message("Waiter billing completed")
                .data(saleData)
                .build();
    }

    @Override
    @Transactional
    public void removeWaiterItem(Long itemId) {
        posWaiterItemRepository.deleteById(itemId);
    }

    @Override
    @Transactional
    public void transferWaiterItems(String fromWaiterId, String toWaiterId) {
        byte[] fromUuidBytes;
        try {
            fromUuidBytes = com.plover.backerymanagmentsystem.admin.util.IdUtil.uuidToBytes(UUID.fromString(fromWaiterId));
        } catch(Exception e) {
            fromUuidBytes = fromWaiterId.getBytes();
        }

        byte[] toUuidBytes;
        try {
            toUuidBytes = com.plover.backerymanagmentsystem.admin.util.IdUtil.uuidToBytes(UUID.fromString(toWaiterId));
        } catch(Exception e) {
            toUuidBytes = toWaiterId.getBytes();
        }

        List<PosWaiterItem> items = posWaiterItemRepository.findByWaiterIdAndIsPaidFalse(fromUuidBytes);
        for (PosWaiterItem item : items) {
            item.setWaiterId(toUuidBytes);
            posWaiterItemRepository.save(item);
        }
    }

    private PosWaiterItemResponseDto mapToResponseDto(PosWaiterItem item) {
        return PosWaiterItemResponseDto.builder()
                .id(item.getId())
                .waiterId(new String(item.getWaiterId()))
                .productId(item.getProductId())
                .productName(item.getProductName())
                .qty(item.getQty())
                .unitPrice(item.getUnitPrice())
                .totalPrice(item.getUnitPrice().multiply(new BigDecimal(item.getQty())))
                .instructions(item.getInstructions())
                .isPaid(item.getIsPaid())
                .kotId(item.getKotId())
                .billId(item.getBillId())
                .build();
    }
}
