package com.alexgit95.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_collection",
        uniqueConstraints = @UniqueConstraint(columnNames = "card_id"))
@Data
@NoArgsConstructor
public class UserCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private Card card;

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private Boolean foil = false;

    private LocalDateTime firstAddedAt;
    private LocalDateTime lastAddedAt;

    @PrePersist
    protected void onCreate() {
        // Null checks allow pre-setting dates (e.g. during backup restore)
        if (firstAddedAt == null) firstAddedAt = LocalDateTime.now();
        if (lastAddedAt == null)  lastAddedAt  = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastAddedAt = LocalDateTime.now();
    }
}
