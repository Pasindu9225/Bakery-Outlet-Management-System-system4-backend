package com.plover.backerymanagmentsystem.pos.model;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Set;
import java.util.UUID;

import com.plover.backerymanagmentsystem.manager.model.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing discount rules (Product-based & Time-based)
 */
@Entity
@Table(name = "discounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Discount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "discount_id")
    private Integer discountId;

    @Column(name = "manager_id", nullable = false, length = 16, columnDefinition = "BINARY(16)")
    private byte[] managerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private DiscountRuleType ruleType;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "start_time")
    private LocalTime startTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "days_of_week")
    private String daysOfWeek; // Comma-separated: MON,TUE,WED,THU,FRI,SAT,SUN

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToMany
    @JoinTable(
        name = "discount_products",
        joinColumns = @JoinColumn(name = "discount_id"),
        inverseJoinColumns = @JoinColumn(name = "product_id")
    )
    private Set<Product> applicableProducts;

    @Column(name = "applied_to_all_products")
    @Builder.Default
    private Boolean appliedToAllProducts = false;

    @Column(name = "maximum_discount_value", precision = 10, scale = 2)
    private BigDecimal maximumDiscountValue;
}
