package com.plover.backerymanagmentsystem.store_keeper.model;

import java.time.LocalDateTime;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Entity representing a Supplier in the system.
 */
@Entity
@Table(name = "suppliers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "supplier_id")
    private Long supplierId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "contact_number", length = 50)
    private String contactNumber;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "rep_name", length = 255)
    private String repName;

    @Column(name = "rep_contact_no", length = 50)
    private String repContactNo;

    @Column(name = "bank_details", length = 255)
    private String bankDetails;

    @Column(name = "vat_status", length = 50)
    private String vatStatus;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RawMaterialSupplier> rawMaterialSuppliers;
}
