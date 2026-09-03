package com.alexgit95.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "edition_value_snapshots")
@Data
@NoArgsConstructor
public class EditionValueSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column(nullable = false)
    private Long editionId;

    private String editionCode;

    private String editionName;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalValueEur;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (recordedAt == null) {
            recordedAt = createdAt;
        }
    }
}
