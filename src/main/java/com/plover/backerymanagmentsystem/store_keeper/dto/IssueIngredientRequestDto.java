package com.plover.backerymanagmentsystem.store_keeper.dto;

import java.util.List;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class IssueIngredientRequestDto {
    private List<ItemIssue> items;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ItemIssue {
        private Long itemId;       // IngredientRequestItem.id
        private Double issuedQty;  // actual issued
    }
}
