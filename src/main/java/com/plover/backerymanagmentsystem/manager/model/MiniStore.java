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
@Table(name = "mini_stores")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MiniStore {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mini_store_id", columnDefinition = "INT")
    private Integer miniStoreId;
    
    @Column(name = "name", nullable = false)
    private String name;
    
    @Column(name = "store_date", nullable = false)
    private LocalDate storeDate;
    
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "miniStore", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<MiniStoreItem> miniStoreItems;
}
