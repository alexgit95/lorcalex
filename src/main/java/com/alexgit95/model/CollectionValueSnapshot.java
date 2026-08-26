package com.alexgit95.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "collection_value_snapshots")
@Data
@NoArgsConstructor
public class CollectionValueSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalCollectionValueEur;

    @Column(nullable = false)
    private String currency = "EUR";

    @Column(nullable = false)
    private String source = "PRICING_SYNC";

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (recordedAt == null) {
            recordedAt = createdAt;
        }
    }
}
