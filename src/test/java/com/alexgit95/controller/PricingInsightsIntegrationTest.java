package com.alexgit95.controller;

import com.alexgit95.model.AppSettings;
import com.alexgit95.model.Card;
import com.alexgit95.model.CollectionValueSnapshot;
import com.alexgit95.model.Edition;
import com.alexgit95.model.EditionValueSnapshot;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.CollectionValueSnapshotRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.EditionValueSnapshotRepository;
import com.alexgit95.repository.UserCollectionRepository;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.profiles.active=test")
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class PricingInsightsIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private EditionRepository editionRepository;
    @Autowired private CardRepository cardRepository;
    @Autowired private UserCollectionRepository collectionRepository;
    @Autowired private AppSettingsRepository settingsRepository;
    @Autowired private CollectionValueSnapshotRepository collectionValueSnapshotRepository;
    @Autowired private EditionValueSnapshotRepository editionValueSnapshotRepository;

    @BeforeEach
    void clean() {
        collectionRepository.deleteAllInBatch();
        editionValueSnapshotRepository.deleteAllInBatch();
        collectionValueSnapshotRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        editionRepository.deleteAllInBatch();
        settingsRepository.deleteAllInBatch();
    }

    @Test
    @WithMockUser
    @DisplayName("insights returns latest priced cards in descending lastPriceAt order")
    void insights_returnsLatestPricedOrder() throws Exception {
        Edition ed = saveEdition("TFC", "Premier Chapitre", 1);
        Card older = saveCard(ed, "older", new BigDecimal("1.00"), "EUR", LocalDateTime.now().minusDays(2));
        Card newest = saveCard(ed, "newest", new BigDecimal("2.00"), "EUR", LocalDateTime.now().minusHours(1));
        Card nonEur = saveCard(ed, "usd", new BigDecimal("3.00"), "USD", LocalDateTime.now());

        saveCollection(older, 1, 0);
        saveCollection(newest, 1, 0);
        saveCollection(nonEur, 1, 0);

        String json = mockMvc.perform(get("/api/pricing/insights"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> payload = objectMapper.readValue(json, new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> latest = (List<Map<String, Object>>) payload.get("latestPricedCards");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ranking = (List<Map<String, Object>>) payload.get("ownedCardPriceRanking");

        assertThat(payload.get("currency")).isEqualTo("EUR");
        assertThat(latest).hasSize(2);
        assertThat(latest.get(0).get("name")).isEqualTo("Card newest");
        assertThat(latest.get(1).get("name")).isEqualTo("Card older");
        assertThat(ranking).hasSize(2);
        assertThat(ranking.get(0).get("name")).isEqualTo("Card newest");
        assertThat(ranking.get(0).get("quantity")).isEqualTo(1);
    }

    @Test
    @WithMockUser
    @DisplayName("insights valuation respects tracked edition scope and formula")
    void insights_respectsTrackedEditionsAndFormula() throws Exception {
        Edition tracked = saveEdition("TFC", "Premier Chapitre", 1);
        Edition ignored = saveEdition("ROF", "Floodborn", 2);

        Card trackedCard = saveCard(tracked, "tracked", new BigDecimal("4.00"), "EUR", LocalDateTime.now().minusHours(1));
        Card ignoredCard = saveCard(ignored, "ignored", new BigDecimal("10.00"), "EUR", LocalDateTime.now().minusHours(2));
        Card missingPrice = saveCard(tracked, "missing", null, "EUR", LocalDateTime.now().minusHours(3));

        saveCollection(trackedCard, 2, 1);
        saveCollection(ignoredCard, 1, 0);
        saveCollection(missingPrice, 1, 0);

        settingsRepository.save(new AppSettings("stats_enabled_sets", String.valueOf(tracked.getId()), "test"));

        String json = mockMvc.perform(get("/api/pricing/insights"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> payload = objectMapper.readValue(json, new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> valuations = (List<Map<String, Object>>) payload.get("editionValuations");

        assertThat(valuations).hasSize(1);
        assertThat(valuations.get(0).get("editionCode")).isEqualTo("TFC");
        assertThat(payload.get("totalCollectionValueEur")).isEqualTo(12.0);
        assertThat(payload.get("excludedNoPrice")).isEqualTo(1);
    }

    @Test
    @WithMockUser
    @DisplayName("removing a price preserves normal and foil collection quantities")
    void removePrice_preservesCollectionQuantities() throws Exception {
        Edition edition = saveEdition("TFC", "Premier Chapitre", 1);
        Card card = saveCard(edition, "priced", new BigDecimal("12.00"), "EUR", LocalDateTime.now());
        saveCollection(card, 2, 1);

        String json = mockMvc.perform(delete("/api/pricing/cards/{cardId}/price", card.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marketPrice").doesNotExist())
                .andExpect(jsonPath("$.quantity").value(2))
                .andExpect(jsonPath("$.foilQuantity").value(1))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> response = objectMapper.readValue(json, new TypeReference<>() {});
        assertThat(response.get("marketPrice")).isNull();
        assertThat(cardRepository.findById(card.getId()).orElseThrow().getMarketPrice()).isNull();
        UserCollection collection = collectionRepository.findByCardId(card.getId()).orElseThrow();
        assertThat(collection.getQuantity()).isEqualTo(2);
        assertThat(collection.getFoilQuantity()).isEqualTo(1);

        String insightsJson = mockMvc.perform(get("/api/pricing/insights"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Map<String, Object> insights = objectMapper.readValue(insightsJson, new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> ranking = (List<Map<String, Object>>) insights.get("ownedCardPriceRanking");
        assertThat(ranking).isEmpty();
    }

    @Test
    @WithMockUser
    @DisplayName("trend endpoints return ordered global points and edition deltas")
    void trendEndpointsReturnOrderedPointsAndEditionDeltas() throws Exception {
        Edition edition = saveEdition("TFC", "Premier Chapitre", 1);
        LocalDateTime now = LocalDateTime.now().withNano(0);

        saveGlobalSnapshot(now.minusDays(30), "100.00");
        saveGlobalSnapshot(now.minusDays(7), "120.00");
        saveGlobalSnapshot(now, "150.00");
        saveEditionSnapshot(edition, now.minusDays(30), "100.00");
        saveEditionSnapshot(edition, now.minusDays(7), "120.00");
        saveEditionSnapshot(edition, now, "150.00");

        String trendJson = mockMvc.perform(get("/api/pricing/trend"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trend").isArray())
                .andExpect(jsonPath("$.trend.length()").value(3))
                .andExpect(jsonPath("$.trend[0].totalCollectionValueEur").value(100.0))
                .andExpect(jsonPath("$.trend[2].totalCollectionValueEur").value(150.0))
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> trendPayload = objectMapper.readValue(trendJson, new TypeReference<>() {});
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trend = (List<Map<String, Object>>) trendPayload.get("trend");
        assertThat(trend.get(0).get("recordedAt").toString())
                .isLessThan(trend.get(1).get("recordedAt").toString());

        mockMvc.perform(get("/api/pricing/edition-deltas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].editionCode").value("TFC"))
                .andExpect(jsonPath("$[0].currentValueEur").value(150.0))
                .andExpect(jsonPath("$[0].value7dEur").value(120.0))
                .andExpect(jsonPath("$[0].value30dEur").value(100.0))
                .andExpect(jsonPath("$[0].delta7dPercent").value(25.0))
                .andExpect(jsonPath("$[0].delta30dPercent").value(50.0));
    }

    @Test
    @WithMockUser
    @DisplayName("recompute-value persists a fresh snapshot from current collection")
    void recomputeValue_persistsFreshSnapshot() throws Exception {
        Edition edition = saveEdition("TFC", "Premier Chapitre", 1);
        Card card = saveCard(edition, "priced", new BigDecimal("5.00"), "EUR", LocalDateTime.now());
        saveCollection(card, 2, 0);

        assertThat(collectionValueSnapshotRepository.findAllByOrderByRecordedAtAsc()).isEmpty();

        mockMvc.perform(post("/api/pricing/recompute-value"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        List<CollectionValueSnapshot> snapshots = collectionValueSnapshotRepository.findAllByOrderByRecordedAtAsc();
        assertThat(snapshots).hasSize(1);
        assertThat(snapshots.get(0).getTotalCollectionValueEur()).isEqualByComparingTo("10.00");
        assertThat(editionValueSnapshotRepository.findByEditionIdOrderByRecordedAtAsc(edition.getId())).hasSize(1);
    }

    private void saveGlobalSnapshot(LocalDateTime recordedAt, String value) {
        CollectionValueSnapshot snapshot = new CollectionValueSnapshot();
        snapshot.setRecordedAt(recordedAt);
        snapshot.setTotalCollectionValueEur(new BigDecimal(value));
        snapshot.setCurrency("EUR");
        snapshot.setSource("PRICING_SYNC");
        collectionValueSnapshotRepository.save(snapshot);
    }

    private void saveEditionSnapshot(Edition edition, LocalDateTime recordedAt, String value) {
        EditionValueSnapshot snapshot = new EditionValueSnapshot();
        snapshot.setRecordedAt(recordedAt);
        snapshot.setEditionId(edition.getId());
        snapshot.setEditionCode(edition.getCode());
        snapshot.setEditionName(edition.getName());
        snapshot.setTotalValueEur(new BigDecimal(value));
        editionValueSnapshotRepository.save(snapshot);
    }

    private Edition saveEdition(String code, String name, Integer setNumber) {
        Edition edition = new Edition();
        edition.setCode(code);
        edition.setName(name);
        edition.setSetNumber(setNumber);
        return editionRepository.save(edition);
    }

    private Card saveCard(Edition edition,
                          String externalId,
                          BigDecimal marketPrice,
                          String currency,
                          LocalDateTime lastPriceAt) {
        Card card = new Card();
        card.setEdition(edition);
        card.setExternalId(externalId);
        card.setName("Card " + externalId);
        card.setCardNumber(Math.abs(externalId.hashCode()) % 500 + 1);
        card.setMarketPrice(marketPrice);
        card.setPriceCurrency(currency);
        card.setLastPriceAt(lastPriceAt);
        return cardRepository.save(card);
    }

    private void saveCollection(Card card, int quantity, int foilQuantity) {
        UserCollection uc = new UserCollection();
        uc.setCard(card);
        uc.setQuantity(quantity);
        uc.setFoilQuantity(foilQuantity);
        uc.setFoil(foilQuantity > 0);
        collectionRepository.save(uc);
    }
}
