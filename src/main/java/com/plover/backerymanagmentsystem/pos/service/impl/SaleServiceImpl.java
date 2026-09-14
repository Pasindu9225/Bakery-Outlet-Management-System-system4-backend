package com.plover.backerymanagmentsystem.pos.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.manager.model.ProductionOrder;
import com.plover.backerymanagmentsystem.manager.model.ProductionOrderItem;
import com.plover.backerymanagmentsystem.pos.dto.CreateSaleRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.CreateSaleResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GetAllSalesResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GetSaleByIdResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.DailyDiscountSummaryDto;
import com.plover.backerymanagmentsystem.pos.dto.SaleItemRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.SaleItemResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.StandaloneKotRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.StandaloneKotResponseDto;
import com.plover.backerymanagmentsystem.pos.exception.InvalidCashierException;
import com.plover.backerymanagmentsystem.pos.exception.InvalidDayProductionItemException;
import com.plover.backerymanagmentsystem.pos.exception.InvalidPaymentMethodException;
import com.plover.backerymanagmentsystem.pos.exception.MultiplePaymentMethodsException;
import com.plover.backerymanagmentsystem.pos.exception.SaleException;
import com.plover.backerymanagmentsystem.pos.exception.SaleNotFoundException;
import com.plover.backerymanagmentsystem.pos.model.DayProductionItem;
import com.plover.backerymanagmentsystem.pos.model.DiscountAudit;
import com.plover.backerymanagmentsystem.pos.model.Discount;
import com.plover.backerymanagmentsystem.pos.model.DiscountType;
import com.plover.backerymanagmentsystem.pos.model.PaymentCategory;
import com.plover.backerymanagmentsystem.pos.model.PaymentMethod;
import com.plover.backerymanagmentsystem.pos.model.Promotion;
import com.plover.backerymanagmentsystem.pos.model.Sale;
import com.plover.backerymanagmentsystem.pos.model.SaleItem;
import com.plover.backerymanagmentsystem.pos.model.SaleItemStatus;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.DiscountAuditRepository;
import com.plover.backerymanagmentsystem.pos.repository.DiscountRepository;
import com.plover.backerymanagmentsystem.pos.repository.PaymentMethodRepository;
import com.plover.backerymanagmentsystem.pos.repository.PromotionRepository;
import com.plover.backerymanagmentsystem.pos.repository.SaleItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.SaleRepository;
import com.plover.backerymanagmentsystem.pos.service.SaleService;
import com.plover.backerymanagmentsystem.store_keeper.repository.ProductionOrderRepository;
import com.plover.backerymanagmentsystem.manager.model.OutletProductionCenter;
import com.plover.backerymanagmentsystem.manager.repository.OutletProductionCenterRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of SaleService for managing sale operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SaleServiceImpl implements SaleService {

    private final SaleRepository saleRepository;
    private final SaleItemRepository saleItemRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final DayProductionItemRepository dayProductionItemRepository;
    private final DiscountRepository discountRepository;
    private final AuthRepository authRepository;
    private final PromotionRepository promotionRepository;
    private final DiscountAuditRepository discountAuditRepository;
    private final ProductionOrderRepository productionOrderRepository;
    private final com.plover.backerymanagmentsystem.pos.service.CashFloatService cashFloatService;
    private final ProductRepository productRepository;
    private final com.plover.backerymanagmentsystem.pos.service.DiscountService discountService;
    private final OutletProductionCenterRepository outletProductionCenterRepository;
    private final ProductionCenterRepository productionCenterRepository;
    private final com.plover.backerymanagmentsystem.manager.service.CustomerService customerService;

    @Override
    @Transactional
    public CreateSaleResponseDto createSale(CreateSaleRequestDto requestDto) {
        log.info("Creating sale for cashier ID: {} with {} items", requestDto.getCashierId(),
                requestDto.getItems().size());

        try {
            // 0. Validate morning float entry (FR-POS-16)
            if (!cashFloatService.isFloatOpen(requestDto.getCashierId())) {
                throw new SaleException("Opening balance must be declared before processing sales.");
            }

            // 1. Validate cashier
            AuthModel cashier = validateCashier(requestDto.getCashierId());

            // 2. Validate payment methods (ensure all items use the same payment method)
            Integer paymentMethodId = validatePaymentMethods(requestDto.getItems());
            PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                    .orElseThrow(() -> new InvalidPaymentMethodException(paymentMethodId));

            // 3. Validate day production items
            // List<DayProductionItem> dayProductionItems = validateDayProductionItems(requestDto.getItems());
            // Handled inside the loop now to support productId fallback

            // 4. Calculate total amount and prepare sale items data
            BigDecimal subtotalForGlobal = BigDecimal.ZERO;
            BigDecimal globalPromotionDiscount = BigDecimal.ZERO;
            Promotion globalPromotion = null;

            // Pre-calculate subtotal for global promotion distribution if applicable
            if (requestDto.getGlobalPromotionId() != null) {
                globalPromotion = promotionRepository.findById(requestDto.getGlobalPromotionId())
                        .orElseThrow(() -> new SaleException("Global Promotion not found"));

                LocalDateTime now = LocalDateTime.now();
                if (!globalPromotion.getIsActive() || now.isBefore(globalPromotion.getStartDate()) || now.isAfter(globalPromotion.getEndDate())) {
                    throw new SaleException("Global Promotion is Expired or Invalid");
                }

                for (SaleItemRequestDto item : requestDto.getItems()) {
                    subtotalForGlobal = subtotalForGlobal.add(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQty())));
                }

                if (globalPromotion.getDiscountType() == DiscountType.PERCENTAGE) {
                    BigDecimal percentage = BigDecimal.valueOf(globalPromotion.getDiscountValue()).divide(BigDecimal.valueOf(100));
                    globalPromotionDiscount = subtotalForGlobal.multiply(percentage);
                } else if (globalPromotion.getDiscountType() == DiscountType.FLAT) {
                    globalPromotionDiscount = BigDecimal.valueOf(globalPromotion.getDiscountValue());
                }

                if (globalPromotion.getMaximumDiscountValue() != null && globalPromotionDiscount.compareTo(BigDecimal.valueOf(globalPromotion.getMaximumDiscountValue())) > 0) {
                    globalPromotionDiscount = BigDecimal.valueOf(globalPromotion.getMaximumDiscountValue());
                }
            }

            BigDecimal totalAmount = BigDecimal.ZERO;
            BigDecimal discountAmountTotal = BigDecimal.ZERO;
            BigDecimal finalSaleTotal = BigDecimal.ZERO;
            List<SaleItemData> saleItemsData = new ArrayList<>();
            Map<String, BigDecimal> promoDiscounts = new HashMap<>();

            for (int i = 0; i < requestDto.getItems().size(); i++) {
                SaleItemRequestDto itemRequest = requestDto.getItems().get(i);
                DayProductionItem dayProductionItem = null;
                Product product = null;

                if (itemRequest.getDayProductionItemId() != null) {
                    dayProductionItem = dayProductionItemRepository.findById(itemRequest.getDayProductionItemId())
                            .orElseThrow(() -> new InvalidDayProductionItemException(itemRequest.getDayProductionItemId()));
                    product = dayProductionItem.getProduct();
                } else if (itemRequest.getProductId() != null) {
                    product = productRepository.findById(itemRequest.getProductId())
                            .orElseThrow(() -> new SaleException("Product not found: " + itemRequest.getProductId()));
                } else {
                    throw new SaleException("Either Day Production Item ID or Product ID must be provided");
                }

                BigDecimal itemBasePrice = itemRequest.getUnitPrice().multiply(BigDecimal.valueOf(itemRequest.getQty()));
                BigDecimal itemFinalPrice = itemBasePrice;
                BigDecimal appliedDiscountAmount = BigDecimal.ZERO;
                Promotion appliedPromotion = null;
                Discount appliedAutoDiscount = null;

                // Handle FR-POS-05: Free Meal Restriction
                if (paymentMethod.getCategory() == PaymentCategory.FREE_MEAL) {
                    if (itemRequest.getFreeMealReason() == null || itemRequest.getFreeMealReason().trim().isEmpty()) {
                        throw new SaleException("Reason for Free Meal is mandatory when payment method is FREE_MEAL.");
                    }
                    if (itemRequest.getFreeMealReason().trim().length() < 10) {
                        throw new SaleException("Reason for Free Meal must be at least 10 characters.");
                    }
                    
                    appliedDiscountAmount = itemBasePrice;
                    log.info("Free meal validation passed and applied for item {}: {}", dayProductionItem.getDayProductionItemId(),
                            itemRequest.getFreeMealReason());
                } else {
                    // Check for promotion first
                    if (itemRequest.getPromotionId() != null) {
                        appliedPromotion = promotionRepository.findById(itemRequest.getPromotionId())
                                .orElseThrow(() -> new SaleException("Promotion not found"));

                        LocalDateTime now = LocalDateTime.now();
                        if (!appliedPromotion.getIsActive() || now.isBefore(appliedPromotion.getStartDate()) || now.isAfter(appliedPromotion.getEndDate())) {
                            throw new SaleException("Promotion applied is Expired or Invalid");
                        }

                        if (appliedPromotion.getDiscountType() == DiscountType.PERCENTAGE) {
                            BigDecimal percentage = BigDecimal.valueOf(appliedPromotion.getDiscountValue()).divide(BigDecimal.valueOf(100));
                            appliedDiscountAmount = itemBasePrice.multiply(percentage);
                        } else if (appliedPromotion.getDiscountType() == DiscountType.FLAT) {
                            appliedDiscountAmount = BigDecimal.valueOf(appliedPromotion.getDiscountValue());
                        }

                        if (appliedPromotion.getMaximumDiscountValue() != null && appliedDiscountAmount.compareTo(BigDecimal.valueOf(appliedPromotion.getMaximumDiscountValue())) > 0) {
                            appliedDiscountAmount = BigDecimal.valueOf(appliedPromotion.getMaximumDiscountValue());
                        }

                        String code = appliedPromotion.getPromoCode();
                        BigDecimal currentDiscount = promoDiscounts.getOrDefault(code, BigDecimal.ZERO);
                        promoDiscounts.put(code, currentDiscount.add(appliedDiscountAmount));
                    }

                    // Handle Global Promotion (Proportional Split)
                    if (globalPromotion != null && subtotalForGlobal.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal itemProportion = itemBasePrice.divide(subtotalForGlobal, 10, java.math.RoundingMode.HALF_UP);
                        BigDecimal globalShare = globalPromotionDiscount.multiply(itemProportion).setScale(2, java.math.RoundingMode.HALF_UP);
                        
                        appliedDiscountAmount = appliedDiscountAmount.add(globalShare);
                        
                        // We set appliedPromotion = globalPromotion if no item-level promotion is present
                        // If both exist, the global one is added to the count but the entity ref in SaleItem
                        // will favor one of them. For auditing, we'll track the global one here.
                        if (appliedPromotion == null) {
                            appliedPromotion = globalPromotion;
                        }
                        
                        String code = globalPromotion.getPromoCode();
                        BigDecimal currentDiscount = promoDiscounts.getOrDefault(code, BigDecimal.ZERO);
                        promoDiscounts.put(code, currentDiscount.add(globalShare));
                    }

                    // Add manual discount if provided
                    if (itemRequest.getManualDiscount() != null && itemRequest.getManualDiscount().compareTo(BigDecimal.ZERO) > 0) {
                        appliedDiscountAmount = appliedDiscountAmount.add(itemRequest.getManualDiscount());
                        log.info("Manual discount of {} applied to product {}", itemRequest.getManualDiscount(), product.getId());
                    }

                    // Handle FR-POS-12: Product-Based & Time-Based Discounts (Auto-apply)
                    // Apply ONLY if no manual discount or promotion is already applied
                    if (appliedDiscountAmount.compareTo(BigDecimal.ZERO) == 0 && appliedPromotion == null) {
                        appliedAutoDiscount = discountService.findApplicableDiscount(product.getId());
                        if (appliedAutoDiscount != null) {
                            if (appliedAutoDiscount.getDiscountType() == DiscountType.PERCENTAGE) {
                                BigDecimal percentage = appliedAutoDiscount.getDiscountValue().divide(BigDecimal.valueOf(100));
                                appliedDiscountAmount = itemBasePrice.multiply(percentage);
                            } else {
                                appliedDiscountAmount = appliedAutoDiscount.getDiscountValue();
                            }
                            log.info("Auto-discount '{}' applied to product {}", appliedAutoDiscount.getName(), product.getId());
                        }
                    }
                }

                // Final clamp to ensure discount doesn't exceed base price
                if (appliedDiscountAmount.compareTo(itemBasePrice) > 0) {
                    appliedDiscountAmount = itemBasePrice;
                }

                itemFinalPrice = itemBasePrice.subtract(appliedDiscountAmount);
                if (itemFinalPrice.compareTo(BigDecimal.ZERO) < 0) {
                    itemFinalPrice = BigDecimal.ZERO;
                }

                totalAmount = totalAmount.add(itemBasePrice);
                discountAmountTotal = discountAmountTotal.add(appliedDiscountAmount);
                finalSaleTotal = finalSaleTotal.add(itemFinalPrice);

                // Find the discount entity for linking (manual ID or auto-applied)
                Discount discount = null;
                if (itemRequest.getDiscountId() != null && itemRequest.getDiscountId() > 0) {
                    discount = discountRepository.findById(itemRequest.getDiscountId()).orElse(null);
                }
                if (discount == null && appliedAutoDiscount != null) {
                    discount = appliedAutoDiscount;
                } else if (discount == null) {
                    discount = discountService.findApplicableDiscount(product.getId());
                }

                // Validate bank transfer code if needed
                if ("Bank Transfer".equalsIgnoreCase(paymentMethod.getName())
                        || "Online Transfer".equalsIgnoreCase(paymentMethod.getName())) {
                    if (itemRequest.getBankTransferCode() == null
                            || itemRequest.getBankTransferCode().trim().isEmpty()) {
                        throw new SaleException(
                                "Bank transfer code is required for " + paymentMethod.getName() + " payment method");
                    }
                }

                saleItemsData.add(SaleItemData.builder()
                        .itemRequest(itemRequest)
                        .dayProductionItem(dayProductionItem)
                        .product(product)
                        .discount(discount)
                        .appliedPromotion(appliedPromotion)
                        .appliedDiscount(appliedDiscountAmount)
                        .totalPrice(itemFinalPrice)
                        .isKotEnabled(product.getIsKotEnabled())
                        .productionCenterId(product.getProductionCenterId())
                        .build());
            }

            // Customer and Loyalty integration
            com.plover.backerymanagmentsystem.manager.model.Customer customer = null;
            if (requestDto.getCustomerPhoneNumber() != null && !requestDto.getCustomerPhoneNumber().trim().isEmpty()) {
                customer = customerService.findByContactNumber(requestDto.getCustomerPhoneNumber().trim()).orElse(null);
                if (customer == null) {
                    throw new SaleException("Customer with phone number " + requestDto.getCustomerPhoneNumber() + " not found.");
                }
            }

            boolean didRedeem = false;
            if (customer != null && Boolean.TRUE.equals(requestDto.getRedeemPoints())) {
                // Verify OTP
                boolean otpVerified = customerService.verifyOtp(customer.getContactNumber(), requestDto.getOtp());
                if (!otpVerified) {
                    throw new SaleException("Invalid or expired OTP for customer loyalty points redemption.");
                }

                // Check points
                Double availablePoints = customer.getLoyaltyPoints() != null ? customer.getLoyaltyPoints() : 0.0;
                if (availablePoints < 1000.0) {
                    throw new SaleException("Insufficient loyalty points to redeem. Required: 1000, Available: " + availablePoints);
                }

                // Deduct 1000 LKR or cap to final total
                BigDecimal redemptionDiscount = BigDecimal.valueOf(Math.min(1000.00, finalSaleTotal.doubleValue()));
                finalSaleTotal = finalSaleTotal.subtract(redemptionDiscount);
                discountAmountTotal = discountAmountTotal.add(redemptionDiscount);
                didRedeem = true;
            }

            // Collect discount reasons
            java.util.Set<String> reasons = new java.util.HashSet<>();
            if (globalPromotion != null) {
                reasons.add("Global Promo [ID: " + globalPromotion.getId() + ", Code: " + globalPromotion.getPromoCode() + "]");
            }
            if (didRedeem) {
                reasons.add("Loyalty Points Redemption");
            }
            for (SaleItemData itemData : saleItemsData) {
                if (itemData.getAppliedPromotion() != null && itemData.getAppliedPromotion() != globalPromotion) {
                    reasons.add("Promo [ID: " + itemData.getAppliedPromotion().getId() + ", Code: " + itemData.getAppliedPromotion().getPromoCode() + "]");
                }
                if (itemData.getDiscount() != null) {
                    reasons.add("Auto-Discount [ID: " + itemData.getDiscount().getDiscountId() + ", Name: " + itemData.getDiscount().getName() + "]");
                }
                if (itemData.getItemRequest().getManualDiscount() != null && itemData.getItemRequest().getManualDiscount().compareTo(BigDecimal.ZERO) > 0) {
                    reasons.add("Manual Discount");
                }
                if (itemData.getItemRequest().getFreeMealReason() != null) {
                    reasons.add("Free Meal: " + itemData.getItemRequest().getFreeMealReason());
                }
            }
            
            String finalDiscountReason = null;
            if (!reasons.isEmpty()) {
                finalDiscountReason = String.join(", ", reasons);
                if (finalDiscountReason.length() > 500) {
                    finalDiscountReason = finalDiscountReason.substring(0, 497) + "...";
                }
            }

            // 5. Create sale
            Sale sale = Sale.builder()
                    .saleDate(LocalDate.now())
                    .saleTime(LocalTime.now())
                    .cashierId(requestDto.getCashierId())
                    .totalAmount(totalAmount)
                    .discountAmount(discountAmountTotal)
                    .discountReason(finalDiscountReason)
                    .finalTotal(finalSaleTotal)
                    .tableId(requestDto.getTableId())
                    .outletId(requestDto.getOutletId())
                    .paymentType(paymentMethod.getCategory())
                    .deliveryOption(requestDto.getDeliveryOption())
                    .customerId(customer != null ? customer.getId() : null)
                    .invoicePrinted(requestDto.getInvoicePrinted() != null && requestDto.getInvoicePrinted())
                    .build();

            sale = saleRepository.save(sale);
            log.info("Sale created with ID: {}, Total amount: {}", sale.getSaleId(), finalSaleTotal);

            if (customer != null) {
                // Deduct points
                if (didRedeem) {
                    customerService.deductPoints(
                            customer.getId(),
                            sale.getSaleId(),
                            1000.0,
                            "Redeemed 1000 points at checkout for Sale #" + sale.getSaleId()
                    );
                }

                // Earn points: finalSaleTotal / 1000.0
                Double pointsEarned = finalSaleTotal.doubleValue() / 1000.0;
                if (pointsEarned > 0) {
                    customerService.addPoints(
                            customer.getId(),
                            sale.getSaleId(),
                            pointsEarned,
                            "Earned points from Sale #" + sale.getSaleId()
                    );
                }
            }

            // Audit the discounts
            for (Map.Entry<String, BigDecimal> entry : promoDiscounts.entrySet()) {
                DiscountAudit audit = DiscountAudit.builder()
                        .transactionId(sale.getSaleId())
                        .cashierId(sale.getCashierId())
                        .promoCode(entry.getKey())
                        .discountValue(entry.getValue())
                        .build();
                discountAuditRepository.save(audit);
                log.info("Audited discount: PromoCode '{}', Value '{}', Sale ID: '{}'", entry.getKey(), entry.getValue(), sale.getSaleId());
            }

            // 6. Create sale items and group for KOT
            List<SaleItem> saleItems = new ArrayList<>();
            List<SaleItemResponseDto> saleItemResponses = new ArrayList<>();
            Map<Long, List<SaleItemKotWrapper>> kotGroups = new HashMap<>();

            for (SaleItemData itemData : saleItemsData) {
                SaleItemStatus initialStatus = itemData.getIsKotEnabled() ? SaleItemStatus.PENDING : SaleItemStatus.COMPLETED;
                
                SaleItem saleItem = SaleItem.builder()
                        .dayProductionItemId(itemData.getItemRequest().getDayProductionItemId())
                        .productId(itemData.getProduct() != null ? itemData.getProduct().getId() : null)
                        .qty(itemData.getItemRequest().getQty())
                        .price(itemData.getTotalPrice())
                        .freeMealReason(itemData.getItemRequest().getFreeMealReason())
                        .bankTransferCode(itemData.getItemRequest().getBankTransferCode())
                        .discountId(itemData.getItemRequest().getDiscountId())
                        .promotionId(itemData.getAppliedPromotion() != null ? itemData.getAppliedPromotion().getId() : null)
                        .appliedDiscount(itemData.getAppliedDiscount())
                        .paymentMethodId(paymentMethodId)
                        .saleId(sale.getSaleId())
                        .cashierId(requestDto.getCashierId())
                        .specialInstructions(itemData.getItemRequest().getSpecialInstructions())
                        .itemStatus(initialStatus)
                        .build();

                saleItem = saleItemRepository.save(saleItem);
                saleItems.add(saleItem);

                if (itemData.getIsKotEnabled() && itemData.getProductionCenterId() != null) {
                    kotGroups.computeIfAbsent(itemData.getProductionCenterId(), k -> new ArrayList<>())
                            .add(new SaleItemKotWrapper(saleItem, itemData));
                }

                // 6.1 Update current_qty in DayProductionItem
                DayProductionItem dpi = itemData.getDayProductionItem();
                if (dpi != null && dpi.getCurrentQty() != null) {
                    dpi.setCurrentQty(dpi.getCurrentQty() - itemData.getItemRequest().getQty());
                    dayProductionItemRepository.save(dpi);
                    log.info("Updated current_qty for day_production_item ID {}: new qty {}",
                            dpi.getDayProductionItemId(), dpi.getCurrentQty());
                }

                // Create response DTO
                Product product = itemData.getDayProductionItem().getProduct();
                SaleItemResponseDto saleItemResponse = SaleItemResponseDto.builder()
                        .saleItemId(saleItem.getSaleItemId())
                        .dayProductionItemId(saleItem.getDayProductionItemId())
                        .productName(product.getProductName())
                        .productCode(product.getProductCode())
                        .qty(saleItem.getQty())
                        .unitPrice(itemData.getItemRequest().getUnitPrice())
                        .totalPrice(saleItem.getPrice())
                        .freeMealReason(saleItem.getFreeMealReason())
                        .bankTransferCode(saleItem.getBankTransferCode())
                        .discountId(saleItem.getDiscountId())
                        .discountName(itemData.getDiscount() != null ? itemData.getDiscount().getName() : null)
                        .paymentMethodId(saleItem.getPaymentMethodId())
                        .paymentMethodName(paymentMethod.getName())
                        .manualDiscount(itemData.getItemRequest().getManualDiscount())
                        .itemStatus(saleItem.getItemStatus().name())
                        .specialInstructions(saleItem.getSpecialInstructions())
                        .build();

                saleItemResponses.add(saleItemResponse);
            }

            // 7. Generate ProductionOrders (KOTs)
            List<String> kotNumbers = new ArrayList<>();
            if (!Boolean.TRUE.equals(requestDto.getSkipKot())) {
                for (Map.Entry<Long, List<SaleItemKotWrapper>> entry : kotGroups.entrySet()) {
                Long centerId = entry.getKey();
                List<SaleItemKotWrapper> wrappers = entry.getValue();
                
                String orderNumber = "KOT-POS-" + System.currentTimeMillis() + "-" + centerId;
                
                ProductionOrder kot = ProductionOrder.builder()
                        .orderNumber(orderNumber)
                        .orderDate(LocalDateTime.now())
                        .status(ProductionOrder.ProductionOrderStatus.PENDING)
                        .build();
                
                List<ProductionOrderItem> kotItems = wrappers.stream().map(wrapper -> {
                    Product p = wrapper.getItemData().getDayProductionItem().getProduct();
                    return ProductionOrderItem.builder()
                            .productionOrder(kot)
                            .productId(p.getId())
                            .productName(p.getProductName())
                            .plannedQuantity(wrapper.getSaleItem().getQty())
                            .unitCost(p.getUnitPrice())
                            .totalCost(p.getUnitPrice() * wrapper.getSaleItem().getQty())
                            .build();
                }).collect(Collectors.toList());
                
                kot.setProductionOrderItems(kotItems);
                productionOrderRepository.save(kot);
                kotNumbers.add(orderNumber);
                log.info("Generated KOT {} for production center ID {}", orderNumber, centerId);
                }
            }

            log.info("Sale completed successfully. Sale ID: {}, Items: {}, KOTs: {}", 
                    sale.getSaleId(), saleItems.size(), kotNumbers.size());

            // 8. Build response
            CreateSaleResponseDto.SaleDataDto saleData = CreateSaleResponseDto.SaleDataDto.builder()
                    .saleId(sale.getSaleId())
                    .billNumber(String.format("BILL-%06d", sale.getSaleId()))
                    .saleDate(sale.getSaleDate())
                    .saleTime(sale.getSaleTime())
                    .cashierId(sale.getCashierId())
                    .cashierName(cashier.getFirstName() + " " + cashier.getLastName())
                    .totalAmount(sale.getFinalTotal())
                    .items(saleItemResponses)
                    .kotNumbers(kotNumbers)
                    .build();

            return CreateSaleResponseDto.builder()
                    .success(true)
                    .message("Sale created successfully")
                    .data(saleData)
                    .build();

        } catch (Exception e) {
            log.error("Error creating sale: {}", e.getMessage(), e);
            throw e; // Re-throw to ensure transaction rollback
        }
    }

    private AuthModel validateCashier(java.util.UUID cashierId) {
        AuthModel cashier = authRepository.findById(IdUtil.uuidToBytes(cashierId))
                .orElseThrow(() -> new InvalidCashierException(cashierId.toString()));

        if (!cashier.isActive()) {
            throw new InvalidCashierException("Cashier account is inactive: " + cashierId);
        }

        return cashier;
    }

    private Integer validatePaymentMethods(List<SaleItemRequestDto> items) {
        Set<Integer> paymentMethods = items.stream()
                .map(SaleItemRequestDto::getPaymentMethodId)
                .collect(Collectors.toSet());

        if (paymentMethods.size() > 1) {
            throw new MultiplePaymentMethodsException();
        }

        Integer paymentMethodId = paymentMethods.iterator().next();
        if (!paymentMethodRepository.existsById(paymentMethodId)) {
            throw new InvalidPaymentMethodException(paymentMethodId);
        }

        return paymentMethodId;
    }


    @Override
    public GetAllSalesResponseDto getAllSales() {
        log.info("Retrieving all sales");

        try {
            List<Sale> sales = saleRepository.findAll();
            log.info("Found {} sales", sales.size());

            List<GetAllSalesResponseDto.SaleSummaryDto> saleSummaries = sales.stream()
                    .map(this::convertToSaleSummaryDto)
                    .collect(Collectors.toList());

            BigDecimal totalSalesAmount = sales.stream()
                    .map(Sale::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            return GetAllSalesResponseDto.builder()
                    .sales(saleSummaries)
                    .totalCount(sales.size())
                    .totalSalesAmount(totalSalesAmount)
                    .build();

        } catch (Exception e) {
            log.error("Error retrieving all sales: {}", e.getMessage(), e);
            throw new SaleException("Failed to retrieve sales: " + e.getMessage(), e);
        }
    }

    @Override
    public GetSaleByIdResponseDto getSaleById(Integer saleId) {
        log.info("Retrieving sale with ID: {}", saleId);

        try {
            Sale sale = saleRepository.findById(saleId)
                    .orElseThrow(() -> new SaleNotFoundException(saleId));

            log.info("Found sale with ID: {}, loading sale items", saleId);

            // Fetch sale items for this sale
            List<SaleItem> saleItems = saleItemRepository.findBySaleId(saleId);

            // Convert to detailed DTOs
            List<GetSaleByIdResponseDto.SaleItemDetailDto> itemDetails = saleItems.stream()
                    .map(this::convertToSaleItemDetailDto)
                    .collect(Collectors.toList());

            // Get cashier information
            String cashierName = getCashierName(sale.getCashierId());

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

        } catch (SaleNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error retrieving sale with ID {}: {}", saleId, e.getMessage(), e);
            throw new SaleException("Failed to retrieve sale: " + e.getMessage(), e);
        }
    }

    private GetAllSalesResponseDto.SaleSummaryDto convertToSaleSummaryDto(Sale sale) {
        String cashierName = getCashierName(sale.getCashierId());
        int itemCount = sale.getSaleItems() != null ? sale.getSaleItems().size() : 0;

        return GetAllSalesResponseDto.SaleSummaryDto.builder()
                .saleId(sale.getSaleId())
                .saleDate(sale.getSaleDate())
                .saleTime(sale.getSaleTime())
                .cashierId(sale.getCashierId())
                .cashierName(cashierName)
                .totalAmount(sale.getFinalTotal()) // Use finalTotal for actual revenue
                .itemCount(itemCount)
                .invoicePrinted(sale.getInvoicePrinted())
                .build();
    }

    private GetSaleByIdResponseDto.SaleItemDetailDto convertToSaleItemDetailDto(SaleItem saleItem) {
        // Get product information
        String productName = "Unknown Product";
        String productCode = "N/A";

        if (saleItem.getDayProductionItem() != null && saleItem.getDayProductionItem().getProduct() != null) {
            Product product = saleItem.getDayProductionItem().getProduct();
            productName = product.getProductName();
            productCode = product.getProductCode();
        }

        // Get discount information
        String discountName = null;
        BigDecimal discountPercentage = null;

        if (saleItem.getDiscount() != null) {
            discountName = saleItem.getDiscount().getName();
            if (saleItem.getDiscount().getDiscountType() == DiscountType.PERCENTAGE) {
                discountPercentage = saleItem.getDiscount().getDiscountValue();
            }
        }

        // Get payment method information
        String paymentMethodName = "Unknown Payment Method";
        if (saleItem.getPaymentMethod() != null) {
            paymentMethodName = saleItem.getPaymentMethod().getName();
        }

        // Get cashier information
        String cashierName = getCashierName(saleItem.getCashierId());

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

    @Override
    public DailyDiscountSummaryDto getDailyDiscountSummary() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        
        List<DiscountAudit> todayAudits = discountAuditRepository.findByCreatedAtBetween(startOfDay, endOfDay);
        
        int count = todayAudits.size();
        BigDecimal totalValue = todayAudits.stream()
                .map(DiscountAudit::getDiscountValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        List<Sale> todaySales = saleRepository.findAll().stream()
                .filter(s -> s.getSaleDate().equals(LocalDate.now()))
                .collect(Collectors.toList());
        
        BigDecimal totalSalesSubtotal = todaySales.stream()
                .map(Sale::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
                
        Double avgPercentage = 0.0;
        if (totalSalesSubtotal.compareTo(BigDecimal.ZERO) > 0) {
            avgPercentage = totalValue.doubleValue() / totalSalesSubtotal.doubleValue() * 100.0;
        }

        return DailyDiscountSummaryDto.builder()
                .totalDiscountCount(count)
                .totalDiscountValue(totalValue)
                .averageDiscountPercentage(Math.round(avgPercentage * 100.0) / 100.0)
                .build();
    }
    
    @Override
    public com.plover.backerymanagmentsystem.pos.dto.PosDashboardStatsResponseDto getDashboardStats(Long outletId) {
        LocalDate today = LocalDate.now();
        
        // If outletId is null, we fetch all sales for today (Admin view)
        List<Sale> todaySales;
        if (outletId != null) {
            todaySales = saleRepository.findBySaleDateAndOutletIdOrderBySaleTimeDesc(today, outletId);
        } else {
            todaySales = saleRepository.findBySaleDate(today);
        }

        BigDecimal totalSalesToday = BigDecimal.ZERO;
        BigDecimal cashSalesToday = BigDecimal.ZERO;
        BigDecimal cardSalesToday = BigDecimal.ZERO;
        BigDecimal returnsToday = BigDecimal.ZERO; // Placeholder: Need a ReturnRepository/Model to track this

        for (Sale sale : todaySales) {
            BigDecimal saleAmount = sale.getFinalTotal() != null ? sale.getFinalTotal() : BigDecimal.ZERO;
            totalSalesToday = totalSalesToday.add(saleAmount);
            
            if (sale.getPaymentType() == PaymentCategory.CASH) {
                cashSalesToday = cashSalesToday.add(saleAmount);
            } else if (sale.getPaymentType() == PaymentCategory.CARD) {
                cardSalesToday = cardSalesToday.add(saleAmount);
            }
        }

        // Recent transactions (top 5)
        List<GetAllSalesResponseDto.SaleSummaryDto> recentTransactions = todaySales.stream()
                .limit(5)
                .map(this::convertToSaleSummaryDto)
                .collect(Collectors.toList());

        // Low stock items from today's production - filtered by outlet
        List<DayProductionItem> lowStockDpis = dayProductionItemRepository.findLowStockItems(today, 5, outletId);

        List<com.plover.backerymanagmentsystem.pos.dto.PosDashboardStatsResponseDto.LowStockItemDto> lowStockItems = lowStockDpis.stream()
                .map(dpi -> com.plover.backerymanagmentsystem.pos.dto.PosDashboardStatsResponseDto.LowStockItemDto.builder()
                        .name(dpi.getProduct().getProductName())
                        .current(dpi.getCurrentQty())
                        .minimum(5)
                        .status(dpi.getCurrentQty() != null && dpi.getCurrentQty() <= 2 ? "critical" : "warning")
                        .build())
                .collect(Collectors.toList());

        return com.plover.backerymanagmentsystem.pos.dto.PosDashboardStatsResponseDto.builder()
                .totalSalesToday(totalSalesToday)
                .totalOrdersToday(todaySales.size())
                .cashSalesToday(cashSalesToday)
                .cardSalesToday(cardSalesToday)
                .returnsToday(returnsToday)
                .recentTransactions(recentTransactions)
                .lowStockItems(lowStockItems)
                .build();
    }

    private String getCashierName(java.util.UUID cashierId) {
        try {
            AuthModel cashier = authRepository.findById(IdUtil.uuidToBytes(cashierId)).orElse(null);
            if (cashier != null) {
                return cashier.getFirstName() + " " + cashier.getLastName();
            }
            return "Unknown Cashier";
        } catch (Exception e) {
            log.warn("Could not retrieve cashier name for ID: {}", cashierId);
            return "Unknown Cashier";
        }
    }

    @Override
    @Transactional
    public StandaloneKotResponseDto createStandaloneKot(StandaloneKotRequestDto dto) {
        // Resolve outlet from current authenticated cashier
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentOutletId = null;
        if (auth != null && auth.getPrincipal() instanceof AuthModel) {
            currentOutletId = ((AuthModel) auth.getPrincipal()).getOutletId();
        }

        // Validate production center — accept outlet MPC or any Main PC
        Long resolvedPcId = dto.getProductionCenterId();
        java.util.Optional<OutletProductionCenter> mpcOpt = outletProductionCenterRepository.findById(resolvedPcId);
        if (mpcOpt.isPresent()) {
            if (currentOutletId != null && !mpcOpt.get().getOutlet().getOutletId().equals(currentOutletId)) {
                throw new RuntimeException("Production center does not belong to your outlet");
            }
        } else {
            // Try Main PC
            boolean mainPcExists = productionCenterRepository.existsById(resolvedPcId);
            if (!mainPcExists) {
                throw new RuntimeException("Production center not found with id: " + resolvedPcId);
            }
        }

        // Resolve DayProductionItem -> product
        DayProductionItem dpi = dayProductionItemRepository.findById(dto.getDayProductionItemId())
                .orElseThrow(() -> new RuntimeException("DayProductionItem not found: " + dto.getDayProductionItemId()));
        Product product = dpi.getProduct();

        String orderNumber = "KOT-POS-" + System.currentTimeMillis() + "-" + resolvedPcId;

        ProductionOrder kot = ProductionOrder.builder()
                .orderNumber(orderNumber)
                .orderDate(LocalDateTime.now())
                .status(ProductionOrder.ProductionOrderStatus.PENDING)
                .build();

        ProductionOrderItem kotItem = ProductionOrderItem.builder()
                .productionOrder(kot)
                .productId(product.getId())
                .productName(product.getProductName())
                .plannedQuantity(dto.getQty())
                .unitCost(product.getUnitPrice())
                .totalCost(product.getUnitPrice() != null ? product.getUnitPrice() * dto.getQty() : 0.0)
                .productionCenterId(resolvedPcId)
                .build();

        kot.setProductionOrderItems(new ArrayList<>(java.util.Arrays.asList(kotItem)));
        ProductionOrder saved = productionOrderRepository.save(kot);
        log.info("Standalone KOT {} created for production center {}", orderNumber, resolvedPcId);

        return StandaloneKotResponseDto.builder()
                .kotId(saved.getId())
                .orderNumber(saved.getOrderNumber())
                .status(saved.getStatus().name())
                .build();
    }

    @Override
    @Transactional
    public void updateInvoicePrintedStatus(Integer saleId, Boolean invoicePrinted) {
        Sale sale = saleRepository.findById(saleId)
                .orElseThrow(() -> new SaleNotFoundException(saleId));
        sale.setInvoicePrinted(invoicePrinted);
        saleRepository.save(sale);
        log.info("Updated invoice printed status for sale ID: {} to {}", saleId, invoicePrinted);
    }

    // Internal data class for processing
    @lombok.Data
    @lombok.Builder
    private static class SaleItemData {

        private SaleItemRequestDto itemRequest;
        private DayProductionItem dayProductionItem;
        private Product product;
        private Discount discount;
        private Promotion appliedPromotion;
        private BigDecimal appliedDiscount;
        private BigDecimal totalPrice;
        private Boolean isKotEnabled;
        private Long productionCenterId;
    }

    @lombok.Value
    @lombok.AllArgsConstructor
    private static class SaleItemKotWrapper {
        SaleItem saleItem;
        SaleItemData itemData;
    }
}
