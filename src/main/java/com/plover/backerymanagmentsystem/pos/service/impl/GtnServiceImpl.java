package com.plover.backerymanagmentsystem.pos.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plover.backerymanagmentsystem.manager.model.Product;
import com.plover.backerymanagmentsystem.manager.repository.ProductRepository;
import com.plover.backerymanagmentsystem.pos.dto.CreateGtnRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.CreateGtnResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GetGtnProductsResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GetPartiallyReceivedItemsResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GtnProductItemDto;
import com.plover.backerymanagmentsystem.pos.dto.GtnReceiveRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.GtnReceiveResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.GtnWithProductsDto;
import com.plover.backerymanagmentsystem.pos.dto.ManualEntryRequestDto;
import com.plover.backerymanagmentsystem.pos.dto.ManualEntryResponseDto;
import com.plover.backerymanagmentsystem.pos.dto.PartiallyReceivedItemDto;
import com.plover.backerymanagmentsystem.pos.exception.GtnNotFoundException;
import com.plover.backerymanagmentsystem.pos.exception.GtnProcessingException;
import com.plover.backerymanagmentsystem.pos.model.DayProduction;
import com.plover.backerymanagmentsystem.pos.model.DayProductionItem;
import com.plover.backerymanagmentsystem.pos.model.EntryStatus;
import com.plover.backerymanagmentsystem.pos.model.Gtn;
import com.plover.backerymanagmentsystem.pos.model.GtnDayProduction;
import com.plover.backerymanagmentsystem.pos.model.GtnItem;
import com.plover.backerymanagmentsystem.pos.model.GtnStatus;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.DayProductionRepository;
import com.plover.backerymanagmentsystem.pos.repository.GtnDayProductionRepository;
import com.plover.backerymanagmentsystem.pos.repository.GtnItemRepository;
import com.plover.backerymanagmentsystem.pos.repository.GtnRepository;
import com.plover.backerymanagmentsystem.pos.service.GtnService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementation of GTN service for managing Goods Transfer Note operations
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GtnServiceImpl implements GtnService {

    private final GtnRepository gtnRepository;
    private final GtnItemRepository gtnItemRepository;
    private final DayProductionRepository dayProductionRepository;
    private final DayProductionItemRepository dayProductionItemRepository;
    private final GtnDayProductionRepository gtnDayProductionRepository;
    private final ProductRepository productRepository;

    @Override
    public GetGtnProductsResponseDto getGtnProducts(Long outletId) {
        try {
            log.info("Fetching GTN products with status not in 'received' or 'over received'");

            // Define excluded statuses
            List<GtnStatus> excludedStatuses = Arrays.asList(
                    GtnStatus.RECEIVED,
                    GtnStatus.OVER_RECEIVED);

            // Fetch GTNs with their items and products
            List<Gtn> gtns = gtnRepository.findAllByStatusNotInWithItemsAndOutlet(excludedStatuses, outletId);

            log.info("Found {} GTNs with non-received status", gtns.size());

            // Convert to DTOs
            List<GtnWithProductsDto> gtnDtos = gtns.stream()
                    .map(this::convertToGtnWithProductsDto)
                    .collect(Collectors.toList());

            GetGtnProductsResponseDto response = new GetGtnProductsResponseDto();
            response.setSuccess(true);
            response.setMessage("GTN products retrieved successfully");
            response.setGtns(gtnDtos);
            response.setTotalCount(gtnDtos.size());
            return response;

        } catch (Exception e) {
            log.error("Error occurred while fetching GTN products", e);
            throw new GtnProcessingException("Failed to retrieve GTN products", e);
        }
    }

    /**
     * Convert GTN entity to DTO with product information
     */
    private GtnWithProductsDto convertToGtnWithProductsDto(Gtn gtn) {
        List<GtnProductItemDto> itemDtos = gtn.getGtnItems().stream()
                .map(this::convertToGtnProductItemDto)
                .collect(Collectors.toList());

        return new GtnWithProductsDto(
                gtn.getGtnId(),
                gtn.getDate(),
                gtn.getStatus(),
                gtn.getSource(),
                gtn.getAddedBy(),
                gtn.getApprovedBy(),
                itemDtos);
    }

    /**
     * Convert GTN Item entity to DTO with product information
     */
    private GtnProductItemDto convertToGtnProductItemDto(GtnItem gtnItem) {
        return new GtnProductItemDto(
                gtnItem.getGtnItemId(),
                gtnItem.getProduct().getId(),
                gtnItem.getProduct().getProductName(),
                gtnItem.getExpectedQty(),
                gtnItem.getReceivedQty(),
                gtnItem.getStatus(),
                gtnItem.getExpiryDate(),
                gtnItem.getUnit(),
                gtnItem.getEntryStatus(),
                gtnItem.getRemarks());
    }

    @Override
    @Transactional
    public GtnReceiveResponseDto receiveGtnItems(GtnReceiveRequestDto requestDto) {
        try {
            log.info("Processing GTN receive request for GTN ID: {}", requestDto.getGtnId());

            // Validate request
            if (requestDto.getGtnId() == null) {
                throw new IllegalArgumentException("GTN ID is required");
            }
            if (requestDto.getReceivedItems() == null || requestDto.getReceivedItems().isEmpty()) {
                throw new IllegalArgumentException("At least one item must be provided for receiving");
            }

            // Validate each received item
            for (var item : requestDto.getReceivedItems()) {
                if (item.getGtnItemId() == null) {
                    throw new IllegalArgumentException("GTN Item ID is required for all items");
                }
                if (item.getReceivedQty() == null || item.getReceivedQty() <= 0) {
                    throw new IllegalArgumentException("Received quantity must be greater than 0");
                }
            }

            // Validate GTN exists
            Gtn gtn = gtnRepository.findById(requestDto.getGtnId())
                    .orElseThrow(() -> new GtnNotFoundException(requestDto.getGtnId()));

            // Get GTN items to update
            List<Integer> itemIds = requestDto.getReceivedItems().stream()
                    .map(item -> item.getGtnItemId())
                    .collect(Collectors.toList());

            List<GtnItem> gtnItems = gtnItemRepository.findByGtnIdAndItemIds(
                    requestDto.getGtnId(), itemIds);

            if (gtnItems.size() != requestDto.getReceivedItems().size()) {
                throw new GtnProcessingException("Some GTN items not found or don't belong to this GTN");
            }

            // Update GTN items
            requestDto.getReceivedItems().forEach(receivedItem -> {
                GtnItem gtnItem = gtnItems.stream()
                        .filter(item -> item.getGtnItemId().equals(receivedItem.getGtnItemId()))
                        .findFirst()
                        .orElseThrow(
                                () -> new GtnProcessingException("GTN item not found: " + receivedItem.getGtnItemId()));

                // Update received quantity (cumulative for this GTN item)
                double addedQty = receivedItem.getReceivedQty();
                double newReceivedQty = gtnItem.getReceivedQty() + addedQty;
                gtnItem.setReceivedQty(newReceivedQty);

                // Update status based on expected vs received
                if (newReceivedQty < gtnItem.getExpectedQty()) {
                    gtnItem.setStatus(GtnStatus.PARTIALLY_RECEIVED);
                } else if (newReceivedQty == gtnItem.getExpectedQty()) {
                    gtnItem.setStatus(GtnStatus.RECEIVED);
                } else {
                    gtnItem.setStatus(GtnStatus.OVER_RECEIVED);
                }

                // Set entry status to system
                gtnItem.setEntryStatus(EntryStatus.SYSTEM);
            });

            // Save updated GTN items
            gtnItemRepository.saveAll(gtnItems);

            // Create day production entry
            DayProduction dayProduction = new DayProduction();
            dayProduction.setOrderedDate(LocalDate.now());
            dayProduction.setIsActive(true);
            dayProduction.setCreatedAt(LocalDateTime.now());
            dayProduction.setOutletId(gtn.getOutletId()); // Link production to the specific outlet
            final DayProduction savedDayProduction = dayProductionRepository.save(dayProduction);

            // Create GTN-Day Production link
            GtnDayProduction gtnDayProduction = new GtnDayProduction();
            gtnDayProduction.setGtn(gtn);
            gtnDayProduction.setDayProduction(savedDayProduction);
            gtnDayProductionRepository.save(gtnDayProduction);

            // Track running current_qty for products processed in this batch to handle
            // duplicates
            java.util.Map<Long, Integer> runningCurrentQtyMap = new java.util.HashMap<>();

            // Create day production items for the recently received GTN items
            List<DayProductionItem> dayProductionItems = requestDto.getReceivedItems().stream()
                    .map(receivedItem -> {
                        // Find the corresponding GtnItem
                        GtnItem gtnItem = gtnItems.stream()
                                .filter(item -> item.getGtnItemId().equals(receivedItem.getGtnItemId()))
                                .findFirst()
                                .orElseThrow(() -> new GtnProcessingException(
                                        "GTN item not found for ID: " + receivedItem.getGtnItemId()));

                        Long productId = gtnItem.getProduct().getId();
                        int newlyAdded = receivedItem.getReceivedQty().intValue();

                        DayProductionItem dayProdItem = new DayProductionItem();
                        dayProdItem.setProduct(gtnItem.getProduct());
                        dayProdItem.setDayProduction(savedDayProduction);
                        dayProdItem.setOrderedQty(gtnItem.getExpectedQty().intValue());
                        dayProdItem.setReceivedQty(newlyAdded);
                        dayProdItem.setCurrentQty(newlyAdded);
                        dayProdItem.setCreatedAt(LocalDateTime.now());
                        return dayProdItem;
                    })
                    .collect(Collectors.toList());

            dayProductionItemRepository.saveAll(dayProductionItems);

            log.info("Successfully processed GTN receive for GTN ID: {}, Production ID: {}",
                    requestDto.getGtnId(), savedDayProduction.getProductionId());

            return new GtnReceiveResponseDto(
                    true,
                    "GTN items received successfully",
                    requestDto.getGtnId(),
                    savedDayProduction.getProductionId(),
                    requestDto.getReceivedItems().size());

        } catch (GtnNotFoundException | GtnProcessingException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while processing GTN receive", e);
            throw new GtnProcessingException("Failed to process GTN receive", e);
        }
    }

    @Override
    public GetPartiallyReceivedItemsResponseDto getPartiallyReceivedItems() {
        try {
            log.info("Fetching partially received GTN items");

            List<GtnItem> partiallyReceivedItems = gtnItemRepository
                    .findByStatusWithDetails(GtnStatus.PARTIALLY_RECEIVED);

            List<PartiallyReceivedItemDto> itemDtos = partiallyReceivedItems.stream()
                    .map(this::convertToPartiallyReceivedItemDto)
                    .collect(Collectors.toList());

            return new GetPartiallyReceivedItemsResponseDto(
                    true,
                    "Partially received items retrieved successfully",
                    itemDtos,
                    itemDtos.size());

        } catch (Exception e) {
            log.error("Error occurred while fetching partially received items", e);
            throw new GtnProcessingException("Failed to retrieve partially received items", e);
        }
    }

    /**
     * Convert GTN Item to PartiallyReceivedItemDto
     */
    private PartiallyReceivedItemDto convertToPartiallyReceivedItemDto(GtnItem gtnItem) {
        double remainingQty = gtnItem.getExpectedQty() - gtnItem.getReceivedQty();

        return new PartiallyReceivedItemDto(
                gtnItem.getGtnItemId(),
                gtnItem.getGtn().getGtnId(),
                gtnItem.getGtn().getDate(),
                gtnItem.getProduct().getId(),
                gtnItem.getProduct().getProductName(),
                gtnItem.getProduct().getProductCode(),
                gtnItem.getExpectedQty(),
                gtnItem.getReceivedQty(),
                remainingQty,
                gtnItem.getExpiryDate(),
                gtnItem.getUnit(),
                gtnItem.getStatus(),
                gtnItem.getEntryStatus(),
                gtnItem.getRemarks());
    }

    @Override
    @Transactional
    public ManualEntryResponseDto createManualEntry(ManualEntryRequestDto requestDto) {
        try {
            log.info("Creating manual entry for product ID: {}", requestDto.getProductId());

            // Validate input
            if (requestDto.getProductId() == null) {
                throw new IllegalArgumentException("Product ID is required");
            }
            if (requestDto.getQuantity() == null || requestDto.getQuantity() <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than 0");
            }
            if (requestDto.getUnit() == null) {
                throw new IllegalArgumentException("Unit is required");
            }
            if (requestDto.getSource() == null) {
                throw new IllegalArgumentException("Source is required");
            }
            if (requestDto.getUserId() == null || requestDto.getUserId().trim().isEmpty()) {
                throw new IllegalArgumentException("User ID is required");
            }

            // Validate product exists
            Product product = productRepository.findById(requestDto.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Product not found with ID: " + requestDto.getProductId()));

            // Convert UUID string to UUID object
            UUID userUuid = UUID.fromString(requestDto.getUserId());

            // Create GTN record
            Gtn gtn = new Gtn();
            gtn.setDate(LocalDateTime.now());
            gtn.setSource(requestDto.getSource());
            gtn.setAddedBy(userUuid);
            gtn.setApprovedBy(null);
            gtn.setStatus(GtnStatus.RECEIVED);
            gtn.setOutletId(requestDto.getOutletId()); // Set outlet ID for manual entry

            Gtn savedGtn = gtnRepository.save(gtn);
            log.info("Created GTN record with ID: {}", savedGtn.getGtnId());


            // Create GTN Item record
            GtnItem gtnItem = new GtnItem();
            gtnItem.setGtn(savedGtn);
            gtnItem.setProduct(product);
            gtnItem.setExpectedQty(requestDto.getQuantity());
            gtnItem.setReceivedQty(requestDto.getQuantity());
            gtnItem.setStatus(GtnStatus.RECEIVED);
            gtnItem.setExpiryDate(requestDto.getExpiryDate());
            gtnItem.setUnit(requestDto.getUnit());
            gtnItem.setEntryStatus(EntryStatus.MANUAL);
            gtnItem.setRemarks(requestDto.getRemarks());

            GtnItem savedGtnItem = gtnItemRepository.save(gtnItem);
            log.info("Created GTN Item record with ID: {}", savedGtnItem.getGtnItemId());

            // Create day production entry
            DayProduction dayProduction = new DayProduction();
            dayProduction.setOrderedDate(LocalDate.now());
            dayProduction.setIsActive(true);
            dayProduction.setCreatedAt(LocalDateTime.now());
            dayProduction.setOutletId(requestDto.getOutletId()); // Link production to the specific outlet
            final DayProduction savedDayProduction = dayProductionRepository.save(dayProduction);

            // Create GTN-Day Production link
            GtnDayProduction gtnDayProduction = new GtnDayProduction();
            gtnDayProduction.setGtn(savedGtn);
            gtnDayProduction.setDayProduction(savedDayProduction);
            gtnDayProductionRepository.save(gtnDayProduction);

            // Create day production item for this manual entry
            DayProductionItem dayProdItem = new DayProductionItem();
            dayProdItem.setProduct(product);
            dayProdItem.setDayProduction(savedDayProduction);

            int qty = requestDto.getQuantity().intValue();
            dayProdItem.setOrderedQty(qty);
            dayProdItem.setReceivedQty(qty);
            dayProdItem.setCurrentQty(qty);
            dayProdItem.setCreatedAt(LocalDateTime.now());
            dayProductionItemRepository.save(dayProdItem);

            log.info("Manual production records created successfully. Production ID: {}",
                    savedDayProduction.getProductionId());

            // Create response
            ManualEntryResponseDto response = new ManualEntryResponseDto();
            response.setSuccess(true);
            response.setMessage("Manual entry created successfully and is now salable");
            response.setGtnId(savedGtn.getGtnId());
            response.setGtnItemId(savedGtnItem.getGtnItemId());
            response.setProductId(product.getId());
            response.setProductName(product.getProductName());
            response.setQuantity(requestDto.getQuantity());
            response.setUnit(requestDto.getUnit());
            response.setSource(requestDto.getSource());
            response.setEntryDate(savedGtn.getDate());
            response.setExpiryDate(savedGtnItem.getExpiryDate());
            response.setRemarks(requestDto.getRemarks());

            log.info("Manual entry created successfully for product: {} with GTN ID: {}",
                    product.getProductName(), savedGtn.getGtnId());

            return response;

        } catch (IllegalArgumentException e) {
            log.error("Validation error in manual entry: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error occurred while creating manual entry", e);
            throw new GtnProcessingException("Failed to create manual entry", e);
        }
    }

    @Override
    @Transactional
    public CreateGtnResponseDto createGtn(CreateGtnRequestDto requestDto) {
        log.info("Creating new GTN from source: {}", requestDto.getSource());

        // Validate basic fields
        if (requestDto.getSource() == null) {
            throw new GtnProcessingException("GTN Source is required");
        }
        if (requestDto.getStatus() == null) {
            throw new GtnProcessingException("GTN Status is required");
        }
        if (requestDto.getAddedBy() == null || requestDto.getAddedBy().trim().isEmpty()) {
            throw new GtnProcessingException("AddedBy user ID is required");
        }

        // Create GTN record
        Gtn gtn = new Gtn();
        gtn.setDate(requestDto.getDate() != null ? requestDto.getDate() : LocalDateTime.now());
        gtn.setSource(requestDto.getSource());
        gtn.setStatus(requestDto.getStatus());
        gtn.setOutletId(requestDto.getOutletId());

        try {
            gtn.setAddedBy(UUID.fromString(requestDto.getAddedBy()));
        } catch (IllegalArgumentException e) {
            throw new GtnProcessingException("Invalid UUID format for addedBy: " + requestDto.getAddedBy());
        }

        if (requestDto.getApprovedBy() != null && !requestDto.getApprovedBy().trim().isEmpty()) {
            try {
                gtn.setApprovedBy(UUID.fromString(requestDto.getApprovedBy()));
            } catch (IllegalArgumentException e) {
                throw new GtnProcessingException("Invalid UUID format for approvedBy: " + requestDto.getApprovedBy());
            }
        }

        Gtn savedGtn = gtnRepository.save(gtn);
        log.info("Saved GTN header with ID: {}", savedGtn.getGtnId());

        // Create GTN items
        if (requestDto.getItems() != null && !requestDto.getItems().isEmpty()) {
            List<GtnItem> items = requestDto.getItems().stream().map(itemReq -> {
                Product product = productRepository.findById(itemReq.getProductId())
                        .orElseThrow(
                                () -> new GtnProcessingException(
                                        "Product not found with ID: " + itemReq.getProductId()));

                // Validate item fields
                if (itemReq.getExpectedQty() == null) {
                    throw new GtnProcessingException(
                            "Expected quantity is required for product: " + product.getProductName());
                }
                if (itemReq.getStatus() == null) {
                    throw new GtnProcessingException("Status is required for product: " + product.getProductName());
                }
                if (itemReq.getExpiryDate() == null) {
                    throw new GtnProcessingException(
                            "Expiry date is required for product: " + product.getProductName());
                }
                if (itemReq.getUnit() == null) {
                    throw new GtnProcessingException("Unit is required for product: " + product.getProductName());
                }
                if (itemReq.getEntryStatus() == null) {
                    throw new GtnProcessingException(
                            "Entry status is required for product: " + product.getProductName());
                }

                GtnItem item = new GtnItem();
                item.setGtn(savedGtn);
                item.setProduct(product);
                item.setExpectedQty(itemReq.getExpectedQty());
                item.setReceivedQty(itemReq.getReceivedQty() != null ? itemReq.getReceivedQty() : 0.0);
                item.setStatus(itemReq.getStatus());
                item.setExpiryDate(itemReq.getExpiryDate());
                item.setUnit(itemReq.getUnit());
                item.setEntryStatus(itemReq.getEntryStatus());
                item.setRemarks(itemReq.getRemarks());
                return item;
            }).collect(Collectors.toList());

            gtnItemRepository.saveAll(items);
            log.info("Saved {} GTN items for GTN ID: {}", items.size(), savedGtn.getGtnId());
        }

        return CreateGtnResponseDto.builder()
                .success(true)
                .message("GTN created successfully")
                .gtnId(savedGtn.getGtnId())
                .build();
    }
}
