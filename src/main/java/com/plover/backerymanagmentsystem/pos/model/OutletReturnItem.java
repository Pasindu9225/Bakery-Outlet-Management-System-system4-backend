package com.plover.backerymanagmentsystem.pos.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outlet_return_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutletReturnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "outlet_return_id", nullable = false)
    private OutletReturn outletReturn;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "day_production_item_id")
    private Integer dayProductionItemId;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "batch_note")
    private String batchNote;
}
