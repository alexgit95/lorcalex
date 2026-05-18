package com.alexgit95.controller;

import com.alexgit95.model.ApiKey;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.ApiKeyRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import com.alexgit95.service.ApiKeyService;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the {@code GET /api/export?apiKey=…} endpoint
 * and the {@code /api/admin/apikeys} management endpoints.
 */
@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class ApiKeyExportIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ApiKeyService apiKeyService;
    @Autowired private ApiKeyRepository apiKeyRepository;
    @Autowired private EditionRepository editionRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private UserCollectionRepository collectionRepository;

    @BeforeEach
    void clean() {
        collectionRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        editionRepository.deleteAllInBatch();
        apiKeyRepository.deleteAllInBatch();
    }

    // ── /api/export ───────────────────────────────────────────────────────

    @Test
    @DisplayName("export — valid API key returns 200 with expected payload")
    void export_validKey_returns200() throws Exception {
        Edition ed = saveEdition("TFC", "Premier Chapitre", 1);
        saveCard("Elsa", 1, "ext-elsa", ed);

        Map<String, Object> gen = apiKeyService.generateKey("CI", LocalDateTime.now().plusDays(1));
        String rawKey = (String) gen.get("key");

        String json = mockMvc.perform(get("/api/export").param("apiKey", rawKey))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> export = objectMapper.readValue(json, new TypeReference<>() {});
        assertThat(export.keySet()).contains(
                "exportDate", "version", "totalEditions", "totalCards", "totalCollection",
                "editions", "cards", "collection", "settings");
        assertThat(export.get("totalEditions")).isEqualTo(1);
        assertThat(export.get("totalCards")).isEqualTo(1);
    }

    @Test
    @DisplayName("export — missing API key returns 403")
    void export_missingKey_returns403() throws Exception {
        mockMvc.perform(get("/api/export"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("export — wrong API key returns 403")
    void export_wrongKey_returns403() throws Exception {
        mockMvc.perform(get("/api/export").param("apiKey", "0000000000000000000000000000000000000000000000000000000000000000"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("export — expired API key returns 403")
    void export_expiredKey_returns403() throws Exception {
        Map<String, Object> gen = apiKeyService.generateKey("Expired", LocalDateTime.now().minusSeconds(1));
        String rawKey = (String) gen.get("key");

        mockMvc.perform(get("/api/export").param("apiKey", rawKey))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("export — valid key updates lastUsedAt")
    void export_validKey_updatesLastUsedAt() throws Exception {
        Map<String, Object> gen = apiKeyService.generateKey("Touch", LocalDateTime.now().plusDays(30));
        String rawKey = (String) gen.get("key");

        // Retrieve persisted key
        String hash = ApiKeyService.sha256(rawKey);
        ApiKey before = apiKeyRepository.findByKeyHash(hash).orElseThrow();
        assertThat(before.getLastUsedAt()).isNull();

        mockMvc.perform(get("/api/export").param("apiKey", rawKey))
                .andExpect(status().isOk());

        ApiKey after = apiKeyRepository.findByKeyHash(hash).orElseThrow();
        assertThat(after.getLastUsedAt()).isNotNull();
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Edition saveEdition(String code, String name, int setNumber) {
        Edition e = new Edition();
        e.setCode(code);
        e.setName(name);
        e.setSetNumber(setNumber);
        e.setTotalCards(0);
        return editionRepository.save(e);
    }

    private Card saveCard(String name, int cardNumber, String externalId, Edition edition) {
        Card c = new Card();
        c.setName(name);
        c.setCardNumber(cardNumber);
        c.setExternalId(externalId);
        c.setEdition(edition);
        return cardRepository.save(c);
    }
}
