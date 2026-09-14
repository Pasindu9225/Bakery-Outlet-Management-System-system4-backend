package com.plover.backerymanagmentsystem.manager.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "pack_details")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PackDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pack_uom", nullable = false)
    private String packUom;

    @Column(name = "pack_size", nullable = false)
    private Double packSize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "raw_material_id", referencedColumnName = "id")
    @JsonBackReference
    private RawMaterial rawMaterial;
}
