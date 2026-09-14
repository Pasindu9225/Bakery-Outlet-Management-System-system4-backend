package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outlets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Outlet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "outlet_id")
    private Long outletId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", length = 500)
    private String address;
    @Column(name = "location")
    private String location;

    @Column(name = "main_branch")
    private String mainBranch;

    @Column(name = "status", columnDefinition = "TINYINT(1)")
    private Boolean status;
}
