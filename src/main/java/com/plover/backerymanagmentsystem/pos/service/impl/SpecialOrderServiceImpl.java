package com.plover.backerymanagmentsystem.pos.service.impl;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
import com.plover.backerymanagmentsystem.manager.model.Customer;
import com.plover.backerymanagmentsystem.manager.model.MiniStoreItem;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.repository.CustomerRepository;
import com.plover.backerymanagmentsystem.manager.repository.MiniStoreItemRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.pos.dto.*;
import com.plover.backerymanagmentsystem.pos.model.SpecialOrder;
import com.plover.backerymanagmentsystem.pos.model.SpecialOrderItem;
import com.plover.backerymanagmentsystem.pos.model.SpecialOrderStatus;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.SpecialOrderItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.SpecialOrderRepository;
import com.plover.backerymanagmentsystem.pos.service.SaleService;
import com.plover.backerymanagmentsystem.pos.service.SpecialOrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SpecialOrderServiceImpl implements SpecialOrderService {

    private final SpecialOrderRepository specialOrderRepository;
    private final SpecialOrderItemRepository specialOrderItemRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final MiniStoreItemRepository miniStoreItemRepository;
    private final DayProductionItemRepository dayProductionItemRepository;
    private final SaleService saleService;
    private final AuthRepository authRepository;
    private final com.plover.backerymanagmentsystem.pos.repository.PaymentMethodRepository paymentMethodRepository;

    @Override
    public SpecialOrderResponseDto initiateOrder(CreateSpecialOrderRequestDto requestDto) {
        log.info("Initiating special order for customer: {}", requestDto.getCustomerName());

        // 1. Lookup or Create Customer
        Customer customer = customerRepository.findByContactNumber(requestDto.getCustomerContact())
                .orElseGet(() -> {
                    log.info("Creating new permanent customer record for: {}", requestDto.getCustomerName());
                    Customer newCustomer = Customer.builder()
                            .name(requestDto.getCustomerName())
                            .contactNumber(requestDto.getCustomerContact())
                            .email(requestDto.getCustomerEmail())
                            .address(requestDto.getCustomerAddress())
                            .build();
                    return customerRepository.save(newCustomer);
                });

        // 2. Resolve Manager (Required only if advance amount > 0)
        AuthModel manager = null;
        if (requestDto.getAdvanceAmount() != null && requestDto.getAdvanceAmount().compareTo(BigDecimal.ZERO) > 0) {
            manager = verifyManagerCode(requestDto.getManagerVerificationCode());
        }

        // 3. Initial Order Setup
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal advanceAmt = requestDto.getAdvanceAmount() != null ? requestDto.getAdvanceAmount() : BigDecimal.ZERO;
        SpecialOrder specialOrder = SpecialOrder.builder()
                .customer(customer)
                .advanceAmount(advanceAmt)
                .deliveryDate(requestDto.getDeliveryDate())
                .notes(requestDto.getNotes())
                .status(SpecialOrderStatus.ADVANCE_PAID)
                .cashierId(requestDto.getCashierId())
                .outletId(requestDto.getOutletId())
                .managerVerificationCode(requestDto.getManagerVerificationCode())
                .verifiedByManagerName(manager != null ? (manager.getFirstName() + " " + manager.getLastName()) : "N/A")
                .build();

        // 3. Process Items and calculate total
        List<SpecialOrderItem> orderItems = requestDto.getItems().stream().map(itemDto -> {
            // Safety Net: Resolve Product ID. 
            // The frontend might send dayProductionItemId instead of productId.
            Product product = productRepository.findById(itemDto.getProductId())
                    .orElseGet(() -> {
                        log.info("Product ID {} not found in catalog. Attempting to resolve via DayProductionItem.", itemDto.getProductId());
                        return dayProductionItemRepository.findById(itemDto.getProductId().intValue())
                                .map(dpi -> dpi.getProduct())
                                .orElseThrow(() -> new RuntimeException("Product not found: " + itemDto.getProductId()));
                    });
            
            BigDecimal itemSubtotal = itemDto.getUnitPrice().multiply(BigDecimal.valueOf(itemDto.getQuantity()));
            return SpecialOrderItem.builder()
                    .specialOrder(specialOrder)
                    .product(product)
                    .quantity(itemDto.getQuantity())
                    .unitPrice(itemDto.getUnitPrice())
                    .subtotal(itemSubtotal)
                    .specialInstructions(itemDto.getSpecialInstructions())
                    .build();
        }).collect(Collectors.toList());

        totalAmount = orderItems.stream()
                .map(SpecialOrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        specialOrder.setTotalAmount(totalAmount);
        specialOrder.setBalanceAmount(totalAmount.subtract(requestDto.getAdvanceAmount()));
        
        if (specialOrder.getBalanceAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Advance payment cannot exceed total order amount");
        }

        // 4. Save Order and Items
        SpecialOrder savedOrder = specialOrderRepository.save(specialOrder);
        specialOrderItemRepository.saveAll(orderItems);
        savedOrder.setItems(orderItems);

        return mapToResponseDto(savedOrder);
    }

    @Override
    public SpecialOrderResponseDto getOrderById(Long id) {
        SpecialOrder order = specialOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Special Order not found with ID: " + id));
        return mapToResponseDto(order);
    }

    @Override
    public List<SpecialOrderResponseDto> getPendingOrders() {
        return specialOrderRepository.findByStatus(SpecialOrderStatus.ADVANCE_PAID).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<SpecialOrderResponseDto> getOrdersByDeliveryDate(LocalDate date) {
        return specialOrderRepository.findByDeliveryDate(date).stream()
                .map(this::mapToResponseDto)
                .collect(Collectors.toList());
    }

    @Override
    public SpecialOrderResponseDto recordPayment(Long id, SpecialOrderPaymentRequestDto requestDto) {
        SpecialOrder order = specialOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Special Order not found"));

        BigDecimal newAdvance = order.getAdvanceAmount().add(requestDto.getAmount());
        BigDecimal newBalance = order.getTotalAmount().subtract(newAdvance);

        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Total payments exceed order amount");
        }

        order.setAdvanceAmount(newAdvance);
        order.setBalanceAmount(newBalance);
        
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            order.setStatus(SpecialOrderStatus.AWAITING_APPROVAL);
        }

        return mapToResponseDto(specialOrderRepository.save(order));
    }

    @Override
    public SpecialOrderResponseDto approveAndCloseOrder(Long id, UUID managerId) {
        log.info("Manager approval requested for order {} by manager {}", id, managerId);
        
        SpecialOrder order = specialOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Special Order not found"));

        if (order.getBalanceAmount().compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Cannot close order: Balance remaining Rs. " + order.getBalanceAmount());
        }

        // 1. Manager role verification (Simple check for now)
        AuthModel manager = authRepository.findById(IdUtil.uuidToBytes(managerId))
                .orElseThrow(() -> new RuntimeException("Manager not found"));

        if (!manager.isActive()) {
            throw new RuntimeException("Manager account is inactive");
        }

        // 2. Inventory deduction from General Catalog (MiniStoreItem)
        for (SpecialOrderItem item : order.getItems()) {
            MiniStoreItem stockItem = miniStoreItemRepository.findByProductIdAndOutletId(
                    item.getProduct().getId(), order.getOutletId())
                    .orElseThrow(() -> new RuntimeException("Stock record not found for product: " + item.getProduct().getProductName()));
            
            if (stockItem.getSystemQty().compareTo(BigDecimal.valueOf(item.getQuantity())) < 0) {
                log.warn("Insufficient stock for product {} in catalog. Proceeding anyway as per Special Order rules.", item.getProduct().getProductName());
            }
            
            stockItem.setSystemQty(stockItem.getSystemQty().subtract(BigDecimal.valueOf(item.getQuantity())));
            miniStoreItemRepository.save(stockItem);
            log.info("Deducted {} from catalog stock for product {}", item.getQuantity(), item.getProduct().getProductName());
        }

        // 3. Create Sale record for accounting
        // Resolve payment method ID based on whether the customer has credit allowed
        Integer paymentMethodId = null;
        boolean isCreditCustomer = order.getCustomer() != null && Boolean.TRUE.equals(order.getCustomer().getIsCreditAllowed());
        if (isCreditCustomer) {
            paymentMethodId = paymentMethodRepository.findAll().stream()
                    .filter(m -> com.plover.backerymanagmentsystem.pos.model.PaymentCategory.CREDIT.equals(m.getCategory()))
                    .map(com.plover.backerymanagmentsystem.pos.model.PaymentMethod::getPaymentMethodId)
                    .findFirst()
                    .orElse(null);
        }
        
        // Fallback to CASH if not credit customer or if CREDIT method is not found
        if (paymentMethodId == null) {
            paymentMethodId = paymentMethodRepository.findAll().stream()
                    .filter(m -> com.plover.backerymanagmentsystem.pos.model.PaymentCategory.CASH.equals(m.getCategory()))
                    .map(com.plover.backerymanagmentsystem.pos.model.PaymentMethod::getPaymentMethodId)
                    .findFirst()
                    .orElse(6); // Default fallback Cash ID
        }

        final Integer finalPaymentMethodId = paymentMethodId;

        CreateSaleRequestDto saleRequest = CreateSaleRequestDto.builder()
                .cashierId(order.getCashierId())
                .outletId(order.getOutletId())
                .customerPhoneNumber(order.getCustomer() != null ? order.getCustomer().getContactNumber() : null)
                .items(order.getItems().stream().map(item -> SaleItemRequestDto.builder()
                        .productId(item.getProduct().getId())
                        .qty(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .paymentMethodId(finalPaymentMethodId)
                        .build()).collect(Collectors.toList()))
                .build();
        
        saleService.createSale(saleRequest);
        log.info("Standard sale record created for finalized special order {} with paymentMethodId {}", id, paymentMethodId);

        order.setStatus(SpecialOrderStatus.COMPLETED);
        order.setApproverId(managerId);
        
        return mapToResponseDto(specialOrderRepository.save(order));
    }

    @Override
    public SpecialOrderResponseDto cancelOrder(Long id, String reason) {
        SpecialOrder order = specialOrderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Special Order not found"));
        order.setStatus(SpecialOrderStatus.CANCELLED);
        order.setNotes(order.getNotes() + " | Cancellation Reason: " + reason);
        return mapToResponseDto(specialOrderRepository.save(order));
    }

    @Override
    public AuthModel verifyManagerCode(String code) {
        return authRepository.findByVerificationCode(code)
                .filter(u -> u.isActive() && "10".equals(u.getRoleId()))
                .orElseThrow(() -> new RuntimeException("Invalid manager verification code"));
    }

    private SpecialOrderResponseDto mapToResponseDto(SpecialOrder order) {
        return SpecialOrderResponseDto.builder()
                .id(order.getId())
                .customerId(order.getCustomer().getId())
                .customerName(order.getCustomer().getName())
                .customerContact(order.getCustomer().getContactNumber())
                .totalAmount(order.getTotalAmount())
                .advanceAmount(order.getAdvanceAmount())
                .balanceAmount(order.getBalanceAmount())
                .deliveryDate(order.getDeliveryDate())
                .status(order.getStatus())
                .notes(order.getNotes())
                .cashierId(order.getCashierId())
                .approverId(order.getApproverId())
                .outletId(order.getOutletId())
                .managerVerificationCode(order.getManagerVerificationCode())
                .verifiedByManagerName(order.getVerifiedByManagerName())
                .items(order.getItems().stream().map(item -> SpecialOrderItemResponseDto.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getProductName())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .specialInstructions(item.getSpecialInstructions())
                        .build()).collect(Collectors.toList()))
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
