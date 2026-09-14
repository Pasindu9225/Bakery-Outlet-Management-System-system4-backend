package com.plover.backerymanagmentsystem.pos.service.impl;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.core.login.repository.AuthRepository;
import com.plover.backerymanagmentsystem.manager.model.MiniStoreItem;
import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.repository.MiniStoreItemRepository;
import com.plover.backerymanagmentsystem.manager.repository.OutletRepository;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.pos.dto.OutletReturnDetailDto;
import com.plover.backerymanagmentsystem.pos.dto.OutletReturnRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.OutletReturnResponseDto;
import com.plover.backerymanagmentsystem.pos.model.DayProductionItem;
import com.plover.backerymanagmentsystem.pos.model.OutletReturn;
import com.plover.backerymanagmentsystem.pos.model.OutletReturnItem;
import com.plover.backerymanagmentsystem.pos.model.OutletReturnStatus;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.OutletReturnItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.OutletReturnRepository;
import com.plover.backerymanagmentsystem.pos.service.OutletReturnService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import com.plover.backerymanagmentsystem.admin.util.IdUtil;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutletReturnServiceImpl implements OutletReturnService {

    private final OutletReturnRepository outletReturnRepository;
    private final OutletReturnItemRepository outletReturnItemRepository;
    private final DayProductionItemRepository dayProductionItemRepository;
    private final MiniStoreItemRepository miniStoreItemRepository;
    private final ProductRepository productRepository;
    private final OutletRepository outletRepository;
    private final AuthRepository authRepository;

    @Override
    @Transactional
    public OutletReturnResponseDto initiateReturn(OutletReturnRequestDto requestDto) {
        log.info("Initiating return for outlet ID: {}", requestDto.getOutletId());

        String returnNoteId = generateReturnNoteId();

        OutletReturn outletReturn = OutletReturn.builder()
                .returnNoteId(returnNoteId)
                .outletId(requestDto.getOutletId())
                .reason(requestDto.getReason())
                .remarks(requestDto.getRemarks())
                .status(OutletReturnStatus.PENDING)
                .initiatorId(requestDto.getInitiatorId())
                .build();

        outletReturn = outletReturnRepository.save(outletReturn);

        for (OutletReturnRequestDto.ReturnItemDto itemDto : requestDto.getItems()) {
            // Validation
            DayProductionItem dpi = dayProductionItemRepository.findById(itemDto.getDayProductionItemId())
                    .orElseThrow(() -> new RuntimeException("DayProductionItem not found: " + itemDto.getDayProductionItemId()));

            if (dpi.getCurrentQty() < itemDto.getQty()) {
                throw new RuntimeException("Insufficient stock for product " + itemDto.getProductId() + 
                    ". Available: " + dpi.getCurrentQty() + ", Requested: " + itemDto.getQty());
            }

            // Deduct outlet stock immediately upon initiating return to store
            dpi.setCurrentQty(dpi.getCurrentQty() - itemDto.getQty());
            dayProductionItemRepository.save(dpi);

            OutletReturnItem returnItem = OutletReturnItem.builder()
                    .outletReturn(outletReturn)
                    .productId(itemDto.getProductId())
                    .dayProductionItemId(itemDto.getDayProductionItemId())
                    .qty(itemDto.getQty())
                    .batchNote(itemDto.getBatchNote())
                    .build();

            outletReturnItemRepository.save(returnItem);
        }

        return OutletReturnResponseDto.builder()
                .success(true)
                .message("Return request initiated successfully.")
                .returnId(outletReturn.getId())
                .returnNoteId(returnNoteId)
                .build();
    }

    @Override
    public List<OutletReturnDetailDto> getReturnsByStatus(OutletReturnStatus status) {
        List<OutletReturn> returns = outletReturnRepository.findByStatus(status);
        return returns.stream().map(this::convertToDetailDto).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public OutletReturnResponseDto approveReturn(Long returnId, UUID approverId) {
        OutletReturn outletReturn = outletReturnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return not found: " + returnId));

        if (outletReturn.getStatus() != OutletReturnStatus.PENDING) {
            throw new RuntimeException("Only PENDING returns can be approved.");
        }

        outletReturn.setStatus(OutletReturnStatus.APPROVED);
        outletReturn.setApproverId(approverId);
        outletReturnRepository.save(outletReturn);

        return OutletReturnResponseDto.builder()
                .success(true)
                .message("Return request approved.")
                .returnId(returnId)
                .build();
    }

    @Override
    @Transactional
    public OutletReturnResponseDto rejectReturn(Long returnId, UUID rejecterId) {
        OutletReturn outletReturn = outletReturnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return not found: " + returnId));

        if (outletReturn.getStatus() != OutletReturnStatus.PENDING && outletReturn.getStatus() != OutletReturnStatus.APPROVED) {
            throw new RuntimeException("Only PENDING or APPROVED returns can be rejected.");
        }

        // Restore outlet stock if return is rejected by main store
        for (OutletReturnItem item : outletReturn.getItems()) {
            DayProductionItem dpi = dayProductionItemRepository.findById(item.getDayProductionItemId())
                    .orElse(null);
            if (dpi != null) {
                dpi.setCurrentQty(dpi.getCurrentQty() + item.getQty());
                dayProductionItemRepository.save(dpi);
            }
        }

        outletReturn.setStatus(OutletReturnStatus.REJECTED);
        outletReturn.setApproverId(rejecterId); 
        outletReturnRepository.save(outletReturn);

        return OutletReturnResponseDto.builder()
                .success(true)
                .message("Return request rejected.")
                .returnId(returnId)
                .build();
    }

    @Override
    @Transactional
    public OutletReturnResponseDto receiveReturn(Long returnId, UUID receiverId) {
        OutletReturn outletReturn = outletReturnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return not found: " + returnId));

        if (outletReturn.getStatus() != OutletReturnStatus.APPROVED) {
            throw new RuntimeException("Only APPROVED returns can be received.");
        }

        // Finalize Inventory Update (Outlet stock was already deducted during initiateReturn)
        for (OutletReturnItem item : outletReturn.getItems()) {
            // Increase Main Store Stock
            MiniStoreItem storeItem = miniStoreItemRepository.findByProductIdAndOutletId(item.getProductId(), null)
                    .orElse(null);

            if (storeItem != null) {
                storeItem.setSystemQty(storeItem.getSystemQty().add(BigDecimal.valueOf(item.getQty())));
                miniStoreItemRepository.save(storeItem);
            } else {
                log.warn("Main Store item not found for product ID: {}. Inventory update skipped for main store.", item.getProductId());
            }
        }

        outletReturn.setStatus(OutletReturnStatus.RECEIVED);
        outletReturn.setReceiverId(receiverId);
        outletReturnRepository.save(outletReturn);

        return OutletReturnResponseDto.builder()
                .success(true)
                .message("Return received and inventory updated.")
                .returnId(returnId)
                .build();
    }

    @Override
    public OutletReturnDetailDto getReturnDetails(Long returnId) {
        OutletReturn outletReturn = outletReturnRepository.findById(returnId)
                .orElseThrow(() -> new RuntimeException("Return not found: " + returnId));
        return convertToDetailDto(outletReturn);
    }

    private String generateReturnNoteId() {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        return "RET-" + datePrefix;
    }

    private OutletReturnDetailDto convertToDetailDto(OutletReturn or) {
        String outletName = outletRepository.findById(or.getOutletId())
                .map(o -> o.getName()).orElse("Unknown Outlet");
        
        String initiatorName = "Unknown";
        if (or.getInitiatorId() != null) {
            initiatorName = authRepository.findById(IdUtil.uuidToBytes(or.getInitiatorId()))
                    .map(a -> a.getFirstName() + " " + (a.getLastName() != null ? a.getLastName() : ""))
                    .orElse("Unknown");
        }

        List<OutletReturnDetailDto.ReturnItemDetailDto> itemDtos = or.getItems().stream().map(item -> {
            Product product = productRepository.findById(item.getProductId()).orElse(null);
            return OutletReturnDetailDto.ReturnItemDetailDto.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .productName(product != null ? product.getProductName() : "Unknown")
                    .productCode(product != null ? product.getProductCode() : "N/A")
                    .qty(item.getQty())
                    .batchNote(item.getBatchNote())
                    .build();
        }).collect(Collectors.toList());

        return OutletReturnDetailDto.builder()
                .id(or.getId())
                .returnNoteId(or.getReturnNoteId())
                .outletId(or.getOutletId())
                .outletName(outletName)
                .status(or.getStatus())
                .reason(or.getReason())
                .remarks(or.getRemarks())
                .initiatorId(or.getInitiatorId())
                .initiatorName(initiatorName)
                .createdAt(or.getCreatedAt())
                .items(itemDtos)
                .build();
    }
}
