package com.plover.backerymanagmentsystem.pos.model;

import java.math.BigDecimal;
import java.util.UUID;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;
import com.plover.backerymanagmentsystem.manager.model.Product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entity representing individual items in a sale transaction
 */
@Entity
@Table(name = "sale_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaleItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sale_item_id")
    private Integer saleItemId;

    @Column(name = "day_production_item_id")
    private Integer dayProductionItemId;

    @Column(name = "product_id")
    private Long productId;

    @Column(name = "qty", nullable = false)
    private Integer qty;

    @Column(name = "price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "free_meal_reason")
    private String freeMealReason;

    @Column(name = "bank_transfer_code")
    private String bankTransferCode;

    @Column(name = "discount_id")
    private Integer discountId;

    @Column(name = "payment_method_id", nullable = false)
    private Integer paymentMethodId;

    @Column(name = "promotion_id")
    private Long promotionId;

    @Column(name = "applied_discount", precision = 10, scale = 2)
    private BigDecimal appliedDiscount;

    @Column(name = "sale_id", nullable = false)
    private Integer saleId;

    @Column(name = "cashier_id", nullable = false)
    private UUID cashierId;

    @Column(name = "special_instructions")
    private String specialInstructions;

    @Enumerated(EnumType.STRING)
    @Column(name = "item_status")
    private SaleItemStatus itemStatus;

    // Relationships
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_production_item_id", referencedColumnName = "day_production_item_id", insertable = false, updatable = false)
    private DayProductionItem dayProductionItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id", referencedColumnName = "discount_id", insertable = false, updatable = false)
    private Discount discount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_method_id", referencedColumnName = "payment_method_id", insertable = false, updatable = false)
    private PaymentMethod paymentMethod;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", referencedColumnName = "sale_id", insertable = false, updatable = false)
    private Sale sale;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AuthModel cashier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Promotion promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Product product;
}