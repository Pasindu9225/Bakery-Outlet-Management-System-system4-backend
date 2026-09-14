package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ingredient_request_items")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class IngredientRequestItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "request_id", nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private IngredientRequest request;

    @Column(name = "raw_material_id", nullable = false)
    private Long rawMaterialId;

    @Column(name = "raw_material_name", length = 200)
    private String rawMaterialName;

    @Column(name = "requested_qty", nullable = false)
    private Double requestedQty;

    @Column(name = "issued_qty")
    private Double issuedQty;

    @Column(name = "unit_of_measure", length = 50)
    private String unitOfMeasure;
}
