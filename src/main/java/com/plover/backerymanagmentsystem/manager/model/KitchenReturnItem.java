package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kitchen_return_items")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class KitchenReturnItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kitchen_return_id", nullable = false)
    private KitchenReturn kitchenReturn;

    @Column(name = "raw_material_id", nullable = false)
    private Long rawMaterialId;

    @Column(name = "raw_material_name", length = 200)
    private String rawMaterialName;

    @Column(name = "quantity", nullable = false)
    private Double quantity;

    @Column(name = "unit", length = 50)
    private String unit;

    @Column(name = "reason", length = 500)
    private String reason;
}
