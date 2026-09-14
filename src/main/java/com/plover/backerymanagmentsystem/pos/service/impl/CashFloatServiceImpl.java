package com.plover.backerymanagmentsystem.pos.service.impl;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.pos.dto.CashFloatStatusResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.OpenCashFloatRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.UpdateCashFloatRequestDto;
import com.plover.backerymanagmentsystem.pos.exception.DayEndException;
import com.plover.backerymanagmentsystem.pos.model.CashFloat;
import com.plover.backerymanagmentsystem.pos.repository.CashFloatRepository;
import com.plover.backerymanagmentsystem.pos.service.CashFloatService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashFloatServiceImpl implements CashFloatService {

    private final CashFloatRepository cashFloatRepository;

    @Override
    @Transactional
    public void openFloat(OpenCashFloatRequestDto request) {
        log.info("Opening cash float for cashier: {}, amount: {}", request.getCashierId(), request.getOpeningBalance());
        
        cashFloatRepository.findByCashierIdAndFloatDate(request.getCashierId(), LocalDate.now())
                .ifPresent(f -> {
                    throw new DayEndException("Opening float already declared for today.");
                });

        CashFloat cashFloat = CashFloat.builder()
                .cashierId(request.getCashierId())
                .outletId(request.getOutletId())
                .openingBalance(request.getOpeningBalance())
                .floatDate(LocalDate.now())
                .isClosed(false)
                .build();

        cashFloatRepository.save(cashFloat);
    }

    @Override
    @Transactional
    public void updateFloat(UpdateCashFloatRequestDto request) {
        log.info("Manager update for cash float - cashier: {}, new amount: {}", request.getCashierId(), request.getNewOpeningBalance());
        
        CashFloat cashFloat = cashFloatRepository.findByCashierIdAndFloatDate(request.getCashierId(), LocalDate.now())
                .orElseThrow(() -> new DayEndException("No opening float record found for today to update."));

        if (cashFloat.getIsClosed()) {
            throw new DayEndException("Cannot update float. The day is already closed.");
        }

        cashFloat.setOpeningBalance(request.getNewOpeningBalance());
        cashFloatRepository.save(cashFloat);
    }

    @Override
    public CashFloatStatusResponseDto getStatus(UUID cashierId) {
        return cashFloatRepository.findByCashierIdAndFloatDate(cashierId, LocalDate.now())
                .map(f -> CashFloatStatusResponseDto.builder()
                        .isOpened(true)
                        .isClosed(f.getIsClosed())
                        .openingBalance(f.getOpeningBalance())
                        .openingTimestamp(f.getOpeningTimestamp())
                        .build())
                .orElse(CashFloatStatusResponseDto.builder()
                        .isOpened(false)
                        .isClosed(false)
                        .build());
    }

    @Override
    public boolean isFloatOpen(UUID cashierId) {
        return cashFloatRepository.findByCashierIdAndFloatDateAndIsClosed(cashierId, LocalDate.now(), false).isPresent();
    }
}
