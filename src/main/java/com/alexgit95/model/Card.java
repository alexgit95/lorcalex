package com.alexgit95.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "cards")
@Data
@NoArgsConstructor
public class Card {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private Integer cardNumber;

    private String rarity;

    private Integer cost;

    private String inkColor;

    private String type;

    private String subtypes;

    @Column(columnDefinition = "TEXT")
    private String bodyText;

    @Column(columnDefinition = "TEXT")
    private String flavorText;

    @Column(columnDefinition = "TEXT")
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String thumbnailUrl;

    private String artist;

    private Boolean inkable;

    private String externalId;

    private Long imageHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "edition_id")
    private Edition edition;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
