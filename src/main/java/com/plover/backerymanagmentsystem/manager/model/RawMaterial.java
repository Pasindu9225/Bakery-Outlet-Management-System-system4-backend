package com.plover.backerymanagmentsystem.manager.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "raw_materials")
@com.fasterxml.jackson.annotation.JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMaterial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Column(name = "material_name", nullable = false)
    private String materialName;

    @Column(name = "material_code")
    private String materialCode;

    @Column(name = "unit_of_measure", nullable = false)
    private String unitOfMeasure;

    @Column(name = "description")
    private String description;

    @Column(name = "unit_cost", nullable = false)
    private Double unitCost;

    @Column(name = "current_stock", nullable = false)
    private Double currentStock;

    @Column(name = "minimum_stock_level")
    private Double minimumStockLevel;

    @Column(name = "batch_no")
    private String batchNo;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "expire_date")
    private LocalDate expireDate;

    @Column(name = "max_stock_level")
    private Double maxStockLevel;

    @Column(name = "vat_included")
    private Boolean vatIncluded;

    @Column(name = "initial_quantity")
    private Double initialQuantity;

    @Column(name = "supplier_name")
    private String supplierName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "rawMaterial", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<PackDetails> packDetails;

    // Proxy methods for backward compatibility
    public String getGenericMaterialName() {
        return brand != null && brand.getGenericMaterial() != null ? brand.getGenericMaterial().getName() : null;
    }

    public String getCategory() {
        return brand != null && brand.getGenericMaterial() != null ? brand.getGenericMaterial().getCategory() : null;
    }

    // Helper for easier access
    public String getBrandName() {
        return brand != null ? brand.getName() : null;
    }

    public String getDisplayName() {
        String genericName = getGenericMaterialName();
        String brandName = getBrandName();
        String primaryName = genericName != null && !genericName.trim().isEmpty() ? genericName 
                           : (brandName != null && !brandName.trim().isEmpty() ? brandName : null);

        if (primaryName == null || primaryName.trim().isEmpty()) {
            return materialName != null ? materialName : "Unknown Material";
        }

        if (materialName == null || materialName.trim().isEmpty()) {
            return primaryName;
        }

        String matLower = materialName.toLowerCase();
        String primLower = primaryName.toLowerCase();

        if (matLower.equals(primLower) || matLower.contains(primLower)) {
            return materialName;
        }

        return primaryName + " - " + materialName;
    }
}

