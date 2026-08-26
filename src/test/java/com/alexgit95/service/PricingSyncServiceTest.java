package com.alexgit95.service;

import com.alexgit95.model.Card;
import com.alexgit95.repository.CardRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingSyncServiceTest {

    @Mock
    private CardRepository cardRepository;

    @Mock
    private PricingSettingsService pricingSettingsService;

    @Mock
    private PricingProviderClient pricingProviderClient;

    private PricingSyncService pricingSyncService;

    @BeforeEach
    void setUp() {
        pricingSyncService = new PricingSyncService(cardRepository, pricingSettingsService, pricingProviderClient);
    }

    @Test
        @DisplayName("runSync applies priority tiers when updating mapped cards")
    void runSync_prioritizesMissingThenOldest() {
        Card missing = card("missing", null);
        missing.setMarketPrice(null);
        Card oldest = card("oldest", LocalDateTime.of(2025, 1, 1, 0, 0));
        oldest.setMarketPrice(new BigDecimal("8.00"));
        Card newest = card("newest", LocalDateTime.now().minusDays(1));
        newest.setMarketPrice(new BigDecimal("9.00"));

        mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
        mockQueueCounts(0L, 3L);

        when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(Map.of("id", 1L)),
            new PricingProviderClient.Paging(1, 1, 1)
        ));

        List<Map<String, Object>> rows = List.of(
            row("TFC", 3, "13.00"),
            row("TFC", 2, "12.00"),
            row("TFC", 1, "11.00")
        );
        when(pricingProviderClient.fetchEpisodeCardsPage(1L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
            rows,
            new PricingProviderClient.Paging(1, 1, 100)
        ));

        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 1)).thenReturn(java.util.Optional.of(missing));
        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 2)).thenReturn(java.util.Optional.of(oldest));
        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 3)).thenReturn(java.util.Optional.of(newest));

        Map<String, Object> report = pricingSyncService.runSync("manual", 3);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        org.mockito.Mockito.verify(cardRepository, atLeast(3)).save(captor.capture());
        List<Card> saved = captor.getAllValues();

        assertThat(saved.get(0).getExternalId()).isEqualTo("missing");
        assertThat(saved.get(1).getExternalId()).isEqualTo("oldest");
        assertThat(saved.get(2).getExternalId()).isEqualTo("newest");

        assertThat(report.get("attempted")).isEqualTo(2);
        assertThat(report.get("successCount")).isEqualTo(3);
        assertThat(report.get("budgetBlocked")).isEqualTo(false);
    }

        @Test
        @DisplayName("runSync stops immediately when budget is exhausted")
        void runSync_stopsWhenBudgetExhausted() {
        Card missing = card("missing", null);

        when(pricingSettingsService.isSyncEnabled()).thenReturn(true);
        when(pricingSettingsService.getCursor()).thenReturn(PricingSettingsService.CursorState.initial());
        when(pricingSettingsService.getMinuteLimit()).thenReturn(30);
        when(pricingSettingsService.hasRemainingAttempts()).thenReturn(false);
        when(pricingSettingsService.getBudgetStatus()).thenReturn(Map.of(
            "dailyBudget", 1,
            "usedAttempts", 1,
            "remainingAttempts", 0
        ));

        when(cardRepository.countByMarketPriceIsNull()).thenReturn(1L);
        when(cardRepository.countByLastPriceAtIsNotNull()).thenReturn(0L);
        when(cardRepository.findByMarketPriceIsNotNullAndLastPriceAtBeforeOrderByLastPriceAtAscIdAsc(any(LocalDateTime.class)))
            .thenReturn(List.of());

        Map<String, Object> report = pricingSyncService.runSync("manual", 10);

        assertThat(report.get("attempted")).isEqualTo(0);
        assertThat(report.get("budgetBlocked")).isEqualTo(true);
        verify(pricingProviderClient, never()).fetchEpisodesPage(any(Integer.class));
        }

        @Test
        @DisplayName("runSync reports unresolved mapping and provider page errors")
        void runSync_reportsUnresolvedAndErrors() {
        Card mapped = card("mapped", null);
        mapped.setMarketPrice(null);

            mockEnabledSyncWithBudget(3, 3, new boolean[]{true, true, true, false}, new boolean[]{true, true, true, false});
            mockQueueCounts(1L, 1L);

        when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(Map.of("id", 1L)),
            new PricingProviderClient.Paging(1, 1, 1)
        ));
        when(pricingProviderClient.fetchEpisodeCardsPage(1L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(row("TFC", 1, "10.00"), new LinkedHashMap<>()),
            new PricingProviderClient.Paging(1, 1, 100)
        ));
        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 1)).thenReturn(java.util.Optional.of(mapped));

        Map<String, Object> report = pricingSyncService.runSync("manual", null);

        assertThat(report.get("attempted")).isEqualTo(2);
        assertThat(report.get("successCount")).isEqualTo(1);
        assertThat(report.get("unresolvedCount")).isEqualTo(1);
        assertThat(report.get("errorCount")).isEqualTo(0);
        }

    @Test
    @DisplayName("runSync does not consume attempt when provider config is missing")
    void runSync_configMissingDoesNotConsumeAttempt() {
        Card missing = card("missing", null);

        when(pricingSettingsService.isSyncEnabled()).thenReturn(true);
        when(pricingSettingsService.getCursor()).thenReturn(PricingSettingsService.CursorState.initial());
        when(pricingSettingsService.getMinuteLimit()).thenReturn(30);
        when(pricingSettingsService.hasRemainingAttempts()).thenReturn(true);
        when(pricingSettingsService.getBudgetStatus()).thenReturn(Map.of(
            "dailyBudget", 5,
            "usedAttempts", 0,
            "remainingAttempts", 5
        ));

        when(cardRepository.countByMarketPriceIsNull()).thenReturn(1L);
        when(cardRepository.countByLastPriceAtIsNotNull()).thenReturn(0L);
        when(cardRepository.findByMarketPriceIsNotNullAndLastPriceAtBeforeOrderByLastPriceAtAscIdAsc(any(LocalDateTime.class)))
            .thenReturn(List.of());

        when(pricingProviderClient.fetchEpisodesPage(1))
            .thenReturn(PricingProviderClient.PagedResult.configMissing("Provider settings are incomplete"));

        Map<String, Object> report = pricingSyncService.runSync("manual", null);

        assertThat(report.get("attempted")).isEqualTo(0);
        assertThat(report.get("reasonCode")).isEqualTo("PROVIDER_CONFIG_MISSING");
        verify(pricingSettingsService, never()).tryConsumeAttempt();
        verify(cardRepository, never()).save(any(Card.class));
    }

        @Test
        @DisplayName("runSync resolves card with episode set number plus card number")
        void runSync_resolvesByEpisodeSetNumberAndCardNumber() {
            Card localCard = card("ext-123", null);

            mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
            mockQueueCounts(1L, 0L);

            when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
                    List.of(Map.of("id", 206L, "set_num", 6)),
                    new PricingProviderClient.Paging(1, 1, 1)
            ));
            when(pricingProviderClient.fetchEpisodeCardsPage(206L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
                    List.of(Map.of(
                            "card_number", 22,
                            "price", "1.25"
                    )),
                    new PricingProviderClient.Paging(1, 1, 100)
            ));

            when(cardRepository.findByEditionSetNumberAndCardNumber(6, 22)).thenReturn(java.util.Optional.of(localCard));

            Map<String, Object> report = pricingSyncService.runSync("manual", null);

            assertThat(report.get("successCount")).isEqualTo(1);
            verify(cardRepository).save(localCard);
            assertThat(localCard.getMarketPrice()).isEqualByComparingTo("1.25");
        }

            @Test
            @DisplayName("runSync resolves card with episode code-derived edition id plus card number")
            void runSync_resolvesByEpisodeCodeEditionIdAndCardNumber() {
            Card localCard = card("ext-episode-code", null);

            mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
            mockQueueCounts(1L, 0L);

            when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(Map.of("id", 401L, "code", "11WSP")),
                new PricingProviderClient.Paging(1, 1, 1)
            ));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("card_number", 143);
            row.put("price", "0.04");

            when(pricingProviderClient.fetchEpisodeCardsPage(401L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(row),
                new PricingProviderClient.Paging(1, 1, 100)
            ));

            when(cardRepository.findByCardNumberAndEditionId(143, 11L)).thenReturn(java.util.Optional.of(localCard));

            Map<String, Object> report = pricingSyncService.runSync("manual", null);

            assertThat(report.get("successCount")).isEqualTo(1);
            verify(cardRepository).findByCardNumberAndEditionId(143, 11L);
            verify(cardRepository).save(localCard);
            }

            @Test
            @DisplayName("runSync falls back when episode code has no leading digits")
            void runSync_fallsBackWhenEpisodeCodeHasNoLeadingDigits() {
            Card mapped = card("fallback", null);

            mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
            mockQueueCounts(1L, 0L);

            when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(Map.of("id", 402L, "code", "WSP11")),
                new PricingProviderClient.Paging(1, 1, 1)
            ));

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("set_code", "TFC");
            row.put("card_number", 124);
            row.put("price", "1.23");

            when(pricingProviderClient.fetchEpisodeCardsPage(402L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(row),
                new PricingProviderClient.Paging(1, 1, 100)
            ));
            when(cardRepository.findByEditionCodeAndCardNumber("TFC", 124)).thenReturn(java.util.Optional.of(mapped));

            Map<String, Object> report = pricingSyncService.runSync("manual", null);

            assertThat(report.get("successCount")).isEqualTo(1);
            verify(cardRepository, never()).findByCardNumberAndEditionId(any(Integer.class), anyLong());
            verify(cardRepository).findByEditionCodeAndCardNumber("TFC", 124);
            }

            @Test
            @DisplayName("runSync maps fixture-like row from retourPriceAPI payload")
            void runSync_mapsFixtureLikeRowFromRetourPriceApi() throws Exception {
            Card localCard = card("fixture-143", null);

            mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
            mockQueueCounts(1L, 0L);

            Map<String, Object> fixtureRow = loadFixtureRowByCardNumber(143);

            when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(Map.of("id", 401L)),
                new PricingProviderClient.Paging(1, 1, 1)
            ));
            when(pricingProviderClient.fetchEpisodeCardsPage(401L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(fixtureRow),
                new PricingProviderClient.Paging(1, 1, 100)
            ));

            when(cardRepository.findByCardNumberAndEditionId(143, 11L)).thenReturn(java.util.Optional.of(localCard));

            Map<String, Object> report = pricingSyncService.runSync("manual", null);

            assertThat(report.get("successCount")).isEqualTo(1);
            verify(cardRepository).findByCardNumberAndEditionId(143, 11L);
            verify(cardRepository).save(localCard);
            assertThat(localCard.getMarketPrice()).isEqualByComparingTo("0.02");
            }

            @Test
            @DisplayName("runSync prefers cardmarket lowest_near_mint_FR_EU_only over other price fields")
            void runSync_prefersCardmarketLowestNearMintFrEuOnly() {
            Card localCard = card("ext-fr-eu-price", null);

            mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
            mockQueueCounts(1L, 0L);

            when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(Map.of("id", 401L, "code", "11WSP")),
                new PricingProviderClient.Paging(1, 1, 1)
            ));

            Map<String, Object> providerRow = new LinkedHashMap<>();
            providerRow.put("card_number", 166);
            providerRow.put("prices", Map.of(
                "cardmarket", Map.of(
                    "lowest_near_mint", 0.04,
                    "lowest_near_mint_FR_EU_only", 0.09,
                    "30d_average", 999.99,
                    "7d_average", 888.88
                ),
                "tcg_player", Map.of("currency", "EUR", "market_price", 777.77)
            ));

            when(pricingProviderClient.fetchEpisodeCardsPage(401L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(providerRow),
                new PricingProviderClient.Paging(1, 1, 100)
            ));
            when(cardRepository.findByCardNumberAndEditionId(166, 11L)).thenReturn(java.util.Optional.of(localCard));

            Map<String, Object> report = pricingSyncService.runSync("manual", null);

            assertThat(report.get("successCount")).isEqualTo(1);
            assertThat(localCard.getMarketPrice()).isEqualByComparingTo("0.09");
            }

        @Test
        @DisplayName("runSync parses nested price with comma decimal")
        void runSync_parsesNestedCommaPrice() {
        Card mapped = card("mapped", null);

            mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
            mockQueueCounts(1L, 0L);

        when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(Map.of("id", 213L)),
            new PricingProviderClient.Paging(1, 1, 1)
        ));

        Map<String, Object> providerRow = new LinkedHashMap<>();
        providerRow.put("set_code", "TFC");
        providerRow.put("card_number", 124);
        providerRow.put("prices", Map.of(
            "market", Map.of("eur", "1,23 €")
        ));

        when(pricingProviderClient.fetchEpisodeCardsPage(213L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(providerRow),
            new PricingProviderClient.Paging(1, 1, 100)
        ));

        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 124)).thenReturn(java.util.Optional.of(mapped));

        Map<String, Object> report = pricingSyncService.runSync("manual", null);

        assertThat(report.get("successCount")).isEqualTo(1);
        assertThat(mapped.getMarketPrice()).isEqualByComparingTo("1.23");
        }

    private static Map<String, Object> row(String setCode, int cardNumber, String marketPrice) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("set_code", setCode);
        row.put("card_number", cardNumber);
        row.put("marketPrice", marketPrice);
        return row;
    }

    private void mockEnabledSyncWithBudget(int dailyBudget,
                                           int usedAttempts,
                                           boolean[] hasRemainingAttempts,
                                           boolean[] consumeAttempts) {
        when(pricingSettingsService.isSyncEnabled()).thenReturn(true);
        when(pricingSettingsService.getCursor()).thenReturn(PricingSettingsService.CursorState.initial());
        when(pricingSettingsService.getMinuteLimit()).thenReturn(30);
        when(pricingSettingsService.getProviderName()).thenReturn("rapidapi");
        when(pricingSettingsService.getProviderCurrency()).thenReturn("EUR");

        when(pricingSettingsService.getBudgetStatus()).thenReturn(Map.of(
                "dailyBudget", dailyBudget,
                "dailyHardLimit", 100,
                "dailySafetyMargin", 0,
                "effectiveDailyBudget", dailyBudget,
                "minuteLimit", 30,
                "usedAttempts", usedAttempts,
                "remainingAttempts", Math.max(0, dailyBudget - usedAttempts)
        ));

        stubBooleanSequence(() -> when(pricingSettingsService.hasRemainingAttempts()), hasRemainingAttempts);
        if (consumeAttempts != null && consumeAttempts.length > 0) {
            stubBooleanSequence(() -> when(pricingSettingsService.tryConsumeAttempt()), consumeAttempts);
        }
    }

    private void mockQueueCounts(long queueWithoutPrice, long queueWithPrice) {
        when(cardRepository.countByMarketPriceIsNull()).thenReturn(queueWithoutPrice);
        when(cardRepository.countByLastPriceAtIsNotNull()).thenReturn(queueWithPrice);
        when(cardRepository.findByMarketPriceIsNotNullAndLastPriceAtBeforeOrderByLastPriceAtAscIdAsc(any(LocalDateTime.class)))
                .thenReturn(List.of());
    }

    private void stubBooleanSequence(java.util.function.Supplier<org.mockito.stubbing.OngoingStubbing<Boolean>> stubbing,
                                     boolean[] values) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException("Boolean sequence cannot be empty");
        }
        Boolean[] boxed = new Boolean[values.length];
        for (int i = 0; i < values.length; i++) {
            boxed[i] = values[i];
        }
        if (boxed.length == 1) {
            stubbing.get().thenReturn(boxed[0]);
            return;
        }
        Boolean[] tail = new Boolean[boxed.length - 1];
        System.arraycopy(boxed, 1, tail, 0, tail.length);
        stubbing.get().thenReturn(boxed[0], tail);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadFixtureRowByCardNumber(int cardNumber) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        InputStream in = getClass().getResourceAsStream("/retourPriceAPI.json");
        assertThat(in).isNotNull();

        Map<String, Object> payload = mapper.readValue(in, new TypeReference<Map<String, Object>>() {
        });
        Object dataNode = payload.get("data");
        assertThat(dataNode).isInstanceOf(List.class);

        for (Object raw : (List<?>) dataNode) {
            if (!(raw instanceof Map<?, ?> rawMap)) {
                continue;
            }
            Object numberNode = rawMap.get("card_number");
            if (!(numberNode instanceof Number n)) {
                continue;
            }
            if (n.intValue() == cardNumber) {
                Map<String, Object> converted = new LinkedHashMap<>();
                for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                    converted.put(String.valueOf(entry.getKey()), entry.getValue());
                }
                return converted;
            }
        }

        throw new IllegalStateException("No fixture row found for card_number=" + cardNumber);
    }

    private static Card card(String externalId, LocalDateTime lastPriceAt) {
        Card card = new Card();
        card.setExternalId(externalId);
        card.setName(externalId);
        card.setLastPriceAt(lastPriceAt);
        return card;
    }
}
