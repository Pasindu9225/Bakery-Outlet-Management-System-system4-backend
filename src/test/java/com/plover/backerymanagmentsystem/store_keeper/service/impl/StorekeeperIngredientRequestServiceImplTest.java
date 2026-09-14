package com.plover.backerymanagmentsystem.store_keeper.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.plover.backerymanagmentsystem.manager.model.IngredientRequest;
import com.plover.backerymanagmentsystem.manager.model.IngredientRequestItem;
import com.plover.backerymanagmentsystem.manager.model.IngredientRequestStatus;
import com.plover.backerymanagmentsystem.manager.model.RawMaterial;
import com.plover.backerymanagmentsystem.manager.repository.IngredientRequestRepository;
import com.plover.backerymanagmentsystem.manager.repository.RawMaterialRepository;
import com.plover.backerymanagmentsystem.store_keeper.dto.IssueIngredientRequestDto;
import com.plover.backerymanagmentsystem.store_keeper.exception.InsufficientStockException;
import com.plover.backerymanagmentsystem.worker.dto.IngredientRequestDto;

@ExtendWith(MockitoExtension.class)
class StorekeeperIngredientRequestServiceImplTest {

    @Mock
    private IngredientRequestRepository ingredientRequestRepository;

    @Mock
    private RawMaterialRepository rawMaterialRepository;

    @Mock
    private com.plover.backerymanagmentsystem.manager.repository.ProductionCenterRepository productionCenterRepository;

    @Mock
    private com.plover.backerymanagmentsystem.manager.repository.MiniStoreItemRepository miniStoreItemRepository;

    @InjectMocks
    private StorekeeperIngredientRequestServiceImpl service;

    private IngredientRequest request;
    private IngredientRequestItem item1;
    private IngredientRequestItem item2;
    private RawMaterial material1;
    private RawMaterial material2;

    @BeforeEach
    void setUp() {
        material1 = RawMaterial.builder()
                .id(1L)
                .materialName("Wheat Flour")
                .materialCode("WF001")
                .currentStock(100.0)
                .unitOfMeasure("kg")
                .unitCost(2.5)
                .build();

        material2 = RawMaterial.builder()
                .id(2L)
                .materialName("Sugar")
                .materialCode("S001")
                .currentStock(10.0) // Low stock to trigger failure
                .unitOfMeasure("kg")
                .unitCost(1.5)
                .build();

        request = IngredientRequest.builder()
                .id(100L)
                .productionPlanId(500L)
                .productionCenterId(10L)
                .status(IngredientRequestStatus.PENDING)
                .notes("Daily request")
                .createdAt(LocalDateTime.now())
                .build();

        item1 = IngredientRequestItem.builder()
                .id(10L)
                .request(request)
                .rawMaterialId(1L)
                .rawMaterialName("Wheat Flour")
                .requestedQty(50.0)
                .unitOfMeasure("kg")
                .build();

        item2 = IngredientRequestItem.builder()
                .id(11L)
                .request(request)
                .rawMaterialId(2L)
                .rawMaterialName("Sugar")
                .requestedQty(25.0)
                .unitOfMeasure("kg")
                .build();

        request.setItems(List.of(item1, item2));
    }

    @Test
    void listPending_returnsPendingRequests() {
        when(ingredientRequestRepository.findByStatusOrderByCreatedAtAsc(IngredientRequestStatus.PENDING))
                .thenReturn(List.of(request));
        when(rawMaterialRepository.findById(1L)).thenReturn(Optional.of(material1));
        when(rawMaterialRepository.findById(2L)).thenReturn(Optional.of(material2));
        when(rawMaterialRepository.findAllByMaterialCode("WF001")).thenReturn(List.of(material1));
        when(rawMaterialRepository.findAllByMaterialCode("S001")).thenReturn(List.of(material2));

        List<IngredientRequestDto> result = service.listPending();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(100L);
        assertThat(result.get(0).getItems()).hasSize(2);
        assertThat(result.get(0).getItems().get(0).getAvailableQty()).isEqualTo(100.0);
        assertThat(result.get(0).getItems().get(1).getAvailableQty()).isEqualTo(10.0);
        verify(ingredientRequestRepository, times(1)).findByStatusOrderByCreatedAtAsc(IngredientRequestStatus.PENDING);
    }

