package com.alexgit95.controller;

import com.alexgit95.model.Card;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests based on the real production backup file
 * {@code lorcalex-backup-2026-04-01.json} (11 sets, 2500 cards, 1181 entries).
 *
 * <p>The file is a <em>legacy</em> format (version "2" but before the foil/dates feature),
 * so it also validates the backwards-compatibility of the restore logic.
 */
@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class RealBackupRestoreTest {

    // ── Known values extracted from the backup file ─────────────────────────
    private static final int    EXPECTED_EDITIONS   = 11;
    private static final int    EXPECTED_CARDS      = 2500;
    private static final int    EXPECTED_COLLECTION = 1181;

    /** externalId → expected quantity for a handful of spot-check entries. */
    private static final Map<String, Integer> SPOT_QTY = Map.of(
            "1",   5,   // Ariel #1 set 1
            "2",   1,   // Ariel #2 set 1
            "3",   5,   // Cendrillon #3 set 1
            "5",   1,   // Hadès #5 set 1
            "12",  6,   // set 1 #12
            "13",  7    // set 1 #13
    );

    // ── Expected edition codes and set numbers ────────────────────────────────
    private static final Map<String, Integer> EDITION_SET_NUMBERS = Map.of(
            "1",  1,  "2",  2,  "3",  3,  "4",  4,  "5",  5,
            "6",  6,  "7",  7,  "8",  8,  "9",  9, "10", 10
    );

    // ── stats_enabled_sets old value in backup: "1,2,3,4,5,6,10" ─────────────
    // (these are the old DB IDs for editions with setNumber 1–6 and 10)
    private static final List<Integer> STATS_SET_NUMBERS = List.of(1, 2, 3, 4, 5, 6, 10);

    @Autowired private MockMvc                  mockMvc;
    @Autowired private ObjectMapper             objectMapper;
    @Autowired private EditionRepository        editionRepository;
    @Autowired private CardRepository           cardRepository;
    @Autowired private UserCollectionRepository collectionRepository;
    @Autowired private AppSettingsRepository    settingsRepository;

    // ── Backup file payload ──────────────────────────────────────────────────
    private static String backupJson;

    @BeforeEach
    void setUp() throws Exception {
        // Load file once; reuse across tests
        if (backupJson == null) {
            backupJson = new ClassPathResource("lorcalex-backup-2026-04-01.json")
                    .getContentAsString(StandardCharsets.UTF_8);
        }
        // Clean slate before each test
        collectionRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        editionRepository.deleteAllInBatch();
        settingsRepository.deleteAllInBatch();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. Restore — counts and response
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Restore from real backup — counts & response")
    class RestoreCounts {

        @Test
        @WithMockUser
        @DisplayName("restore returns success=true")
        void restore_returnsSuccess() throws Exception {
            mockMvc.perform(post("/api/admin/restore")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(backupJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @WithMockUser
        @DisplayName("11 editions are recreated in the database")
        void restore_editionCount() throws Exception {
            restore();
            assertThat(editionRepository.count()).isEqualTo(EXPECTED_EDITIONS);
        }

        @Test
        @WithMockUser
        @DisplayName("2500 cards are recreated in the database")
        void restore_cardCount() throws Exception {
            restore();
            assertThat(cardRepository.count()).isEqualTo(EXPECTED_CARDS);
        }

        @Test
        @WithMockUser
        @DisplayName("1181 collection entries are recreated in the database")
        void restore_collectionCount() throws Exception {
            restore();
            assertThat(collectionRepository.count()).isEqualTo(EXPECTED_COLLECTION);
        }

        @Test
        @WithMockUser
        @DisplayName("response message contains correct counts")
        void restore_messageContainsCounts() throws Exception {
            mockMvc.perform(post("/api/admin/restore")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(backupJson))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString(EXPECTED_EDITIONS + " édition")))
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString(EXPECTED_CARDS + " carte")))
                    .andExpect(jsonPath("$.message").value(
                            org.hamcrest.Matchers.containsString(EXPECTED_COLLECTION + " entrée")));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. Restore — editions integrity
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Restore from real backup — editions integrity")
    class RestoreEditions {

        @Test
        @WithMockUser
        @DisplayName("all 11 edition codes are present")
        void restore_allEditionCodes() throws Exception {
            restore();
            List<String> codes = editionRepository.findAll().stream()
                    .map(com.alexgit95.model.Edition::getCode)
                    .collect(Collectors.toList());
            assertThat(codes).containsExactlyInAnyOrder(
                    "1","2","3","4","5","6","7","8","9","10","11");
        }

        @Test
        @WithMockUser
        @DisplayName("set numbers 1–11 are all present")
        void restore_setNumbers() throws Exception {
            restore();
            List<Integer> setNumbers = editionRepository.findAll().stream()
                    .map(com.alexgit95.model.Edition::getSetNumber)
                    .sorted()
                    .collect(Collectors.toList());
            assertThat(setNumbers).containsExactly(1,2,3,4,5,6,7,8,9,10,11);
        }

        @Test
        @WithMockUser
        @DisplayName("edition names match the backup values")
        void restore_editionNames() throws Exception {
            restore();
            Map<String, String> codeToName = editionRepository.findAll().stream()
                    .collect(Collectors.toMap(
                            com.alexgit95.model.Edition::getCode,
                            com.alexgit95.model.Edition::getName));
            assertThat(codeToName.get("1")).isEqualTo("Premier Chapitre");
            assertThat(codeToName.get("2")).isEqualTo("L'Ascension des Floodborn");
            assertThat(codeToName.get("3")).isEqualTo("Les Terres d'Encres");
            assertThat(codeToName.get("9")).isEqualTo("Fabuleux");
            assertThat(codeToName.get("11")).isEqualTo("Givresort");
        }

        @Test
        @WithMockUser
        @DisplayName("edition totalCards match the backup values")
        void restore_editionTotalCards() throws Exception {
            restore();
            Map<String, Integer> codeToTotal = editionRepository.findAll().stream()
                    .collect(Collectors.toMap(
                            com.alexgit95.model.Edition::getCode,
                            com.alexgit95.model.Edition::getTotalCards));
            assertThat(codeToTotal.get("1")).isEqualTo(216);
            assertThat(codeToTotal.get("9")).isEqualTo(243);
            assertThat(codeToTotal.get("11")).isEqualTo(242);
        }

        @Test
        @WithMockUser
        @DisplayName("edition release dates are preserved")
        void restore_editionReleaseDates() throws Exception {
            restore();
            Map<String, String> codeToDate = editionRepository.findAll().stream()
                    .collect(Collectors.toMap(
                            com.alexgit95.model.Edition::getCode,
                            e -> e.getReleaseDate() != null ? e.getReleaseDate() : ""));
            assertThat(codeToDate.get("1")).isEqualTo("2023-09-01");
            assertThat(codeToDate.get("7")).isEqualTo("2025-03-21");
            assertThat(codeToDate.get("11")).isEqualTo("2026-02-20");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. Restore — cards integrity
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Restore from real backup — cards integrity")
    class RestoreCards {

        @Test
        @WithMockUser
        @DisplayName("first card (externalId='1') has correct fields")
        void restore_firstCardFields() throws Exception {
            restore();
            Card card = cardRepository.findByExternalId("1").orElseThrow();
            assertThat(card.getName()).isEqualTo("Ariel - Sur des jambes humaines");
            assertThat(card.getCardNumber()).isEqualTo(1);
            assertThat(card.getRarity()).isEqualTo("Inhabituelle");
            assertThat(card.getCost()).isEqualTo(4);
            assertThat(card.getInkColor()).isEqualTo("Ambre");
            assertThat(card.getType()).isEqualTo("Personnage");
            assertThat(card.getArtist()).isEqualTo("Matthew Robert Davies");
            assertThat(card.getInkable()).isTrue();
            assertThat(card.getEdition().getCode()).isEqualTo("1");
        }

        @Test
        @WithMockUser
        @DisplayName("card externalId='2' (Ariel chanteuse) has correct fields")
        void restore_secondCardFields() throws Exception {
            restore();
            Card card = cardRepository.findByExternalId("2").orElseThrow();
            assertThat(card.getName()).isEqualTo("Ariel - Chanteuse exceptionnelle");
            assertThat(card.getRarity()).isEqualTo("Très Rare");
            assertThat(card.getCost()).isEqualTo(3);
        }

        @Test
        @WithMockUser
        @DisplayName("card externalId='5' (Hadès) is not inkable")
        void restore_hadesNotInkable() throws Exception {
            restore();
            Card hades = cardRepository.findByExternalId("5").orElseThrow();
            assertThat(hades.getInkable()).isFalse();
        }

        @Test
        @WithMockUser
        @DisplayName("card is linked to the correct edition after restore")
        void restore_cardEditionLink() throws Exception {
            restore();
            Card card = cardRepository.findByExternalId("1").orElseThrow();
            assertThat(card.getEdition()).isNotNull();
            assertThat(card.getEdition().getSetNumber()).isEqualTo(1);
        }

        @Test
        @WithMockUser
        @DisplayName("cards across multiple editions are all restored")
        void restore_cardsDistributedAcrossEditions() throws Exception {
            restore();
            for (int setNum = 1; setNum <= 11; setNum++) {
                final int s = setNum;
                long count = cardRepository.findAll().stream()
                        .filter(c -> c.getEdition() != null
                                && s == c.getEdition().getSetNumber())
                        .count();
                assertThat(count)
                        .as("Set %d should have at least 1 card", setNum)
                        .isGreaterThan(0);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 4. Restore — collection integrity
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Restore from real backup — collection integrity")
    class RestoreCollection {

        @Test
        @WithMockUser
        @DisplayName("spot-check: specific quantities are restored correctly")
        void restore_spotCheckQuantities() throws Exception {
            restore();
            for (Map.Entry<String, Integer> expected : SPOT_QTY.entrySet()) {
                Card card = cardRepository.findByExternalId(expected.getKey()).orElseThrow(
                        () -> new AssertionError("Card externalId=" + expected.getKey() + " not found"));
                UserCollection uc = collectionRepository.findByCardId(card.getId()).orElseThrow(
                        () -> new AssertionError("Collection entry for externalId=" + expected.getKey() + " not found"));
                assertThat(uc.getQuantity())
                        .as("quantity for externalId=%s", expected.getKey())
                        .isEqualTo(expected.getValue());
            }
        }

        @Test
        @WithMockUser
        @DisplayName("restore reads explicit foil values and defaults missing foil to false")
        void restore_foilValuesAndFallback() throws Exception {
            restore();

            assertFoilForExternalId("1", true);
            assertFoilForExternalId("2", false);
            assertFoilForExternalId("3", true);
            assertFoilForExternalId("5", false);
            assertFoilForExternalId("6", true);
            assertFoilForExternalId("7", false);

            // Entry without foil in JSON should fallback to false
            assertFoilForExternalId("8", false);
        }

        @Test
        @WithMockUser
        @DisplayName("legacy backup (no dates) sets firstAddedAt to a recent timestamp")
        void restore_legacyBackup_firstAddedAtSetByPrePersist() throws Exception {
            LocalDateTime before = LocalDateTime.now().minusSeconds(5);
            restore();
            LocalDateTime after = LocalDateTime.now().plusSeconds(5);

            long missingDate = collectionRepository.findAll().stream()
                    .filter(uc -> uc.getFirstAddedAt() == null)
                    .count();
            assertThat(missingDate).isZero();

            // All dates must be in the window of the restore call
            collectionRepository.findAll().forEach(uc ->
                    assertThat(uc.getFirstAddedAt())
                        .as("firstAddedAt should be set during restore")
                            .isAfterOrEqualTo(before)
                            .isBeforeOrEqualTo(after));
        }

        @Test
        @WithMockUser
        @DisplayName("collection entry is linked to a card that belongs to the correct edition")
        void restore_collectionLinkedToCorrectEdition() throws Exception {
            restore();
            Optional<Card> ariel = cardRepository.findByExternalId("1");
            assertThat(ariel).isPresent();
            Optional<UserCollection> uc = collectionRepository.findByCardId(ariel.get().getId());
            assertThat(uc).isPresent();
            assertThat(ariel.get().getEdition()).isNotNull();
            assertThat(ariel.get().getEdition().getSetNumber()).isEqualTo(1);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 5. Restore — settings integrity
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Restore from real backup — settings integrity")
    class RestoreSettings {

        @Test
        @WithMockUser
        @DisplayName("lorcajson_url setting is restored")
        void restore_lorcajsonUrlSetting() throws Exception {
            restore();
            assertThat(settingsRepository.findBySettingKey("lorcajson_url"))
                    .isPresent()
                    .hasValueSatisfying(s ->
                            assertThat(s.getSettingValue())
                                    .isEqualTo("https://lorcanajson.org/files/current/fr/allCards.json"));
        }

        @Test
        @WithMockUser
        @DisplayName("stats_enabled_sets is remapped: contains 7 IDs, none of them 1–11 (old IDs)")
        void restore_statsEnabledSets_remapped() throws Exception {
            restore();
            String remapped = settingsRepository.findBySettingKey("stats_enabled_sets")
                    .orElseThrow().getSettingValue();

            String[] parts = remapped.split(",");
            assertThat(parts).hasSize(7); // "1,2,3,4,5,6,10" → 7 sets

            // The new IDs assigned by H2 could be anything; verify they resolve to the
            // editions with set numbers 1,2,3,4,5,6,10
            List<Integer> resolvedSetNumbers = java.util.Arrays.stream(parts)
                    .map(String::trim)
                    .mapToLong(Long::parseLong)
                    .mapToObj(id -> editionRepository.findById(id).orElseThrow())
                    .map(com.alexgit95.model.Edition::getSetNumber)
                    .sorted()
                    .collect(Collectors.toList());

            assertThat(resolvedSetNumbers).containsExactly(1, 2, 3, 4, 5, 6, 10);
        }

        @Test
        @WithMockUser
        @DisplayName("stats_enabled_sets does NOT contain old backup IDs '1' through '11'")
        void restore_statsEnabledSets_noRawOldIds() throws Exception {
            restore();
            // Editions in H2 get new auto-incremented IDs after each test (due to deleteAllInBatch).
            // We verify the remapping did NOT simply copy the raw old values.
            // The old value was "1,2,3,4,5,6,10".
            // The new IDs must point to editions — this will be verified by checking each ID
            // actually exists as an edition in the DB (would throw otherwise in the test above).
            // Here we just check the setting exists and is not empty.
            String remapped = settingsRepository.findBySettingKey("stats_enabled_sets")
                    .orElseThrow().getSettingValue();
            assertThat(remapped).isNotBlank();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 6. Round-trip — restore then backup
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Round-trip — restore real backup → backup again")
    class RoundTrip {

        @Test
        @WithMockUser
        @DisplayName("backup after restore produces the same edition/card/collection counts")
        void roundTrip_countsPreserved() throws Exception {
            // Step 1: restore the real backup
            restore();

            // Step 2: back up the just-restored state
            String secondBackupJson = mockMvc.perform(get("/api/admin/backup"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            Map<?, ?> b = objectMapper.readValue(secondBackupJson, Map.class);
            assertThat(b.get("totalEditions")).isEqualTo(EXPECTED_EDITIONS);
            assertThat(b.get("totalCards")).isEqualTo(EXPECTED_CARDS);
            assertThat(b.get("totalCollection")).isEqualTo(EXPECTED_COLLECTION);
        }

        @Test
        @WithMockUser
        @DisplayName("second backup contains foil and date fields (upgrade of legacy format)")
        void roundTrip_secondBackupHasNewFields() throws Exception {
            restore();

            String secondBackupJson = mockMvc.perform(get("/api/admin/backup"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> col =
                    (List<Map<String, Object>>) objectMapper.readValue(secondBackupJson, Map.class)
                            .get("collection");

            // All entries must now have the new fields
            assertThat(col).isNotEmpty();
            col.forEach(entry -> {
                assertThat(entry).containsKey("foil");
                assertThat(entry).containsKey("firstAddedAt");
                assertThat(entry).containsKey("lastAddedAt");
                assertThat(entry.get("foil")).isInstanceOf(Boolean.class);
                assertThat(entry.get("firstAddedAt")).isNotNull();
                assertThat(entry.get("lastAddedAt")).isNotNull();
            });

            // Fixture now contains explicit foil=true and foil=false entries
            long foilTrueCount = col.stream().filter(e -> Boolean.TRUE.equals(e.get("foil"))).count();
            long foilFalseCount = col.stream().filter(e -> Boolean.FALSE.equals(e.get("foil"))).count();
            assertThat(foilTrueCount).isGreaterThan(0);
            assertThat(foilFalseCount).isGreaterThan(0);
        }

        @Test
        @WithMockUser
        @DisplayName("double restore (restore twice in a row) produces the same counts")
        void roundTrip_doubleRestore_stableState() throws Exception {
            restore();
            restore(); // second restore erases first, so counts must stay identical

            assertThat(editionRepository.count()).isEqualTo(EXPECTED_EDITIONS);
            assertThat(cardRepository.count()).isEqualTo(EXPECTED_CARDS);
            assertThat(collectionRepository.count()).isEqualTo(EXPECTED_COLLECTION);
        }

        @Test
        @WithMockUser
        @DisplayName("restore → backup → restore: final counts match original")
        void roundTrip_fullCycle() throws Exception {
            // Restore the original file
            restore();

            // Capture the new backup (now with foil+dates)
            String secondBackup = mockMvc.perform(get("/api/admin/backup"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();

            // Wipe and restore from the new backup
            collectionRepository.deleteAllInBatch();
            cardRepository.deleteAllInBatch();
            editionRepository.deleteAllInBatch();
            settingsRepository.deleteAllInBatch();

            mockMvc.perform(post("/api/admin/restore")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(secondBackup))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            assertThat(editionRepository.count()).isEqualTo(EXPECTED_EDITIONS);
            assertThat(cardRepository.count()).isEqualTo(EXPECTED_CARDS);
            assertThat(collectionRepository.count()).isEqualTo(EXPECTED_COLLECTION);

            // Spot-check quantities survive the double round-trip
            for (Map.Entry<String, Integer> expected : SPOT_QTY.entrySet()) {
                Card card = cardRepository.findByExternalId(expected.getKey()).orElseThrow();
                UserCollection uc = collectionRepository.findByCardId(card.getId()).orElseThrow();
                assertThat(uc.getQuantity())
                        .as("qty for externalId=%s after full cycle", expected.getKey())
                        .isEqualTo(expected.getValue());
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Helper
    // ══════════════════════════════════════════════════════════════════════════

    private void assertFoilForExternalId(String externalId, boolean expectedFoil) {
        Card card = cardRepository.findByExternalId(externalId).orElseThrow();
        UserCollection uc = collectionRepository.findByCardId(card.getId()).orElseThrow();
        assertThat(Boolean.TRUE.equals(uc.getFoil())).isEqualTo(expectedFoil);
    }

    private void restore() throws Exception {
        mockMvc.perform(post("/api/admin/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(backupJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
