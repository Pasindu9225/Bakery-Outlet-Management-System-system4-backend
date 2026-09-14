package com.plover.backerymanagmentsystem.pos.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.plover.backerymanagmentsystem.pos.dto.PaymentMethodResponseDto;
import com.plover.backerymanagmentsystem.pos.model.PaymentMethod;
import com.plover.backerymanagmentsystem.pos.repository.PaymentMethodRepository;
import com.plover.backerymanagmentsystem.pos.service.PaymentMethodService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMethodServiceImpl implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    @Override
    public List<PaymentMethodResponseDto> getAllPaymentMethods() {
        log.info("Fetching all payment methods");

        List<PaymentMethod> paymentMethods = paymentMethodRepository.findAllByOrderByNameAsc();

        List<PaymentMethodResponseDto> result = paymentMethods.stream()
                .map(PaymentMethodResponseDto::fromEntity)
                .collect(Collectors.toList());

        log.info("Found {} payment methods", result.size());
        return result;
    }
}