    @Test
    void listAll_returnsSortedRequests() {
        when(ingredientRequestRepository.findAll()).thenReturn(List.of(request));
        when(rawMaterialRepository.findById(1L)).thenReturn(Optional.of(material1));
        when(rawMaterialRepository.findById(2L)).thenReturn(Optional.of(material2));
        when(rawMaterialRepository.findAllByMaterialCode("WF001")).thenReturn(List.of(material1));
        when(rawMaterialRepository.findAllByMaterialCode("S001")).thenReturn(List.of(material2));

        List<IngredientRequestDto> result = service.listAll();

        assertThat(result).hasSize(1);
        verify(ingredientRequestRepository, times(1)).findAll();
    }

    @Test
    void issue_success_deductsStockAndSetsStatus() {
        when(ingredientRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(rawMaterialRepository.findById(1L)).thenReturn(Optional.of(material1));
        when(rawMaterialRepository.findById(2L)).thenReturn(Optional.of(material2));
        when(rawMaterialRepository.findAllByMaterialCode("WF001")).thenReturn(List.of(material1));
        when(rawMaterialRepository.findAllByMaterialCode("S001")).thenReturn(List.of(material2));
        when(ingredientRequestRepository.save(any(IngredientRequest.class))).thenReturn(request);

        IssueIngredientRequestDto.ItemIssue issue1 = IssueIngredientRequestDto.ItemIssue.builder()
                .itemId(10L)
                .issuedQty(30.0)
                .build();
        IssueIngredientRequestDto.ItemIssue issue2 = IssueIngredientRequestDto.ItemIssue.builder()
                .itemId(11L)
                .issuedQty(5.0)
                .build();
        IssueIngredientRequestDto dto = IssueIngredientRequestDto.builder()
                .items(List.of(issue1, issue2))
                .build();

        IngredientRequestDto result = service.issue(100L, dto);

        // Verification
        assertThat(result.getStatus()).isEqualTo("ISSUED");
        assertThat(item1.getIssuedQty()).isEqualTo(30.0);
        assertThat(item2.getIssuedQty()).isEqualTo(5.0);
        assertThat(material1.getCurrentStock()).isEqualTo(70.0); // 100 - 30
        assertThat(material2.getCurrentStock()).isEqualTo(5.0);  // 10 - 5

        verify(rawMaterialRepository, times(1)).save(material1);
        verify(rawMaterialRepository, times(1)).save(material2);
        verify(ingredientRequestRepository, times(1)).save(request);
    }

    @Test
    void issue_throwsInsufficientStockException_withDetailMessages() {
        when(ingredientRequestRepository.findById(100L)).thenReturn(Optional.of(request));
        when(rawMaterialRepository.findById(1L)).thenReturn(Optional.of(material1));
        when(rawMaterialRepository.findById(2L)).thenReturn(Optional.of(material2));
        when(rawMaterialRepository.findAllByMaterialCode("WF001")).thenReturn(List.of(material1));
        when(rawMaterialRepository.findAllByMaterialCode("S001")).thenReturn(List.of(material2));

        IssueIngredientRequestDto.ItemIssue issue1 = IssueIngredientRequestDto.ItemIssue.builder()
                .itemId(10L)
                .issuedQty(120.0) // More than 100 available
                .build();
        IssueIngredientRequestDto.ItemIssue issue2 = IssueIngredientRequestDto.ItemIssue.builder()
                .itemId(11L)
                .issuedQty(15.0) // More than 10 available
                .build();
        IssueIngredientRequestDto dto = IssueIngredientRequestDto.builder()
                .items(List.of(issue1, issue2))
                .build();

        assertThatThrownBy(() -> service.issue(100L, dto))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock for materials:")
                .satisfies(ex -> {
                    InsufficientStockException exception = (InsufficientStockException) ex;
                    assertThat(exception.getInsufficientMaterials()).containsExactlyInAnyOrder(
                        "Wheat Flour (Requested: 50.00, Available: 100.00)",
                        "Sugar (Requested: 25.00, Available: 10.00)"
                    );
                });

        // No stocks should be modified or saved
        verify(rawMaterialRepository, never()).save(any(RawMaterial.class));
        verify(ingredientRequestRepository, never()).save(any(IngredientRequest.class));
    }

    @Test
    void issue_throwsRuntimeException_ifRequestNotPending() {
        request.setStatus(IngredientRequestStatus.ISSUED);
        when(ingredientRequestRepository.findById(100L)).thenReturn(Optional.of(request));

        IssueIngredientRequestDto dto = IssueIngredientRequestDto.builder().items(List.of()).build();

        assertThatThrownBy(() -> service.issue(100L, dto))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cannot issue: request is not in PENDING status");

        verify(rawMaterialRepository, never()).save(any(RawMaterial.class));
    }
}
