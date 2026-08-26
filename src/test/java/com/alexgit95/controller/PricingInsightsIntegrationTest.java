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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @BeforeEach
    void clean() {
        collectionRepository.deleteAllInBatch();
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

        assertThat(payload.get("currency")).isEqualTo("EUR");
        assertThat(latest).hasSize(2);
        assertThat(latest.get(0).get("name")).isEqualTo("Card newest");
        assertThat(latest.get(1).get("name")).isEqualTo("Card older");
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
