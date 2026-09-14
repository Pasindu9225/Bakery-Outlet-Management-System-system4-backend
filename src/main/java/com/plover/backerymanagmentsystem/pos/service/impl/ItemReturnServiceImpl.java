package com.plover.backerymanagmentsystem.pos.service.impl;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.pos.dto.GetSaleByIdResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.ReturnRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.ReturnResponseDto;
import com.plover.backerymanagmentsystem.pos.model.DayProductionItem;
import com.plover.backerymanagmentsystem.pos.model.Return;
import com.plover.backerymanagmentsystem.pos.model.ReturnItem;
import com.plover.backerymanagmentsystem.pos.model.Sale;
import com.plover.backerymanagmentsystem.pos.model.SaleItem;
import com.plover.backerymanagmentsystem.pos.model.DiscountType;
import com.plover.backerymanagmentsystem.pos.exception.SaleException;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.ReturnItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.ReturnRepository;
import com.plover.backerymanagmentsystem.pos.repository.SaleItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.SaleRepository;
import com.plover.backerymanagmentsystem.pos.service.ItemReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ItemReturnServiceImpl implements ItemReturnService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final AuthRepository authRepository;
    private final ReturnRepository returnRepository;
    private final ReturnItemRepository returnItemRepository;
    private final DayProductionItemRepository dayProductionItemRepository;

    @Override
    public List<GetSaleByIdResponseDto> getTodaySalesWithDetails() {
        LocalDate today = LocalDate.now();
        log.info("Fetching sales for today: {}", today);

        List<Sale> todaySales = saleRepository.findBySaleDate(today);
        log.info("Found {} sales for today", todaySales.size());

        return todaySales.stream()
                .map(this::convertToDetailedDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ReturnResponseDto processReturn(ReturnRequestDto requestDto) {
        log.info("Processing return for sale ID: {}, refund type: {}", requestDto.getSaleId(), requestDto.getRefundType());

        if (requestDto.getReturnReason() == null || requestDto.getReturnReason().trim().isEmpty()) {
            throw new SaleException("Return reason is mandatory.");
        }

        try {
            // 1. Create and save the primary return record
            Return returnRecord = Return.builder()
                    .saleId(requestDto.getSaleId())
                    .returnReason(requestDto.getReturnReason())
                    .refundType(Return.RefundType.valueOf(requestDto.getRefundType()))
                    .cashierId(requestDto.getCashierId())
                    .paymentMethodId(requestDto.getPaymentMethodId())
                    .totalReturnAmount(requestDto.getTotalReturnAmount())
                    .totalExchangeAmount(requestDto.getTotalExchangeAmount())
                    .netRefundAmount(requestDto.getNetRefundAmount())
                    .build();

            returnRecord = returnRepository.save(returnRecord);
            Integer returnId = returnRecord.getReturnId();

            // 2. Handle return items
            BigDecimal deductedAmount = BigDecimal.ZERO;

            if (requestDto.getReturnItems() != null) {
                for (ReturnRequestDto.ReturnItemDto itemDto : requestDto.getReturnItems()) {
                    if (requestDto.getSaleId() != null) {
                        SaleItem saleItem = saleItemRepository.findById(itemDto.getSaleItemId())
                                .orElseThrow(() -> new SaleException("SaleItem not found for ID: " + itemDto.getSaleItemId()));

                        if (!saleItem.getSaleId().equals(requestDto.getSaleId())) {
                            throw new SaleException("SaleItem does not belong to the specified Sale ID.");
                        }

                        List<ReturnItem> pastReturns = returnItemRepository.findBySaleItemId(itemDto.getSaleItemId());
                        int alreadyReturnedQty = pastReturns.stream().mapToInt(ReturnItem::getQty).sum();
                        
                        if (alreadyReturnedQty + itemDto.getQty() > saleItem.getQty()) {
                            throw new SaleException("Quantity being returned exceeds the originally sold quantity.");
                        }
                    }

                    ReturnItem returnItem = ReturnItem.builder()
                            .returnId(returnId)
                            .saleItemId(itemDto.getSaleItemId())
                            .qty(itemDto.getQty())
                            .unitPrice(itemDto.getUnitPrice())
                            .isResellable(itemDto.getIsResellable())
                            .build();

                    returnItemRepository.save(returnItem);
                    
                    deductedAmount = deductedAmount.add(itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQty())));

                    // 2.1 Update inventory or wastage
                    SaleItem saleItemRef = null;
                    if (itemDto.getSaleItemId() != null) {
                        saleItemRef = saleItemRepository.findById(itemDto.getSaleItemId()).orElse(null);
                    }
                    if (saleItemRef != null && saleItemRef.getDayProductionItemId() != null) {
                        if (Boolean.TRUE.equals(itemDto.getIsResellable())) {
                            updateInventory(saleItemRef.getDayProductionItemId(), itemDto.getQty());
                        } else {
                            updateWastage(saleItemRef.getDayProductionItemId(), itemDto.getQty());
                        }
                    }
                }
            }

            // 3. Handle exchange items (update inventory)
            if (requestDto.getExchangeItems() != null) {
                for (ReturnRequestDto.ExchangeItemDto exchangeDto : requestDto.getExchangeItems()) {
                    // Update inventory (decrement - items going out)
                    updateInventory(exchangeDto.getDayProductionItemId(), -exchangeDto.getQty());
                }
            }

            // 4. Update Original Sale Totals
            if (requestDto.getSaleId() != null && deductedAmount.compareTo(BigDecimal.ZERO) > 0) {
                Sale originalSale = saleRepository.findById(requestDto.getSaleId())
                       .orElseThrow(() -> new SaleException("Sale not found"));
                
                originalSale.setTotalAmount(originalSale.getTotalAmount().subtract(deductedAmount));
                originalSale.setFinalTotal(originalSale.getFinalTotal().subtract(deductedAmount));
                // Clamp to zero
                if (originalSale.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
                    originalSale.setTotalAmount(BigDecimal.ZERO);
                }
                if (originalSale.getFinalTotal().compareTo(BigDecimal.ZERO) < 0) {
                    originalSale.setFinalTotal(BigDecimal.ZERO);
                }
                saleRepository.save(originalSale);
            }

            return ReturnResponseDto.builder()
                    .success(true)
                    .message("Return processed successfully")
                    .returnId(returnId)
                    .build();

        } catch (Exception e) {
            log.error("Error processing return: {}", e.getMessage(), e);
            throw e;
        }
    }

    private void updateInventory(Integer dayProductionItemId, Integer qtyChange) {
        DayProductionItem dpi = dayProductionItemRepository.findById(dayProductionItemId).orElse(null);
        if (dpi != null) {
            Integer currentQty = dpi.getCurrentQty() != null ? dpi.getCurrentQty() : 0;
            dpi.setCurrentQty(currentQty + qtyChange);
            dayProductionItemRepository.save(dpi);
            log.info("Updated inventory for DayProductionItem ID {}: change {}, new qty {}",
                    dayProductionItemId, qtyChange, dpi.getCurrentQty());
        }
    }

    private void updateWastage(Integer dayProductionItemId, Integer qtyChange) {
        DayProductionItem dpi = dayProductionItemRepository.findById(dayProductionItemId).orElse(null);
        if (dpi != null) {
            Integer wastageQty = dpi.getWastageQty() != null ? dpi.getWastageQty() : 0;
            dpi.setWastageQty(wastageQty + qtyChange);
            dayProductionItemRepository.save(dpi);
            log.info("Updated wastage for DayProductionItem ID {}: change {}, new wastage qty {}",
                    dayProductionItemId, qtyChange, dpi.getWastageQty());
        }
    }

    private GetSaleByIdResponseDto convertToDetailedDto(Sale sale) {
        String cashierName = getCashierFullName(sale.getCashierId());

        List<GetSaleByIdResponseDto.SaleItemDetailDto> itemDetails = sale.getSaleItems().stream()
                .map(this::convertToItemDetailDto)
                .collect(Collectors.toList());

        return GetSaleByIdResponseDto.builder()
                .saleId(sale.getSaleId())
                .billNumber(String.format("BILL-%06d", sale.getSaleId()))
                .saleDate(sale.getSaleDate())
                .saleTime(sale.getSaleTime())
                .cashierId(sale.getCashierId())
                .cashierName(cashierName)
                .totalAmount(sale.getTotalAmount())
                .invoicePrinted(sale.getInvoicePrinted())
                .saleItems(itemDetails)
                .build();
    }

    private GetSaleByIdResponseDto.SaleItemDetailDto convertToItemDetailDto(SaleItem saleItem) {
        String productName = "Unknown Product";
        String productCode = "N/A";
        if (saleItem.getDayProductionItem() != null && saleItem.getDayProductionItem().getProduct() != null) {
            Product product = saleItem.getDayProductionItem().getProduct();
            productName = product.getProductName();
            productCode = product.getProductCode();
        }

        String discountName = null;
        BigDecimal discountPercentage = null;
        if (saleItem.getDiscount() != null) {
            discountName = saleItem.getDiscount().getName();
            if (saleItem.getDiscount().getDiscountType() == DiscountType.PERCENTAGE) {
                discountPercentage = saleItem.getDiscount().getDiscountValue();
            }
        }

        String paymentMethodName = "Unknown Payment Method";
        if (saleItem.getPaymentMethod() != null) {
            paymentMethodName = saleItem.getPaymentMethod().getName();
        }

        String cashierName = getCashierFullName(saleItem.getCashierId());

        return GetSaleByIdResponseDto.SaleItemDetailDto.builder()
                .saleItemId(saleItem.getSaleItemId())
                .dayProductionItemId(saleItem.getDayProductionItemId())
                .productName(productName)
                .productCode(productCode)
                .qty(saleItem.getQty())
                .price(saleItem.getPrice())
                .freeMealReason(saleItem.getFreeMealReason())
                .bankTransferCode(saleItem.getBankTransferCode())
                .discountId(saleItem.getDiscountId())
                .discountName(discountName)
                .discountPercentage(discountPercentage)
                .paymentMethodId(saleItem.getPaymentMethodId())
                .paymentMethodName(paymentMethodName)
                .cashierId(saleItem.getCashierId())
                .cashierName(cashierName)
                .build();
    }

    private String getCashierFullName(UUID cashierId) {
        try {
            AuthModel cashier = authRepository.findById(IdUtil.uuidToBytes(cashierId)).orElse(null);
            if (cashier != null) {
                return (cashier.getFirstName() != null ? cashier.getFirstName() : "") + 
                       " " + 
                       (cashier.getLastName() != null ? cashier.getLastName() : "").trim();
            }
            return "Unknown Cashier";
        } catch (Exception e) {
            log.warn("Could not retrieve cashier name for ID: {}", cashierId);
            return "Unknown Cashier";
        }
    }
}
