package com.alexgit95.service;

import com.alexgit95.model.Card;
import com.alexgit95.repository.CardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

        when(pricingSettingsService.isSyncEnabled()).thenReturn(true);
        when(pricingSettingsService.getCursor()).thenReturn(PricingSettingsService.CursorState.initial());
        when(pricingSettingsService.getMinuteLimit()).thenReturn(30);
        when(pricingSettingsService.hasRemainingAttempts()).thenReturn(true, true, false);
        when(pricingSettingsService.tryConsumeAttempt()).thenReturn(true, true, false);
        when(pricingSettingsService.getProviderName()).thenReturn("rapidapi");
        when(pricingSettingsService.getProviderCurrency()).thenReturn("EUR");
        when(pricingSettingsService.getBudgetStatus()).thenReturn(Map.of(
            "dailyBudget", 2,
            "dailyHardLimit", 100,
            "dailySafetyMargin", 0,
            "effectiveDailyBudget", 2,
            "minuteLimit", 30,
            "usedAttempts", 2,
                "remainingAttempts", 0
        ));

        when(cardRepository.countByMarketPriceIsNull()).thenReturn(0L);
        when(cardRepository.countByLastPriceAtIsNotNull()).thenReturn(3L);
        when(cardRepository.findByMarketPriceIsNotNullAndLastPriceAtBeforeOrderByLastPriceAtAscIdAsc(any(LocalDateTime.class)))
            .thenReturn(List.of());

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

        when(pricingSettingsService.isSyncEnabled()).thenReturn(true);
        when(pricingSettingsService.getCursor()).thenReturn(PricingSettingsService.CursorState.initial());
        when(pricingSettingsService.getMinuteLimit()).thenReturn(30);
        when(pricingSettingsService.hasRemainingAttempts()).thenReturn(true, true, true, false);
        when(pricingSettingsService.tryConsumeAttempt()).thenReturn(true, true, true, false);
        when(pricingSettingsService.getProviderName()).thenReturn("rapidapi");
        when(pricingSettingsService.getProviderCurrency()).thenReturn("EUR");
        when(pricingSettingsService.getBudgetStatus()).thenReturn(Map.of(
            "dailyBudget", 3,
            "dailyHardLimit", 100,
            "dailySafetyMargin", 0,
            "effectiveDailyBudget", 3,
            "minuteLimit", 30,
            "usedAttempts", 3,
            "remainingAttempts", 0
        ));

        when(cardRepository.countByMarketPriceIsNull()).thenReturn(1L);
        when(cardRepository.countByLastPriceAtIsNotNull()).thenReturn(1L);
        when(cardRepository.findByMarketPriceIsNotNullAndLastPriceAtBeforeOrderByLastPriceAtAscIdAsc(any(LocalDateTime.class)))
            .thenReturn(List.of());

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

    private static Map<String, Object> row(String setCode, int cardNumber, String marketPrice) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("set_code", setCode);
        row.put("card_number", cardNumber);
        row.put("marketPrice", marketPrice);
        return row;
    }

    private static Card card(String externalId, LocalDateTime lastPriceAt) {
        Card card = new Card();
        card.setExternalId(externalId);
        card.setName(externalId);
        card.setLastPriceAt(lastPriceAt);
        return card;
    }
}
