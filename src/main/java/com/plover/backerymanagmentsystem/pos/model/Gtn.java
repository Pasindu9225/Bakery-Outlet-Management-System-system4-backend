package com.plover.backerymanagmentsystem.pos.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Convert;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing a GTN (Goods Transfer Note) in the system. A GTN tracks
 * the transfer of products from different sources.
 */
@Entity
@Table(name = "gtn")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Gtn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "gtn_id")
    private Integer gtnId;

    @Column(name = "date", nullable = false)
    private LocalDateTime date;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private GtnStatus status;

    @Column(name = "source", nullable = false)
    @Convert(converter = com.plover.backerymanagmentsystem.pos.model.converter.GtnSourceConverter.class)
    private GtnSource source;

    @Column(name = "added_by", nullable = false, columnDefinition = "BINARY(16)")
    private UUID addedBy;

    @Column(name = "approved_by", columnDefinition = "BINARY(16)")
    private UUID approvedBy;

    @Column(name = "outlet_id")
    private Long outletId;

    @OneToMany(mappedBy = "gtn", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GtnItem> gtnItems;
}
