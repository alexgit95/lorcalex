package com.alexgit95.service;

import com.alexgit95.model.ApiKey;
import com.alexgit95.repository.ApiKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ApiKeyService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final ApiKeyRepository apiKeyRepository;

    public ApiKeyService(ApiKeyRepository apiKeyRepository) {
        this.apiKeyRepository = apiKeyRepository;
    }

    // ── Génération ─────────────────────────────────────────────────────────

    /**
     * Génère une nouvelle clé API, la persiste (hachée) et retourne la clé en clair
     * en même temps que l'entité persistée (la clé ne sera plus jamais récupérable).
     *
     * @param name      nom descriptif de la clé
     * @param expiresAt date d'expiration
     * @return map contenant {@code "key"} (valeur en clair, une seule fois) et {@code "apiKey"} (entité)
     */
    @Transactional
    public Map<String, Object> generateKey(String name, LocalDateTime expiresAt) {
        byte[] rawBytes = new byte[32];
        SECURE_RANDOM.nextBytes(rawBytes);
        String rawKey = HexFormat.of().formatHex(rawBytes); // 64 chars hex

        String hash   = sha256(rawKey);
        String prefix = rawKey.substring(0, 8);

        ApiKey apiKey = new ApiKey();
        apiKey.setName(name);
        apiKey.setKeyHash(hash);
        apiKey.setKeyPrefix(prefix);
        apiKey.setExpiresAt(expiresAt);
        apiKeyRepository.save(apiKey);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("key", rawKey);
        result.put("apiKey", toDTO(apiKey));
        return result;
    }

    // ── Validation ─────────────────────────────────────────────────────────

    /**
     * Valide une clé fournie en clair.  Si valide et non expirée, met à jour
     * {@code lastUsedAt} et retourne l'entité.
     */
    @Transactional
    public Optional<ApiKey> validateAndTouch(String rawKey) {
        if (rawKey == null || rawKey.isBlank()) return Optional.empty();
        String hash = sha256(rawKey);
        Optional<ApiKey> opt = apiKeyRepository.findByKeyHash(hash);
        if (opt.isEmpty()) return Optional.empty();
        ApiKey key = opt.get();
        if (key.getExpiresAt().isBefore(LocalDateTime.now())) return Optional.empty();
        key.setLastUsedAt(LocalDateTime.now());
        apiKeyRepository.save(key);
        return Optional.of(key);
    }

    // ── Listage / Suppression ──────────────────────────────────────────────

    public List<Map<String, Object>> listKeys() {
        return apiKeyRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public boolean deleteKey(Long id) {
        if (!apiKeyRepository.existsById(id)) return false;
        apiKeyRepository.deleteById(id);
        return true;
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    public Map<String, Object> toDTO(ApiKey k) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", k.getId());
        m.put("name", k.getName());
        m.put("keyPrefix", k.getKeyPrefix());
        m.put("expiresAt", k.getExpiresAt() != null ? k.getExpiresAt().toString() : null);
        m.put("lastUsedAt", k.getLastUsedAt() != null ? k.getLastUsedAt().toString() : null);
        m.put("createdAt", k.getCreatedAt() != null ? k.getCreatedAt().toString() : null);
        m.put("expired", k.getExpiresAt() != null && k.getExpiresAt().isBefore(LocalDateTime.now()));
        return m;
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
