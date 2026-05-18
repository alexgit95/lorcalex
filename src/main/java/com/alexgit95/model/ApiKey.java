package com.alexgit95.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nom lisible donné à la clé (ex. : "Home Assistant", "Script Python"). */
    @Column(nullable = false)
    private String name;

    /** SHA-256 de la clé réelle — jamais exposé en clair. */
    @Column(nullable = false, unique = true)
    private String keyHash;

    /** 8 premiers caractères de la clé en clair, pour identification dans l'UI. */
    @Column(nullable = false, length = 8)
    private String keyPrefix;

    /** Date d'expiration de la clé. */
    @Column(nullable = false)
    private LocalDateTime expiresAt;

    /** Dernière utilisation réussie (null si jamais utilisée). */
    @Column
    private LocalDateTime lastUsedAt;

    /** Date de création. */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    // ── Getters / Setters ──────────────────────────────────────────────────

    public Long getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getKeyHash() { return keyHash; }
    public void setKeyHash(String keyHash) { this.keyHash = keyHash; }

    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
