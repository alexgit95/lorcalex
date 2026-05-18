package com.alexgit95.controller;

import com.alexgit95.model.AppSettings;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
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

    @BeforeEach
    void clean() {
        collectionRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        editionRepository.deleteAllInBatch();
        settingsRepository.deleteAllInBatch();
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
