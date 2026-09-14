package com.plover.backerymanagmentsystem.pos.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.plover.backerymanagmentsystem.core.login.model.AuthModel;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "day_end_closings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayEndClosing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "closing_id")
    private Integer id;

    @Column(name = "outlet_id", nullable = false)
    private Long outletId;

    @Column(name = "cashier_id", nullable = false)
    private UUID cashierId;

    @Column(name = "closing_date", nullable = false)
    private LocalDate closingDate;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "dayEndClosing", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DayEndClosingItem> items;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cashier_id", referencedColumnName = "id", insertable = false, updatable = false)
    private AuthModel cashier;
}
