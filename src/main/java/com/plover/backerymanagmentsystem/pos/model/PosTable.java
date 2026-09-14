package com.plover.backerymanagmentsystem.pos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tables")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosTable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", length = 100, unique = true)
    private String tableName;

    @Column(name = "status", length = 50)
    private String status;

    @Column(name = "seat_count")
    private Integer seatCount;
}
