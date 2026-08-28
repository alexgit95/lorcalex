package com.alexgit95.controller;

import com.alexgit95.model.AppSettings;
import com.alexgit95.model.ApiKey;
import com.alexgit95.model.Card;
import com.alexgit95.model.CollectionValueSnapshot;
import com.alexgit95.model.Edition;
import com.alexgit95.model.EditionValueSnapshot;
import com.alexgit95.model.User;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.ApiKeyRepository;
import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.CollectionValueSnapshotRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.EditionValueSnapshotRepository;
import com.alexgit95.repository.UserCollectionRepository;
import com.alexgit95.repository.UserRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class BackupRestoreIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EditionRepository editionRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private UserCollectionRepository collectionRepository;
    @Autowired private AppSettingsRepository settingsRepository;
    @Autowired private CollectionValueSnapshotRepository collectionValueSnapshotRepository;
    @Autowired private EditionValueSnapshotRepository editionValueSnapshotRepository;
    @Autowired private ApiKeyRepository apiKeyRepository;
    @Autowired private UserRepository userRepository;

    @BeforeEach
    void clean() {
        collectionRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        editionRepository.deleteAllInBatch();
        settingsRepository.deleteAllInBatch();
        collectionValueSnapshotRepository.deleteAllInBatch();
        editionValueSnapshotRepository.deleteAllInBatch();
    }

    @Test
    @WithMockUser
    @DisplayName("backup returns expected top-level keys")
    void backup_topLevelStructure() throws Exception {
        Edition ed = saveEdition("TFC", "Premier Chapitre", 1);
        saveCard("Elsa", 1, "ext-1", ed);

        String json = mockMvc.perform(get("/api/admin/backup"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> backup = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        assertThat(backup.keySet()).contains(
            "backupDate",
            "version",
            "totalEditions",
            "totalCards",
            "totalCollection",
            "editions",
            "cards",
            "collection",
            "settings");
    }

    @Test
    @WithMockUser
    @DisplayName("restore supports foil=true and preserves dates")
    void restore_preservesFoilAndDates() throws Exception {
        LocalDateTime first = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        Map<String, Object> payload = buildPayload("ext-1", "TFC", "Premier Chapitre", 1, 3, true, first);

        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        UserCollection uc = collectionRepository.findAll().get(0);
        assertThat(uc.getFoil()).isTrue();
        assertThat(uc.getQuantity()).isEqualTo(3);
        assertThat(uc.getFirstAddedAt().withNano(0)).isEqualTo(first.withNano(0));
    }

    @Test
    @WithMockUser
    @DisplayName("restore legacy collection without foil defaults to false")
    void restore_legacyWithoutFoil_defaultsFalse() throws Exception {
        LocalDateTime first = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        Map<String, Object> payload = buildPayload("ext-2", "ROF", "Set 2", 2, 1, false, first);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> collection = (List<Map<String, Object>>) payload.get("collection");
        collection.get(0).remove("foil");

        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        UserCollection uc = collectionRepository.findAll().get(0);
        assertThat(uc.getFoil()).isFalse();
    }

    @Test
    @WithMockUser
    @DisplayName("restore preserves foilQuantity")
    void restore_preservesFoilQuantity() throws Exception {
        LocalDateTime first = LocalDateTime.of(2025, 6, 1, 8, 0, 0);
        Map<String, Object> payload = buildPayload("ext-foilqty", "TFC", "Premier Chapitre", 1, 2, true, first);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> collection = (List<Map<String, Object>>) payload.get("collection");
        collection.get(0).put("foilQuantity", 3);

        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        UserCollection uc = collectionRepository.findAll().get(0);
        assertThat(uc.getFoil()).isTrue();
        assertThat(uc.getQuantity()).isEqualTo(2);
        assertThat(uc.getFoilQuantity()).isEqualTo(3);
    }

    @Test
    @WithMockUser
    @DisplayName("backup includes foilQuantity in collection entries")
    void backup_includesFoilQuantity() throws Exception {
        Edition ed = saveEdition("TFC", "Premier Chapitre", 1);
        Card card = saveCard("Elsa", 1, "ext-foilbk", ed);

        UserCollection uc = new UserCollection();
        uc.setCard(card);
        uc.setQuantity(1);
        uc.setFoilQuantity(4);
        uc.setFoil(true);
        collectionRepository.save(uc);

        String json = mockMvc.perform(get("/api/admin/backup"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> backup = objectMapper.readValue(json, new tools.jackson.core.type.TypeReference<>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> coll = (List<Map<String, Object>>) backup.get("collection");
        assertThat(coll).hasSize(1);
        assertThat(coll.get(0).get("foilQuantity")).isEqualTo(4);
        assertThat(coll.get(0).get("foil")).isEqualTo(true);
    }

    @Test
    @WithMockUser
    @DisplayName("backup includes pricing value and last price timestamp")
    void backup_includesPricingMetadata() throws Exception {
        Edition ed = saveEdition("TFC", "Premier Chapitre", 1);
        Card card = saveCard("Elsa", 1, "ext-price-bk", ed);
        card.setMarketPrice(new BigDecimal("9.95"));
        card.setPriceCurrency("EUR");
        card.setPriceSource("rapidapi-lorcana-prices");
        LocalDateTime lastPriceAt = LocalDateTime.of(2026, 8, 25, 9, 30, 0);
        card.setLastPriceAt(lastPriceAt);
        card.setLastPriceStatus("SUCCESS");
        cardRepository.save(card);

        String json = mockMvc.perform(get("/api/admin/backup"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> backup = objectMapper.readValue(json, new tools.jackson.core.type.TypeReference<>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cards = (List<Map<String, Object>>) backup.get("cards");

        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).get("marketPrice")).isEqualTo(9.95);
        assertThat(cards.get(0).get("priceCurrency")).isEqualTo("EUR");
        assertThat(cards.get(0).get("priceSource")).isEqualTo("rapidapi-lorcana-prices");
        assertThat(cards.get(0).get("lastPriceAt")).isEqualTo(lastPriceAt.toString());
        assertThat(cards.get(0).get("lastPriceStatus")).isEqualTo("SUCCESS");
    }

    @Test
    @WithMockUser
    @DisplayName("restore preserves pricing value and last price timestamp")
    void restore_preservesPricingMetadata() throws Exception {
        LocalDateTime first = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        LocalDateTime lastPriceAt = LocalDateTime.of(2026, 8, 25, 9, 30, 0);
        Map<String, Object> payload = buildPayload("ext-price-rs", "TFC", "Premier Chapitre", 1, 1, false, first);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cards = (List<Map<String, Object>>) payload.get("cards");
        cards.get(0).put("marketPrice", 12.50);
        cards.get(0).put("priceCurrency", "EUR");
        cards.get(0).put("priceSource", "rapidapi-lorcana-prices");
        cards.get(0).put("lastPriceAt", lastPriceAt.toString());
        cards.get(0).put("lastPriceStatus", "SUCCESS");

        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Card restored = cardRepository.findByExternalId("ext-price-rs").orElseThrow();
        assertThat(restored.getMarketPrice()).isEqualByComparingTo("12.5");
        assertThat(restored.getPriceCurrency()).isEqualTo("EUR");
        assertThat(restored.getPriceSource()).isEqualTo("rapidapi-lorcana-prices");
        assertThat(restored.getLastPriceAt().withNano(0)).isEqualTo(lastPriceAt.withNano(0));
        assertThat(restored.getLastPriceStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @WithMockUser
    @DisplayName("backup includes the wanted marker")
    void backup_includesWanted() throws Exception {
        Edition ed = saveEdition("TFC", "Premier Chapitre", 1);
        Card card = saveCard("Elsa", 1, "ext-wanted-bk", ed);
        card.setWanted(true);
        cardRepository.save(card);

        String json = mockMvc.perform(get("/api/admin/backup"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> backup = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cards = (List<Map<String, Object>>) backup.get("cards");
        assertThat(cards).hasSize(1);
        assertThat(cards.get(0).get("wanted")).isEqualTo(true);
    }

    @Test
    @WithMockUser
    @DisplayName("restore applies the wanted marker")
    void restore_appliesWanted() throws Exception {
        LocalDateTime first = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        Map<String, Object> payload = buildPayload("ext-wanted-rs", "TFC", "Premier Chapitre", 1, 1, false, first);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cards = (List<Map<String, Object>>) payload.get("cards");
        cards.get(0).put("wanted", true);

        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Card restored = cardRepository.findByExternalId("ext-wanted-rs").orElseThrow();
        assertThat(restored.getWanted()).isTrue();
    }

    @Test
    @WithMockUser
    @DisplayName("restore of legacy payload without wanted defaults to false")
    void restore_legacyWithoutWanted_defaultsFalse() throws Exception {
        LocalDateTime first = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        Map<String, Object> payload = buildPayload("ext-wanted-legacy", "TFC", "Premier Chapitre", 1, 1, false, first);

        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Card restored = cardRepository.findByExternalId("ext-wanted-legacy").orElseThrow();
        assertThat(restored.getWanted()).isFalse();
    }

    @Test
    @WithMockUser
    @DisplayName("backup includes collection and edition value history")
    void backup_includesValueHistory() throws Exception {
        Edition ed = saveEdition("TFC", "Premier Chapitre", 1);

        CollectionValueSnapshot collSnap = new CollectionValueSnapshot();
        collSnap.setRecordedAt(LocalDateTime.of(2026, 8, 1, 2, 0, 0));
        collSnap.setTotalCollectionValueEur(new BigDecimal("100.00"));
        collSnap.setCurrency("EUR");
        collSnap.setSource("PRICING_SYNC");
        collectionValueSnapshotRepository.save(collSnap);

        EditionValueSnapshot editionSnap = new EditionValueSnapshot();
        editionSnap.setRecordedAt(LocalDateTime.of(2026, 8, 1, 2, 0, 0));
        editionSnap.setEditionId(ed.getId());
        editionSnap.setEditionCode(ed.getCode());
        editionSnap.setEditionName(ed.getName());
        editionSnap.setTotalValueEur(new BigDecimal("100.00"));
        editionValueSnapshotRepository.save(editionSnap);

        String json = mockMvc.perform(get("/api/admin/backup"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> backup = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        assertThat(backup).containsKey("valueHistory");
        @SuppressWarnings("unchecked")
        Map<String, Object> valueHistory = (Map<String, Object>) backup.get("valueHistory");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> collSnapshots = (List<Map<String, Object>>) valueHistory.get("collectionSnapshots");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> editionSnapshots = (List<Map<String, Object>>) valueHistory.get("editionSnapshots");
        assertThat(collSnapshots).hasSize(1);
        assertThat(collSnapshots.get(0).get("totalCollectionValueEur")).isEqualTo(100.00);
        assertThat(editionSnapshots).hasSize(1);
        assertThat(editionSnapshots.get(0).get("editionId")).isEqualTo(ed.getId().intValue());
    }

    @Test
    @WithMockUser
    @DisplayName("restore recreates value history and remaps edition ids")
    void restore_recreatesValueHistoryWithRemappedEditionIds() throws Exception {
        LocalDateTime first = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        Map<String, Object> payload = buildPayload("ext-vh-rs", "TFC", "Premier Chapitre", 1, 1, false, first);

        Map<String, Object> collSnap = new LinkedHashMap<>();
        collSnap.put("recordedAt", "2026-08-01T02:00:00");
        collSnap.put("totalCollectionValueEur", 42.5);
        collSnap.put("currency", "EUR");
        collSnap.put("source", "PRICING_SYNC");

        Map<String, Object> editionSnap = new LinkedHashMap<>();
        editionSnap.put("recordedAt", "2026-08-01T02:00:00");
        editionSnap.put("editionId", 1); // old id from the "editions" array in buildPayload
        editionSnap.put("editionCode", "TFC");
        editionSnap.put("editionName", "Premier Chapitre");
        editionSnap.put("totalValueEur", 42.5);

        Map<String, Object> valueHistory = new LinkedHashMap<>();
        valueHistory.put("collectionSnapshots", List.of(collSnap));
        valueHistory.put("editionSnapshots", List.of(editionSnap));
        payload.put("valueHistory", valueHistory);

        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        List<CollectionValueSnapshot> collSnapshots = collectionValueSnapshotRepository.findAll();
        assertThat(collSnapshots).hasSize(1);
        assertThat(collSnapshots.get(0).getTotalCollectionValueEur()).isEqualByComparingTo("42.5");

        List<EditionValueSnapshot> editionSnapshots = editionValueSnapshotRepository.findAll();
        assertThat(editionSnapshots).hasSize(1);
        Edition restoredEdition = editionRepository.findAll().get(0);
        assertThat(editionSnapshots.get(0).getEditionId()).isEqualTo(restoredEdition.getId());
    }

    @Test
    @WithMockUser
    @DisplayName("restore of legacy payload without valueHistory succeeds with no snapshots")
    void restore_legacyWithoutValueHistory_noSnapshots() throws Exception {
        LocalDateTime first = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        Map<String, Object> payload = buildPayload("ext-vh-legacy", "TFC", "Premier Chapitre", 1, 1, false, first);

        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(collectionValueSnapshotRepository.findAll()).isEmpty();
        assertThat(editionValueSnapshotRepository.findAll()).isEmpty();
    }

    @Test
    @WithMockUser
    @DisplayName("restore skips an edition-level snapshot referencing an edition absent from the payload")
    void restore_skipsEditionSnapshotForMissingEdition() throws Exception {
        LocalDateTime first = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        Map<String, Object> payload = buildPayload("ext-vh-missing-ed", "TFC", "Premier Chapitre", 1, 1, false, first);

        Map<String, Object> editionSnap = new LinkedHashMap<>();
        editionSnap.put("recordedAt", "2026-08-01T02:00:00");
        editionSnap.put("editionId", 999); // no matching edition in this payload's "editions" array
        editionSnap.put("editionCode", "GONE");
        editionSnap.put("editionName", "Édition disparue");
        editionSnap.put("totalValueEur", 10.0);

        Map<String, Object> valueHistory = new LinkedHashMap<>();
        valueHistory.put("collectionSnapshots", List.of());
        valueHistory.put("editionSnapshots", List.of(editionSnap));
        payload.put("valueHistory", valueHistory);

        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(editionValueSnapshotRepository.findAll()).isEmpty();
    }

    @Test
    @WithMockUser
    @DisplayName("backup does not include API keys or admin credentials")
    void backup_excludesApiKeysAndUsers() throws Exception {
        ApiKey key = new ApiKey();
        key.setName("Test key");
        key.setKeyHash("hash-exclude");
        key.setKeyPrefix("abcd1234");
        key.setExpiresAt(LocalDateTime.now().plusDays(30));
        apiKeyRepository.save(key);

        String json = mockMvc.perform(get("/api/admin/backup"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> backup = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        assertThat(backup.keySet()).doesNotContain("apiKeys", "users");
    }

    @Test
    @WithMockUser
    @DisplayName("restore does not modify existing API keys or admin credentials")
    void restore_doesNotModifyApiKeysOrUsers() throws Exception {
        ApiKey key = new ApiKey();
        key.setName("Test key");
        key.setKeyHash("hash-restore");
        key.setKeyPrefix("abcd1234");
        key.setExpiresAt(LocalDateTime.now().plusDays(30));
        apiKeyRepository.save(key);
        long userCountBefore = userRepository.count();

        LocalDateTime first = LocalDateTime.of(2025, 1, 1, 10, 0, 0);
        Map<String, Object> payload = buildPayload("ext-nosec", "TFC", "Premier Chapitre", 1, 1, false, first);

        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertThat(apiKeyRepository.findAll()).hasSize(1);
        assertThat(apiKeyRepository.findAll().get(0).getName()).isEqualTo("Test key");
        assertThat(userRepository.count()).isEqualTo(userCountBefore);
    }

    private Edition saveEdition(String code, String name, int setNumber) {
        Edition ed = new Edition();
        ed.setCode(code);
        ed.setName(name);
        ed.setSetNumber(setNumber);
        return editionRepository.save(ed);
    }

    private Card saveCard(String name, int number, String externalId, Edition ed) {
        Card card = new Card();
        card.setName(name);
        card.setCardNumber(number);
        card.setExternalId(externalId);
        card.setEdition(ed);
        return cardRepository.save(card);
    }

    private Map<String, Object> buildPayload(
            String externalId,
            String editionCode,
            String editionName,
            int setNumber,
            int qty,
            boolean foil,
            LocalDateTime firstAddedAt
    ) {
        Map<String, Object> ed = new LinkedHashMap<>();
        ed.put("id", 1);
        ed.put("code", editionCode);
        ed.put("name", editionName);
        ed.put("setNumber", setNumber);

        Map<String, Object> card = new LinkedHashMap<>();
        card.put("externalId", externalId);
        card.put("cardNumber", 1);
        card.put("editionCode", editionCode);
        card.put("name", "Card " + externalId);

        Map<String, Object> uc = new LinkedHashMap<>();
        uc.put("externalId", externalId);
        uc.put("cardNumber", 1);
        uc.put("editionCode", editionCode);
        uc.put("quantity", qty);
        uc.put("foil", foil);
        uc.put("firstAddedAt", firstAddedAt.toString());
        uc.put("lastAddedAt", firstAddedAt.plusDays(1).toString());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("version", "2");
        payload.put("editions", List.of(ed));
        payload.put("cards", List.of(card));
        payload.put("collection", List.of(uc));
        payload.put("settings", List.of(new AppSettings("k", "v", "d")));
        return payload;
    }
}
