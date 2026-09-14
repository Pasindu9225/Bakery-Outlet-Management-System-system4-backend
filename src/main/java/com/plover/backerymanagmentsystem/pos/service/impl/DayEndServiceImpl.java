package com.plover.backerymanagmentsystem.pos.service.impl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.pos.dto.CategoryTotalDto;
import com.plover.backerymanagmentsystem.pos.dto.DayEndSummaryResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.ShiftClosureRequestDto;
import com.plover.backerymanagmentsystem.pos.exception.DayEndException;
import com.plover.backerymanagmentsystem.pos.model.PaymentCategory;
import com.plover.backerymanagmentsystem.pos.model.Sale;
import com.plover.backerymanagmentsystem.pos.model.SaleItemStatus;
import com.plover.backerymanagmentsystem.pos.model.ShiftClosure;
import com.plover.backerymanagmentsystem.pos.repository.ReturnRepository;
import com.plover.backerymanagmentsystem.pos.repository.SaleRepository;
import com.plover.backerymanagmentsystem.pos.repository.ShiftClosureRepository;
import com.plover.backerymanagmentsystem.pos.service.DayEndService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DayEndServiceImpl implements DayEndService {

    private final SaleRepository saleRepository;
    private final ReturnRepository returnRepository;
    private final ShiftClosureRepository shiftClosureRepository;
    private final com.plover.backerymanagmentsystem.pos.repository.CashFloatRepository cashFloatRepository;
    private final com.plover.backerymanagmentsystem.core.login.repository.AuthRepository authRepository;
    private final com.plover.backerymanagmentsystem.pos.repository.PosWaiterItemRepository posWaiterItemRepository;
    private final com.plover.backerymanagmentsystem.pos.repository.PosTableItemRepository posTableItemRepository;

    @Override
    @Transactional
    public void processDayEnd(ShiftClosureRequestDto request) {
        log.info("Processing Day-End closure for cashier: {}, date: {}", request.getCashierId(), request.getClosureDate());

        // 0. Verify Cashier PIN
        byte[] cashierIdBytes = com.plover.backerymanagmentsystem.admin.util.IdUtil.uuidToBytes(request.getCashierId());
        com.plover.backerymanagmentsystem.core.login.model.AuthModel cashier = authRepository.findById(cashierIdBytes)
                .orElseThrow(() -> new DayEndException("Cashier not found."));
        
        String expectedPin = cashier.getVerificationCode() != null ? cashier.getVerificationCode() : "0000";
        if (!expectedPin.equals(request.getCashierPin())) {
            log.warn("PIN verification failed for cashier: {}. Expected: {}, Provided: {}", request.getCashierId(), expectedPin, request.getCashierPin());
            throw new DayEndException("Invalid cashier PIN. Verification failed.");
        }

        // 1. Check if already closed (Idempotency)
        boolean alreadyClosed;
        if (Boolean.TRUE.equals(request.getShiftOnly())) {
            alreadyClosed = shiftClosureRepository.findByClosureDateAndOutletIdAndCashierId(
                    request.getClosureDate(), request.getOutletId(), request.getCashierId()).isPresent();
        } else {
            alreadyClosed = shiftClosureRepository.findLockedDayEnd(
                    request.getClosureDate(), request.getOutletId()).isPresent();
        }

        if (alreadyClosed) {
            log.info("Day end / Shift closure has already been processed for outlet {} on {}.", 
                request.getOutletId(), request.getClosureDate());
            return;
        }

        // Validation 2: Ensure no pending transactions
        List<SaleItemStatus> pendingStatuses = Arrays.asList(SaleItemStatus.PENDING, SaleItemStatus.PREPARING);
        boolean hasPending = saleRepository.hasPendingTransactionsForCashierAndDate(
                request.getClosureDate(), request.getCashierId(), pendingStatuses);
                
        if (hasPending) {
            throw new DayEndException("Cannot close day. There are pending or preparing transactions.");
        }

        long pendingWaiterCount = posWaiterItemRepository.countUnpaidWaiterItems() + posTableItemRepository.countUnpaidTableItems();
        if (pendingWaiterCount > 0) {
            throw new DayEndException("Cannot close day/shift. There are " + pendingWaiterCount + " pending waiter/table items that must be settled first.");
        }

        // Get Expected Summary
        DayEndSummaryResponseDto expectedSummary = getDayEndSummary(request.getClosureDate(), request.getCashierId());

        BigDecimal openingFloat = expectedSummary.getOpeningFloat() != null ? expectedSummary.getOpeningFloat() : BigDecimal.ZERO;
        
        // FR-POS-16: Verify Closing Balance >= Opening Balance
        if (request.getActualCash() != null && request.getActualCash().compareTo(openingFloat) < 0) {
            throw new DayEndException("Closing cash balance cannot be less than the opening float of " + openingFloat);
        }

        // 3. Calculate Variance
        BigDecimal currentSystemTotal = expectedSummary.getExpectedCash().add(expectedSummary.getExpectedCard())
                .add(expectedSummary.getExpectedUber()).add(expectedSummary.getExpectedPickme());
                
        BigDecimal actCash = request.getActualCash() != null ? request.getActualCash() : BigDecimal.ZERO;
        BigDecimal actCard = request.getActualCard() != null ? request.getActualCard() : BigDecimal.ZERO;
        BigDecimal actUber = request.getActualUber() != null ? request.getActualUber() : BigDecimal.ZERO;
        BigDecimal actPickme = request.getActualPickme() != null ? request.getActualPickme() : BigDecimal.ZERO;
        
        BigDecimal cashierTotal = actCash.add(actCard).add(actUber).add(actPickme);
        
        BigDecimal variance = currentSystemTotal.subtract(cashierTotal);

        // 4. Save ShiftClosure Record
        ShiftClosure shiftClosure = ShiftClosure.builder()
                .cashierId(request.getCashierId())
                .outletId(request.getOutletId())
                .closureDate(request.getClosureDate())
                .isLocked(!Boolean.TRUE.equals(request.getShiftOnly()))
                .actualCash(request.getActualCash())
                .actualCard(request.getActualCard())
                .actualUber(request.getActualUber())
                .actualPickme(request.getActualPickme())
                .expectedCash(expectedSummary.getExpectedCash())
                .expectedCard(expectedSummary.getExpectedCard())
                .expectedUber(expectedSummary.getExpectedUber())
                .expectedPickme(expectedSummary.getExpectedPickme())
                .variance(variance)
                .cashDenominations(request.getCashDenominations())
                .build();

        shiftClosureRepository.save(shiftClosure);

        // FR-POS-16: Close the cash float
        cashFloatRepository.findByCashierIdAndFloatDate(request.getCashierId(), request.getClosureDate())
                .ifPresent(f -> {
                    f.setIsClosed(true);
                    f.setClosingTimestamp(LocalDateTime.now());
                    cashFloatRepository.save(f);
                });

        // 5. Lock Data
        List<Sale> salesToLock = saleRepository.findBySaleDateAndCashierId(request.getClosureDate(), request.getCashierId());
        salesToLock.forEach(sale -> sale.setIsLocked(true));
        saleRepository.saveAll(salesToLock);
        
        log.info("Day end processing completed successfully for Cashier ID: {}", request.getCashierId());
    }

    @Override
    public DayEndSummaryResponseDto getDayEndSummary(LocalDate closureDate, UUID cashierId) {
        log.info("Generating day end summary for Cashier: {} on Date: {}", cashierId, closureDate);
        
        com.plover.backerymanagmentsystem.pos.model.CashFloat cashFloat = cashFloatRepository.findByCashierIdAndFloatDate(cashierId, closureDate)
                .orElseThrow(() -> {
                    log.warn("No opening float found for Cashier: {} on Date: {}", cashierId, closureDate);
                    return new DayEndException("No opening float found for this cashier today.");
                });

        BigDecimal expectedCash = BigDecimal.ZERO;
        BigDecimal expectedCard = BigDecimal.ZERO;
        BigDecimal expectedUber = BigDecimal.ZERO;
        BigDecimal expectedPickme = BigDecimal.ZERO;
        BigDecimal totalFreeMealsAmount = BigDecimal.ZERO;

        log.debug("Fetching sales totals from repository...");
        List<CategoryTotalDto> salesTotals = saleRepository.calculateExpectedTotalsByCashierAndDate(closureDate, cashierId);
        log.debug("Found {} sales total categories", salesTotals != null ? salesTotals.size() : 0);

        if (salesTotals != null) {
            for (CategoryTotalDto totalItem : salesTotals) {
                BigDecimal amount = totalItem.getTotalAmount() != null ? totalItem.getTotalAmount() : BigDecimal.ZERO;
                PaymentCategory category = totalItem.getCategory();
                
                log.debug("Processing category: {}, amount: {}", category, amount);
                
                if (category == PaymentCategory.CASH) expectedCash = expectedCash.add(amount);
                else if (category == PaymentCategory.CARD) expectedCard = expectedCard.add(amount);
                else if (category == PaymentCategory.UBER) expectedUber = expectedUber.add(amount);
                else if (category == PaymentCategory.PICKME) expectedPickme = expectedPickme.add(amount);
                else if (category == PaymentCategory.FREE_MEAL) totalFreeMealsAmount = totalFreeMealsAmount.add(amount);
                else if (category == PaymentCategory.BANK_TRANSFER) {
                    log.info("Bank transfer sale found: Rs. {}. Note: This is currently not tracked in summary fields.", amount);
                }
            }
        }

        LocalDateTime startOfDay = closureDate.atStartOfDay();
        LocalDateTime endOfDay = closureDate.plusDays(1).atStartOfDay();
        
        log.debug("Fetching refund totals from repository for range {} to {}", startOfDay, endOfDay);
        List<CategoryTotalDto> returnTotals = returnRepository.calculateRefundTotalsByCashierAndDate(
                startOfDay, endOfDay, cashierId);
        log.debug("Found {} refund total categories", returnTotals != null ? returnTotals.size() : 0);

        BigDecimal totalCashRefund = BigDecimal.ZERO;
        if (returnTotals != null) {
            for (CategoryTotalDto refundItem : returnTotals) {
                BigDecimal amount = refundItem.getTotalAmount() != null ? refundItem.getTotalAmount() : BigDecimal.ZERO;
                if (refundItem.getCategory() == PaymentCategory.CASH) {
                    totalCashRefund = totalCashRefund.add(amount);
                }
            }
        }

        // Calculate Expected Cash: Opening Float + Cash Sales - Cash Refunds
        BigDecimal openingBal = cashFloat.getOpeningBalance() != null ? cashFloat.getOpeningBalance() : BigDecimal.ZERO;
        expectedCash = expectedCash.add(openingBal).subtract(totalCashRefund);

        // Fetch sales details for summary
        List<com.plover.backerymanagmentsystem.pos.model.Sale> cashierSales = saleRepository.findBySaleDateAndCashierId(closureDate, cashierId);
        int totalBills = 0;
        BigDecimal totalSalesAmt = BigDecimal.ZERO;
        long loyaltyCustomersCount = 0;
        
        if (cashierSales != null) {
            totalBills = cashierSales.size();
            totalSalesAmt = cashierSales.stream()
                    .map(s -> s.getFinalTotal() != null ? s.getFinalTotal() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            loyaltyCustomersCount = cashierSales.stream()
                    .map(com.plover.backerymanagmentsystem.pos.model.Sale::getCustomerId)
                    .filter(java.util.Objects::nonNull)
                    .distinct()
                    .count();
        }

        long pendingWaiterCount = posWaiterItemRepository.countUnpaidWaiterItems() + posTableItemRepository.countUnpaidTableItems();

        log.info("Summary generated: Expected Cash={}, Expected Card={}, Total Bills={}, Total Sales Amount={}, Pending Waiter Items={}", expectedCash, expectedCard, totalBills, totalSalesAmt, pendingWaiterCount);

        return DayEndSummaryResponseDto.builder()
                .closureDate(closureDate)
                .cashierId(cashierId)
                .openingFloat(openingBal)
                .expectedCash(expectedCash)
                .expectedCard(expectedCard)
                .expectedUber(expectedUber)
                .expectedPickme(expectedPickme)
                .totalCashRefunds(totalCashRefund)
                .totalFreeMealsAmount(totalFreeMealsAmount)
                .totalBills(totalBills)
                .totalSalesAmount(totalSalesAmt)
                .loyaltyCustomersCount(loyaltyCustomersCount)
                .pendingWaiterItemsCount(pendingWaiterCount)
                .build();
    }
}
