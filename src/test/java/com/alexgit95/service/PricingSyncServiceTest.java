package com.alexgit95.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import java.util.Set;

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

    @Mock
    private CollectionValueTrendService collectionValueTrendService;

    private PricingSyncService pricingSyncService;

    @BeforeEach
    void setUp() {
        pricingSyncService = new PricingSyncService(cardRepository, pricingSettingsService, pricingProviderClient, collectionValueTrendService);
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
    @DisplayName("High market price log is emitted when enabled and suppressed when disabled")
    void runSync_highPriceLog_respectsToggle() {
        Card mapped = card("mapped", null);
        mapped.setMarketPrice(null);

        mockEnabledSyncWithBudget(1, 0, new boolean[]{true}, new boolean[]{true});
        mockQueueCounts(1L, 0L);
        when(pricingSettingsService.isHighPriceLogEnabled()).thenReturn(true);
        when(pricingSettingsService.getHighPriceLogThreshold()).thenReturn(10);

        when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(Map.of("id", 1L)),
            new PricingProviderClient.Paging(1, 1, 1)
        ));
        when(pricingProviderClient.fetchEpisodeCardsPage(1L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(row("TFC", 1, "999.00")),
            new PricingProviderClient.Paging(1, 1, 100)
        ));
        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 1)).thenReturn(java.util.Optional.of(mapped));

        ListAppender<ILoggingEvent> appender = attachLogAppender();
        pricingSyncService.runSync("manual", null);
        detachLogAppender(appender);

        assertThat(appender.list).anyMatch(e -> e.getFormattedMessage().contains("High market price detected"));
    }

    @Test
    @DisplayName("High market price log is suppressed when the setting is disabled")
    void runSync_highPriceLog_suppressedWhenDisabled() {
        Card mapped = card("mapped", null);
        mapped.setMarketPrice(null);

        mockEnabledSyncWithBudget(1, 0, new boolean[]{true}, new boolean[]{true});
        mockQueueCounts(1L, 0L);
        when(pricingSettingsService.isHighPriceLogEnabled()).thenReturn(false);

        when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(Map.of("id", 1L)),
            new PricingProviderClient.Paging(1, 1, 1)
        ));
        when(pricingProviderClient.fetchEpisodeCardsPage(1L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(row("TFC", 1, "999.00")),
            new PricingProviderClient.Paging(1, 1, 100)
        ));
        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 1)).thenReturn(java.util.Optional.of(mapped));

        ListAppender<ILoggingEvent> appender = attachLogAppender();
        pricingSyncService.runSync("manual", null);
        detachLogAppender(appender);

        assertThat(appender.list).noneMatch(e -> e.getFormattedMessage().contains("High market price detected"));
    }

    @Test
    @DisplayName("High market price log uses the configured threshold instead of the previous hardcoded 5 EUR")
    void runSync_highPriceLog_usesConfiguredThreshold() {
        Card mapped = card("mapped", null);
        mapped.setMarketPrice(null);

        mockEnabledSyncWithBudget(1, 0, new boolean[]{true}, new boolean[]{true});
        mockQueueCounts(1L, 0L);
        when(pricingSettingsService.isHighPriceLogEnabled()).thenReturn(true);
        when(pricingSettingsService.getHighPriceLogThreshold()).thenReturn(1);

        when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(Map.of("id", 1L)),
            new PricingProviderClient.Paging(1, 1, 1)
        ));
        // Price is below the previous hardcoded default (5) but above the configured threshold (1).
        when(pricingProviderClient.fetchEpisodeCardsPage(1L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(row("TFC", 1, "2.00")),
            new PricingProviderClient.Paging(1, 1, 100)
        ));
        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 1)).thenReturn(java.util.Optional.of(mapped));

        ListAppender<ILoggingEvent> appender = attachLogAppender();
        pricingSyncService.runSync("manual", null);
        detachLogAppender(appender);

        assertThat(appender.list).anyMatch(e -> e.getFormattedMessage().contains("High market price detected"));
    }

    @Test
    @DisplayName("High market price log comparison stays strict: a price equal to the threshold does not log")
    void runSync_highPriceLog_strictComparisonAtThreshold() {
        Card mapped = card("mapped", null);
        mapped.setMarketPrice(null);

        mockEnabledSyncWithBudget(1, 0, new boolean[]{true}, new boolean[]{true});
        mockQueueCounts(1L, 0L);
        when(pricingSettingsService.isHighPriceLogEnabled()).thenReturn(true);
        when(pricingSettingsService.getHighPriceLogThreshold()).thenReturn(5);

        when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(Map.of("id", 1L)),
            new PricingProviderClient.Paging(1, 1, 1)
        ));
        when(pricingProviderClient.fetchEpisodeCardsPage(1L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(row("TFC", 1, "5.00")),
            new PricingProviderClient.Paging(1, 1, 100)
        ));
        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 1)).thenReturn(java.util.Optional.of(mapped));

        ListAppender<ILoggingEvent> appender = attachLogAppender();
        pricingSyncService.runSync("manual", null);
        detachLogAppender(appender);

        assertThat(appender.list).noneMatch(e -> e.getFormattedMessage().contains("High market price detected"));
    }

    @Test
    @DisplayName("Abnormal price alert fires for a matching low-rarity row above the configured threshold")
    void runSync_abnormalPriceLog_firesForLowRarityAboveThreshold() {
        Card mapped = card("mapped", null);
        mapped.setMarketPrice(null);

        mockEnabledSyncWithBudget(1, 0, new boolean[]{true}, new boolean[]{true});
        mockQueueCounts(1L, 0L);
        when(pricingSettingsService.isAbnormalPriceLogEnabled()).thenReturn(true);
        when(pricingSettingsService.getAbnormalPriceLogThreshold()).thenReturn(5);
        when(pricingSettingsService.getAbnormalPriceLogRarities()).thenReturn(Set.of("common", "uncommon", "rare", "super_rare"));

        when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(Map.of("id", 1L)),
            new PricingProviderClient.Paging(1, 1, 1)
        ));
        Map<String, Object> lowRarityRow = row("TFC", 1, "9.00");
        lowRarityRow.put("rarity", "Common");
        when(pricingProviderClient.fetchEpisodeCardsPage(1L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(lowRarityRow),
            new PricingProviderClient.Paging(1, 1, 100)
        ));
        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 1)).thenReturn(java.util.Optional.of(mapped));

        ListAppender<ILoggingEvent> appender = attachLogAppender();
        pricingSyncService.runSync("manual", null);
        detachLogAppender(appender);

        assertThat(appender.list).anyMatch(e -> e.getFormattedMessage().contains("Abnormal price detected for low rarity card"));
    }

    @Test
    @DisplayName("Abnormal price alert is suppressed when disabled")
    void runSync_abnormalPriceLog_suppressedWhenDisabled() {
        Card mapped = card("mapped", null);
        mapped.setMarketPrice(null);

        mockEnabledSyncWithBudget(1, 0, new boolean[]{true}, new boolean[]{true});
        mockQueueCounts(1L, 0L);
        when(pricingSettingsService.isAbnormalPriceLogEnabled()).thenReturn(false);

        when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(Map.of("id", 1L)),
            new PricingProviderClient.Paging(1, 1, 1)
        ));
        Map<String, Object> lowRarityRow = row("TFC", 1, "9.00");
        lowRarityRow.put("rarity", "Common");
        when(pricingProviderClient.fetchEpisodeCardsPage(1L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(lowRarityRow),
            new PricingProviderClient.Paging(1, 1, 100)
        ));
        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 1)).thenReturn(java.util.Optional.of(mapped));

        ListAppender<ILoggingEvent> appender = attachLogAppender();
        pricingSyncService.runSync("manual", null);
        detachLogAppender(appender);

        assertThat(appender.list).noneMatch(e -> e.getFormattedMessage().contains("Abnormal price detected"));
    }

    @Test
    @DisplayName("Abnormal price alert does not fire for a rarity outside the configured list")
    void runSync_abnormalPriceLog_doesNotFireForRarityOutsideList() {
        Card mapped = card("mapped", null);
        mapped.setMarketPrice(null);

        mockEnabledSyncWithBudget(1, 0, new boolean[]{true}, new boolean[]{true});
        mockQueueCounts(1L, 0L);
        when(pricingSettingsService.isAbnormalPriceLogEnabled()).thenReturn(true);
        when(pricingSettingsService.getAbnormalPriceLogThreshold()).thenReturn(5);
        when(pricingSettingsService.getAbnormalPriceLogRarities()).thenReturn(Set.of("common", "uncommon", "rare", "super_rare"));

        when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(Map.of("id", 1L)),
            new PricingProviderClient.Paging(1, 1, 1)
        ));
        Map<String, Object> enchantedRow = row("TFC", 1, "999.00");
        enchantedRow.put("rarity", "Enchanted");
        when(pricingProviderClient.fetchEpisodeCardsPage(1L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(enchantedRow),
            new PricingProviderClient.Paging(1, 1, 100)
        ));
        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 1)).thenReturn(java.util.Optional.of(mapped));

        ListAppender<ILoggingEvent> appender = attachLogAppender();
        pricingSyncService.runSync("manual", null);
        detachLogAppender(appender);

        assertThat(appender.list).noneMatch(e -> e.getFormattedMessage().contains("Abnormal price detected"));
    }

    @Test
    @DisplayName("Abnormal price alert does not fire at or below the configured threshold")
    void runSync_abnormalPriceLog_doesNotFireAtThreshold() {
        Card mapped = card("mapped", null);
        mapped.setMarketPrice(null);

        mockEnabledSyncWithBudget(1, 0, new boolean[]{true}, new boolean[]{true});
        mockQueueCounts(1L, 0L);
        when(pricingSettingsService.isAbnormalPriceLogEnabled()).thenReturn(true);
        when(pricingSettingsService.getAbnormalPriceLogThreshold()).thenReturn(5);
        when(pricingSettingsService.getAbnormalPriceLogRarities()).thenReturn(Set.of("common", "uncommon", "rare", "super_rare"));

        when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(Map.of("id", 1L)),
            new PricingProviderClient.Paging(1, 1, 1)
        ));
        Map<String, Object> lowRarityRow = row("TFC", 1, "5.00");
        lowRarityRow.put("rarity", "Common");
        when(pricingProviderClient.fetchEpisodeCardsPage(1L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(lowRarityRow),
            new PricingProviderClient.Paging(1, 1, 100)
        ));
        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 1)).thenReturn(java.util.Optional.of(mapped));

        ListAppender<ILoggingEvent> appender = attachLogAppender();
        pricingSyncService.runSync("manual", null);
        detachLogAppender(appender);

        assertThat(appender.list).noneMatch(e -> e.getFormattedMessage().contains("Abnormal price detected"));
    }

    @Test
    @DisplayName("Abnormal price alert and high market price log fire independently when both conditions are met")
    void runSync_abnormalPriceLog_firesIndependentlyOfHighPriceLog() {
        Card mapped = card("mapped", null);
        mapped.setMarketPrice(null);

        mockEnabledSyncWithBudget(1, 0, new boolean[]{true}, new boolean[]{true});
        mockQueueCounts(1L, 0L);
        when(pricingSettingsService.isHighPriceLogEnabled()).thenReturn(true);
        when(pricingSettingsService.getHighPriceLogThreshold()).thenReturn(5);
        when(pricingSettingsService.isAbnormalPriceLogEnabled()).thenReturn(true);
        when(pricingSettingsService.getAbnormalPriceLogThreshold()).thenReturn(5);
        when(pricingSettingsService.getAbnormalPriceLogRarities()).thenReturn(Set.of("common", "uncommon", "rare", "super_rare"));

        when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(Map.of("id", 1L)),
            new PricingProviderClient.Paging(1, 1, 1)
        ));
        Map<String, Object> lowRarityRow = row("TFC", 1, "9.00");
        lowRarityRow.put("rarity", "Common");
        when(pricingProviderClient.fetchEpisodeCardsPage(1L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
            List.of(lowRarityRow),
            new PricingProviderClient.Paging(1, 1, 100)
        ));
        when(cardRepository.findByEditionCodeAndCardNumber("TFC", 1)).thenReturn(java.util.Optional.of(mapped));

        ListAppender<ILoggingEvent> appender = attachLogAppender();
        pricingSyncService.runSync("manual", null);
        detachLogAppender(appender);

        assertThat(appender.list).anyMatch(e -> e.getFormattedMessage().contains("High market price detected"));
        assertThat(appender.list).anyMatch(e -> e.getFormattedMessage().contains("Abnormal price detected for low rarity card"));
    }

    @Test
    @DisplayName("Unresolved mapping diagnostic is logged once per unresolved row when enabled, not capped at 3")
    void runSync_unresolvedMappingLog_emittedPerRowWhenEnabled() {
        when(pricingSettingsService.isUnresolvedMappingLogEnabled()).thenReturn(true);

        ListAppender<ILoggingEvent> appender = attachLogAppender();
        Map<String, Object> report = pricingSyncService.applyManualPricingImport(
                "[{}, {}, {}, {}, {}]");
        detachLogAppender(appender);

        assertThat(report.get("unresolvedCount")).isEqualTo(5);
        long unresolvedLogLines = appender.list.stream()
                .filter(e -> e.getFormattedMessage().contains("Unresolved mapping"))
                .count();
        assertThat(unresolvedLogLines).isEqualTo(5);
        @SuppressWarnings("unchecked")
        List<String> mappingSamples = (List<String>) report.get("mappingSamples");
        assertThat(mappingSamples).hasSize(3);
    }

    @Test
    @DisplayName("Unresolved mapping diagnostic is suppressed when disabled, but mappingSamples cap is unaffected")
    void runSync_unresolvedMappingLog_suppressedWhenDisabled() {
        when(pricingSettingsService.isUnresolvedMappingLogEnabled()).thenReturn(false);

        ListAppender<ILoggingEvent> appender = attachLogAppender();
        Map<String, Object> report = pricingSyncService.applyManualPricingImport(
                "[{}, {}, {}, {}, {}]");
        detachLogAppender(appender);

        assertThat(report.get("unresolvedCount")).isEqualTo(5);
        assertThat(appender.list).noneMatch(e -> e.getFormattedMessage().contains("Unresolved mapping"));
        @SuppressWarnings("unchecked")
        List<String> mappingSamples = (List<String>) report.get("mappingSamples");
        assertThat(mappingSamples).hasSize(3);
    }

    @Test
    @DisplayName("Promo rarity rows are skipped before mapping and leave no trace in the manual import report")
    void applyManualPricingImport_ignoresPromoRarityRows() {
        when(pricingSettingsService.isUnresolvedMappingLogEnabled()).thenReturn(true);

        ListAppender<ILoggingEvent> appender = attachLogAppender();
        Map<String, Object> report = pricingSyncService.applyManualPricingImport(
                "[{\"rarity\": \"Promo\"}, {\"rarity\": \"promo\"}, {}]");
        detachLogAppender(appender);

        // Only the trailing "{}" row (no rarity at all) counts as unresolved; both promo rows are ignored.
        assertThat(report.get("unresolvedCount")).isEqualTo(1);
        long unresolvedLogLines = appender.list.stream()
                .filter(e -> e.getFormattedMessage().contains("Unresolved mapping"))
                .count();
        assertThat(unresolvedLogLines).isEqualTo(1);
        @SuppressWarnings("unchecked")
        List<String> mappingSamples = (List<String>) report.get("mappingSamples");
        assertThat(mappingSamples).hasSize(1);
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    @DisplayName("runSync skips promo rarity rows without touching statusCounts, resolved/unresolved counters, or saving cards")
    void runSync_promoRarityRow_leavesNoTraceInReport() {
        mockEnabledSyncWithBudget(1, 0, new boolean[]{true}, new boolean[]{true});
        mockQueueCounts(1L, 0L);

        when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(Map.of("id", 1L)),
                new PricingProviderClient.Paging(1, 1, 1)
        ));
        Map<String, Object> promoRow = row("TFC", 1, "5.00");
        promoRow.put("rarity", "PROMO");
        when(pricingProviderClient.fetchEpisodeCardsPage(1L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(promoRow),
                new PricingProviderClient.Paging(1, 1, 100)
        ));

        ListAppender<ILoggingEvent> appender = attachLogAppender();
        Map<String, Object> report = pricingSyncService.runSync("manual", null);
        detachLogAppender(appender);

        assertThat(report.get("resolvedMappings")).isEqualTo(0);
        assertThat(report.get("unresolvedMappings")).isEqualTo(0);
        assertThat(report.get("unresolvedCount")).isEqualTo(0);
        assertThat(report.get("successCount")).isEqualTo(0);
        @SuppressWarnings("unchecked")
        Map<String, Integer> statusCounts = (Map<String, Integer>) report.get("statusCounts");
        assertThat(statusCounts).doesNotContainKey("UNRESOLVED_MAPPING");
        assertThat(appender.list).noneMatch(e -> e.getFormattedMessage().contains("Unresolved mapping"));
        verify(cardRepository, never()).save(any(Card.class));
        verify(cardRepository, never()).findByEditionCodeAndCardNumber(any(), any());
    }

    private static ListAppender<ILoggingEvent> attachLogAppender() {
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(PricingSyncService.class);
        // test profile lowers this logger to WARN; force INFO so the assertions below can observe it
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void detachLogAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) org.slf4j.LoggerFactory.getLogger(PricingSyncService.class);
        logger.detachAppender(appender);
        logger.setLevel(null);
        appender.stop();
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
                            "price", "1.25",
                            "prices", Map.of("cardmarket", Map.of("currency", "EUR", "lowest_near_mint", "1.25"))
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
            row.put("prices", Map.of("cardmarket", Map.of("currency", "EUR", "lowest_near_mint", "0.04")));

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
            row.put("prices", Map.of("cardmarket", Map.of("currency", "EUR", "lowest_near_mint", "1.23")));

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
            assertThat(localCard.getMarketPrice()).isEqualByComparingTo("0.06");
            }

            @Test
            @DisplayName("runSync prefers cardmarket 7d_average over other price fields")
            void runSync_prefersCardmarket7dAverage() {
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
            assertThat(localCard.getMarketPrice()).isEqualByComparingTo("888.88");
            }

            @Test
            @DisplayName("runSync prefers cardmarket 30d_average when 7d_average is absent")
            void runSync_prefersCardmarket30dAverageWhen7dAverageMissing() {
            Card localCard = card("ext-30d-average", null);

            mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
            mockQueueCounts(1L, 0L);

            when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(Map.of("id", 401L, "code", "11WSP")),
                new PricingProviderClient.Paging(1, 1, 1)
            ));

            Map<String, Object> providerRow = new LinkedHashMap<>();
            providerRow.put("card_number", 171);
            providerRow.put("prices", Map.of(
                "cardmarket", Map.of(
                    "currency", "EUR",
                    "30d_average", 4.44,
                    "lowest_near_mint_FR", 1.11,
                    "lowest_near_mint", 0.22
                )
            ));

            when(pricingProviderClient.fetchEpisodeCardsPage(401L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(providerRow),
                new PricingProviderClient.Paging(1, 1, 100)
            ));
            when(cardRepository.findByCardNumberAndEditionId(171, 11L)).thenReturn(java.util.Optional.of(localCard));

            Map<String, Object> report = pricingSyncService.runSync("manual", null);

            assertThat(report.get("successCount")).isEqualTo(1);
            assertThat(localCard.getMarketPrice()).isEqualByComparingTo("4.44");
            }

            @Test
            @DisplayName("runSync prefers cardmarket lowest_near_mint_FR when averages are absent")
            void runSync_prefersCardmarketLowestNearMintFrWhenAveragesMissing() {
            Card localCard = card("ext-near-mint-fr", null);

            mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
            mockQueueCounts(1L, 0L);

            when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(Map.of("id", 401L, "code", "11WSP")),
                new PricingProviderClient.Paging(1, 1, 1)
            ));

            Map<String, Object> providerRow = new LinkedHashMap<>();
            providerRow.put("card_number", 172);
            providerRow.put("prices", Map.of(
                "cardmarket", Map.of(
                    "currency", "EUR",
                    "lowest_near_mint_FR", 1.11,
                    "lowest_near_mint_FR_EU_only", 2.22,
                    "lowest_near_mint", 3.33
                )
            ));

            when(pricingProviderClient.fetchEpisodeCardsPage(401L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(providerRow),
                new PricingProviderClient.Paging(1, 1, 100)
            ));
            when(cardRepository.findByCardNumberAndEditionId(172, 11L)).thenReturn(java.util.Optional.of(localCard));

            Map<String, Object> report = pricingSyncService.runSync("manual", null);

            assertThat(report.get("successCount")).isEqualTo(1);
            assertThat(localCard.getMarketPrice()).isEqualByComparingTo("1.11");
            }

            @Test
            @DisplayName("runSync skips cardmarket candidates on currency mismatch and uses tcg_player.market_price")
            void runSync_skipsCardmarketOnCurrencyMismatchAndUsesTcgPlayer() {
            Card localCard = card("ext-currency-mismatch-fallthrough", null);

            mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
            mockQueueCounts(1L, 0L);

            when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(Map.of("id", 401L, "code", "11WSP")),
                new PricingProviderClient.Paging(1, 1, 1)
            ));

            Map<String, Object> providerRow = new LinkedHashMap<>();
            providerRow.put("card_number", 173);
            providerRow.put("prices", Map.of(
                "cardmarket", Map.of(
                    "currency", "USD",
                    "7d_average", 9.99
                ),
                "tcg_player", Map.of("currency", "EUR", "market_price", 6.66)
            ));

            when(pricingProviderClient.fetchEpisodeCardsPage(401L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(providerRow),
                new PricingProviderClient.Paging(1, 1, 100)
            ));
            when(cardRepository.findByCardNumberAndEditionId(173, 11L)).thenReturn(java.util.Optional.of(localCard));

            Map<String, Object> report = pricingSyncService.runSync("manual", null);

            assertThat(report.get("successCount")).isEqualTo(1);
            assertThat(localCard.getMarketPrice()).isEqualByComparingTo("6.66");
            }

            @Test
            @DisplayName("runSync falls back to cardmarket lowest_near_mint when FR_EU_only is absent")
            void runSync_fallsBackToCardmarketLowestNearMintWhenFrEuOnlyMissing() {
            Card localCard = card("ext-generic-near-mint", null);

            mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
            mockQueueCounts(1L, 0L);

            when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(Map.of("id", 401L, "code", "11WSP")),
                new PricingProviderClient.Paging(1, 1, 1)
            ));

            Map<String, Object> providerRow = new LinkedHashMap<>();
            providerRow.put("card_number", 167);
            providerRow.put("prices", Map.of(
                "cardmarket", Map.of(
                    "currency", "EUR",
                    "lowest_near_mint", 0.15
                )
            ));

            when(pricingProviderClient.fetchEpisodeCardsPage(401L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(providerRow),
                new PricingProviderClient.Paging(1, 1, 100)
            ));
            when(cardRepository.findByCardNumberAndEditionId(167, 11L)).thenReturn(java.util.Optional.of(localCard));

            Map<String, Object> report = pricingSyncService.runSync("manual", null);

            assertThat(report.get("successCount")).isEqualTo(1);
            assertThat(localCard.getMarketPrice()).isEqualByComparingTo("0.15");
            }

            @Test
            @DisplayName("runSync rejects cardmarket lowest_near_mint fallback when currency does not match provider currency")
            void runSync_rejectsLowestNearMintFallbackOnCurrencyMismatch() {
            Card localCard = card("ext-currency-mismatch", null);

            mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
            mockQueueCounts(1L, 0L);

            when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(Map.of("id", 401L, "code", "11WSP")),
                new PricingProviderClient.Paging(1, 1, 1)
            ));

            Map<String, Object> providerRow = new LinkedHashMap<>();
            providerRow.put("card_number", 168);
            providerRow.put("prices", Map.of(
                "cardmarket", Map.of(
                    "currency", "USD",
                    "lowest_near_mint", 0.20
                )
            ));

            when(pricingProviderClient.fetchEpisodeCardsPage(401L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(providerRow),
                new PricingProviderClient.Paging(1, 1, 100)
            ));
            when(cardRepository.findByCardNumberAndEditionId(168, 11L)).thenReturn(java.util.Optional.of(localCard));

            Map<String, Object> report = pricingSyncService.runSync("manual", null);

            assertThat(report.get("successCount")).isEqualTo(0);
            assertThat(report.get("unresolvedCount")).isEqualTo(1);
            assertThat(localCard.getMarketPrice()).isNull();
            verify(cardRepository, never()).save(localCard);
            }

            @Test
            @DisplayName("runSync treats zero as a legitimate FR_EU_only price rather than missing")
            void runSync_treatsZeroAsLegitimateFrEuOnlyPrice() {
            Card localCard = card("ext-zero-price", null);

            mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
            mockQueueCounts(1L, 0L);

            when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(Map.of("id", 401L, "code", "11WSP")),
                new PricingProviderClient.Paging(1, 1, 1)
            ));

            Map<String, Object> providerRow = new LinkedHashMap<>();
            providerRow.put("card_number", 169);
            providerRow.put("prices", Map.of(
                "cardmarket", Map.of(
                    "currency", "EUR",
                    "lowest_near_mint_FR_EU_only", 0,
                    "lowest_near_mint", 5.00
                )
            ));

            when(pricingProviderClient.fetchEpisodeCardsPage(401L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(providerRow),
                new PricingProviderClient.Paging(1, 1, 100)
            ));
            when(cardRepository.findByCardNumberAndEditionId(169, 11L)).thenReturn(java.util.Optional.of(localCard));

            Map<String, Object> report = pricingSyncService.runSync("manual", null);

            assertThat(report.get("successCount")).isEqualTo(1);
            assertThat(localCard.getMarketPrice()).isEqualByComparingTo("0.00");
            }

            @Test
            @DisplayName("runSync falls back to prices.tcg_player.market_price when cardmarket is unavailable")
            void runSync_fallsBackToTcgPlayerMarketPriceWhenCardmarketUnavailable() {
            Card localCard = card("ext-tcg-player-price", null);

            mockEnabledSyncWithBudget(2, 2, new boolean[]{true, true, false}, new boolean[]{true, true, false});
            mockQueueCounts(1L, 0L);

            when(pricingProviderClient.fetchEpisodesPage(1)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(Map.of("id", 401L, "code", "11WSP")),
                new PricingProviderClient.Paging(1, 1, 1)
            ));

            Map<String, Object> providerRow = new LinkedHashMap<>();
            providerRow.put("card_number", 170);
            providerRow.put("prices", Map.of(
                "tcg_player", Map.of("currency", "EUR", "market_price", 3.42)
            ));

            when(pricingProviderClient.fetchEpisodeCardsPage(401L, 1, 100)).thenReturn(PricingProviderClient.PagedResult.success(
                List.of(providerRow),
                new PricingProviderClient.Paging(1, 1, 100)
            ));
            when(cardRepository.findByCardNumberAndEditionId(170, 11L)).thenReturn(java.util.Optional.of(localCard));

            Map<String, Object> report = pricingSyncService.runSync("manual", null);

            assertThat(report.get("successCount")).isEqualTo(1);
            assertThat(localCard.getMarketPrice()).isEqualByComparingTo("3.42");
            }

        @Test
        @DisplayName("runSync parses tcg_player market_price with comma decimal")
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
            "tcg_player", Map.of("currency", "EUR", "market_price", "1,23 €")
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
        row.put("prices", Map.of(
            "cardmarket", Map.of("currency", "EUR", "lowest_near_mint", marketPrice)
        ));
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
