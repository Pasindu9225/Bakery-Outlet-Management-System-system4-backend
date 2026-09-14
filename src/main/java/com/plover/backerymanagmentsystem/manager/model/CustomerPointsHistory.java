package com.plover.backerymanagmentsystem.manager.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "customer_points_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPointsHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @JsonIgnore
    private Customer customer;

    @Column(name = "sale_id")
    private Integer saleId;

    @Column(name = "points_changed", columnDefinition = "DECIMAL(10, 3)", nullable = false)
    private Double pointsChanged;

    @Column(name = "description", nullable = false)
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
