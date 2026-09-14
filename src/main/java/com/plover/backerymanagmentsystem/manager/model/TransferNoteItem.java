package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "transfer_note_items")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TransferNoteItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transfer_note_id", nullable = false)
    private TransferNote transferNote;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "product_name", length = 200)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "unit", length = 50)
    private String unit;
}
