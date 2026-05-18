package com.alexgit95.service;

import com.alexgit95.model.ApiKey;
import com.alexgit95.repository.ApiKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ApiKeyService}: generation, validation and deletion logic.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiKeyServiceTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @InjectMocks
    private ApiKeyService apiKeyService;

    private ApiKey storedKey;
    private String rawKey;

    @BeforeEach
    void setup() {
        // Simule la clé retournée lors d'un save ─ l'id est injecté par la BDD
        when(apiKeyRepository.save(any(ApiKey.class))).thenAnswer(inv -> {
            ApiKey k = inv.getArgument(0);
            if (k.getId() == null) {
                // Simulate DB id assignment via reflection
                try {
                    var f = ApiKey.class.getDeclaredField("id");
                    f.setAccessible(true);
                    f.set(k, 1L);
                } catch (Exception ignored) { }
            }
            return k;
        });
    }

    // ── generateKey ────────────────────────────────────────────────────────

    @Test
    @DisplayName("generateKey — returns raw key in result and persists only hash")
    void generateKey_returnsRawKeyAndPersistsHash() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(30);

        Map<String, Object> result = apiKeyService.generateKey("Test Key", expiry);

        assertThat(result).containsKey("key");
        String key = (String) result.get("key");
        assertThat(key).hasSize(64); // 32 bytes → 64 hex chars

        ArgumentCaptor<ApiKey> captor = ArgumentCaptor.forClass(ApiKey.class);
        verify(apiKeyRepository).save(captor.capture());
        ApiKey saved = captor.getValue();

        assertThat(saved.getKeyHash()).isEqualTo(ApiKeyService.sha256(key));
        assertThat(saved.getKeyHash()).isNotEqualTo(key);
        assertThat(saved.getKeyPrefix()).isEqualTo(key.substring(0, 8));
        assertThat(saved.getName()).isEqualTo("Test Key");
        assertThat(saved.getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    @DisplayName("generateKey — two calls produce distinct keys")
    void generateKey_producesDistinctKeys() {
        LocalDateTime expiry = LocalDateTime.now().plusDays(30);

        Map<String, Object> r1 = apiKeyService.generateKey("Key 1", expiry);
        Map<String, Object> r2 = apiKeyService.generateKey("Key 2", expiry);

        assertThat(r1.get("key")).isNotEqualTo(r2.get("key"));
    }

    // ── validateAndTouch ──────────────────────────────────────────────────

    @Test
    @DisplayName("validateAndTouch — valid non-expired key is accepted and lastUsedAt updated")
    void validateAndTouch_validKey_accepted() {
        rawKey = buildAndStoreKey(LocalDateTime.now().plusDays(10));

        Optional<ApiKey> result = apiKeyService.validateAndTouch(rawKey);

        assertThat(result).isPresent();
        verify(apiKeyRepository, atLeast(2)).save(any(ApiKey.class)); // initial + touch
        assertThat(result.get().getLastUsedAt()).isNotNull();
    }

    @Test
    @DisplayName("validateAndTouch — expired key is rejected")
    void validateAndTouch_expiredKey_rejected() {
        rawKey = buildAndStoreKey(LocalDateTime.now().minusSeconds(1));

        Optional<ApiKey> result = apiKeyService.validateAndTouch(rawKey);

        assertThat(result).isEmpty();
        verify(apiKeyRepository, times(1)).save(any(ApiKey.class)); // only initial save
    }

    @Test
    @DisplayName("validateAndTouch — unknown key is rejected")
    void validateAndTouch_unknownKey_rejected() {
        when(apiKeyRepository.findByKeyHash(any())).thenReturn(Optional.empty());

        Optional<ApiKey> result = apiKeyService.validateAndTouch("0000000000000000000000000000000000000000000000000000000000000000");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("validateAndTouch — null or blank key is rejected without DB hit")
    void validateAndTouch_nullOrBlank_rejected() {
        assertThat(apiKeyService.validateAndTouch(null)).isEmpty();
        assertThat(apiKeyService.validateAndTouch("")).isEmpty();
        assertThat(apiKeyService.validateAndTouch("   ")).isEmpty();
        verify(apiKeyRepository, never()).findByKeyHash(any());
    }

    // ── listKeys ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("listKeys — maps entities to DTOs without hash or raw key")
    void listKeys_mapsToDTO() {
        ApiKey k = new ApiKey();
        k.setName("Demo");
        k.setKeyHash("hash");
        k.setKeyPrefix("abcdefgh");
        k.setExpiresAt(LocalDateTime.now().plusDays(5));
        k.setCreatedAt(LocalDateTime.now());
        when(apiKeyRepository.findAll()).thenReturn(List.of(k));

        List<Map<String, Object>> list = apiKeyService.listKeys();

        assertThat(list).hasSize(1);
        Map<String, Object> dto = list.get(0);
        assertThat(dto).containsKey("name");
        assertThat(dto).containsKey("keyPrefix");
        assertThat(dto).containsKey("expiresAt");
        assertThat(dto).containsKey("lastUsedAt");
        assertThat(dto).containsKey("expired");
        assertThat(dto).doesNotContainKey("keyHash");
    }

    @Test
    @DisplayName("listKeys — expired field is true when key is past expiry")
    void listKeys_expiredFlag_true() {
        ApiKey k = new ApiKey();
        k.setName("Old");
        k.setKeyHash("h");
        k.setKeyPrefix("abcdefgh");
        k.setExpiresAt(LocalDateTime.now().minusDays(1));
        k.setCreatedAt(LocalDateTime.now().minusDays(2));
        when(apiKeyRepository.findAll()).thenReturn(List.of(k));

        List<Map<String, Object>> list = apiKeyService.listKeys();

        assertThat((Boolean) list.get(0).get("expired")).isTrue();
    }

    // ── deleteKey ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("deleteKey — returns true and calls deleteById when key exists")
    void deleteKey_existingId_returnsTrue() {
        when(apiKeyRepository.existsById(1L)).thenReturn(true);

        boolean deleted = apiKeyService.deleteKey(1L);

        assertThat(deleted).isTrue();
        verify(apiKeyRepository).deleteById(1L);
    }

    @Test
    @DisplayName("deleteKey — returns false when key does not exist")
    void deleteKey_missingId_returnsFalse() {
        when(apiKeyRepository.existsById(99L)).thenReturn(false);

        boolean deleted = apiKeyService.deleteKey(99L);

        assertThat(deleted).isFalse();
        verify(apiKeyRepository, never()).deleteById(any());
    }

    // ── sha256 ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("sha256 — deterministic and produces 64-char hex")
    void sha256_deterministic() {
        String h1 = ApiKeyService.sha256("hello");
        String h2 = ApiKeyService.sha256("hello");
        assertThat(h1).isEqualTo(h2);
        assertThat(h1).hasSize(64);
        assertThat(h1).isEqualTo("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824");
    }

    // ── Helper ────────────────────────────────────────────────────────────

    /** Builds a key entity, stubs findByKeyHash for it, and returns the raw key string. */
    private String buildAndStoreKey(LocalDateTime expiresAt) {
        LocalDateTime expiry = expiresAt;
        // generate a real key and capture it
        Map<String, Object> gen = apiKeyService.generateKey("k", expiry);
        String key = (String) gen.get("key");

        // Reset: point findByKeyHash to the generated entity
        ApiKey stored = (ApiKey) ((Map<?, ?>) gen.get("apiKey") == null ? null : null);
        // Re-create the entity manually for stubbing
        ApiKey entity = new ApiKey();
        entity.setName("k");
        entity.setKeyHash(ApiKeyService.sha256(key));
        entity.setKeyPrefix(key.substring(0, 8));
        entity.setExpiresAt(expiry);
        entity.setCreatedAt(LocalDateTime.now());

        when(apiKeyRepository.findByKeyHash(ApiKeyService.sha256(key))).thenReturn(Optional.of(entity));
        return key;
    }
}
