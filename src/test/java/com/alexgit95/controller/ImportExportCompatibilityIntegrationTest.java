package com.alexgit95.controller;

import com.alexgit95.model.ApiKey;
import com.alexgit95.repository.ApiKeyRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.CollectionValueSnapshotRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.EditionValueSnapshotRepository;
import com.alexgit95.repository.UserCollectionRepository;
import com.alexgit95.service.ApiKeyService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class ImportExportCompatibilityIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EditionRepository editionRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private UserCollectionRepository collectionRepository;
    @Autowired private ApiKeyService apiKeyService;
    @Autowired private ApiKeyRepository apiKeyRepository;
    @Autowired private CollectionValueSnapshotRepository collectionValueSnapshotRepository;
    @Autowired private EditionValueSnapshotRepository editionValueSnapshotRepository;

    @BeforeEach
    void clean() {
        collectionRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        editionRepository.deleteAllInBatch();
        apiKeyRepository.deleteAllInBatch();
        collectionValueSnapshotRepository.deleteAllInBatch();
        editionValueSnapshotRepository.deleteAllInBatch();
    }

    @Test
    @WithMockUser
    @DisplayName("restore accepts current v2 fixture (N)")
    void restore_acceptsCurrentFixture() throws Exception {
        String payload = new ClassPathResource("compat/backup-v2-current-minimal.json")
                .getContentAsString(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        assertThat(editionRepository.count()).isEqualTo(1);
        assertThat(cardRepository.count()).isEqualTo(1);
        assertThat(collectionRepository.count()).isEqualTo(1);
        assertThat(cardRepository.findByExternalId("compat-ext-1").orElseThrow().getWanted()).isTrue();
        assertThat(collectionValueSnapshotRepository.count()).isEqualTo(1);
        assertThat(editionValueSnapshotRepository.count()).isEqualTo(1);
    }

    @Test
    @WithMockUser
    @DisplayName("restore accepts legacy v2 fixture (N-1)")
    void restore_acceptsLegacyFixture() throws Exception {
        String payload = new ClassPathResource("lorcalex-backup-2026-04-01.json")
                .getContentAsString(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        assertThat(editionRepository.count()).isEqualTo(11);
        assertThat(cardRepository.count()).isEqualTo(2500);
        assertThat(cardRepository.findAll()).allMatch(c -> Boolean.FALSE.equals(c.getWanted()));
        assertThat(collectionValueSnapshotRepository.count()).isEqualTo(0);
        assertThat(editionValueSnapshotRepository.count()).isEqualTo(0);
    }

    @Test
    @DisplayName("export returns contract keys and version with valid api key")
    void export_contractRemainsStable() throws Exception {
        Map<String, Object> gen = apiKeyService.generateKey("compat", LocalDateTime.now().plusDays(1));
        String rawKey = (String) gen.get("key");

        String json = mockMvc.perform(get("/api/export").param("apiKey", rawKey))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> export = objectMapper.readValue(json, new TypeReference<>() {});
        assertThat(export.get("version")).isEqualTo("2");
        assertThat(export.keySet()).contains(
                "exportDate", "version", "totalEditions", "totalCards", "totalCollection",
                "editions", "cards", "collection", "settings");

        String hash = ApiKeyService.sha256(rawKey);
        ApiKey key = apiKeyRepository.findByKeyHash(hash).orElseThrow();
        assertThat(key.getLastUsedAt()).isNotNull();
    }

    @Test
    @WithMockUser
    @DisplayName("current fixture preserves pricing fields through restore and export")
    void restoreThenExport_preservesPricingFields() throws Exception {
        String payload = new ClassPathResource("compat/backup-v2-current-minimal.json")
                .getContentAsString(StandardCharsets.UTF_8);

        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk());

        Map<String, Object> gen = apiKeyService.generateKey("compat-pricing", LocalDateTime.now().plusDays(1));
        String rawKey = (String) gen.get("key");

        String json = mockMvc.perform(get("/api/export").param("apiKey", rawKey))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> export = objectMapper.readValue(json, new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        java.util.List<Map<String, Object>> cards = (java.util.List<Map<String, Object>>) export.get("cards");

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0)).containsKeys(
                "marketPrice", "priceCurrency", "priceSource", "lastPriceAt", "lastPriceStatus");
        assertThat(cards.get(0).get("priceCurrency")).isEqualTo("EUR");
        assertThat(cards.get(0).get("priceSource")).isEqualTo("rapidapi-lorcana-prices");
        assertThat(cards.get(0).get("lastPriceStatus")).isEqualTo("SUCCESS");
    }
}
