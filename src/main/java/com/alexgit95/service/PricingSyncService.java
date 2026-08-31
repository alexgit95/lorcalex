package com.alexgit95.service;

import com.alexgit95.model.Card;
import com.alexgit95.repository.CardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Comparator;
import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class PricingSyncService {

    private static final Logger log = LoggerFactory.getLogger(PricingSyncService.class);
    private static final List<String> CARDMARKET_CANDIDATE_KEYS = List.of(
            "7d_average", "30d_average", "lowest_near_mint_FR", "lowest_near_mint_FR_EU_only", "lowest_near_mint"
    );
    private static final List<String> CARDMARKET_AVERAGE_KEYS = List.of("7d_average", "30d_average");
    private static final List<String> CARDMARKET_MEDIAN_POOL_KEYS = List.of(
            "lowest_near_mint", "lowest_near_mint_EU_only",
            "lowest_near_mint_DE", "lowest_near_mint_DE_EU_only",
            "lowest_near_mint_FR", "lowest_near_mint_FR_EU_only",
            "lowest_near_mint_IT", "lowest_near_mint_IT_EU_only",
            "7d_average", "30d_average"
    );
    private static final int MEDIAN_POOL_MINIMUM_SIZE = 5;
    private static final int PLAUSIBILITY_FACTOR = 5;

    private final CardRepository cardRepository;
    private final PricingSettingsService pricingSettingsService;
    private final PricingProviderClient pricingProviderClient;
    private final CollectionValueTrendService collectionValueTrendService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public PricingSyncService(CardRepository cardRepository,
                              PricingSettingsService pricingSettingsService,
                              PricingProviderClient pricingProviderClient,
                              CollectionValueTrendService collectionValueTrendService) {
        this.cardRepository = cardRepository;
        this.pricingSettingsService = pricingSettingsService;
        this.pricingProviderClient = pricingProviderClient;
        this.collectionValueTrendService = collectionValueTrendService;
    }

    @Transactional
    public Map<String, Object> runSync(String trigger, Integer maxAttemptsOverride) {
        if (!running.compareAndSet(false, true)) {
            log.info("Pricing sync skipped: another run is already in progress (trigger={})", trigger);
            return Map.of(
                    "started", false,
                    "running", true,
                    "reasonCode", "ALREADY_RUNNING",
                    "message", "Une synchronisation pricing est déjà en cours."
            );
        }

        try {
            if (!pricingSettingsService.isSyncEnabled()) {
                log.info("Pricing sync skipped: feature disabled (trigger={})", trigger);
                return Map.of(
                        "started", false,
                        "running", false,
                        "trigger", trigger,
                        "reasonCode", "DISABLED",
                        "message", "La synchronisation pricing est désactivée par configuration."
                );
            }

            int maxAttempts = maxAttemptsOverride != null ? Math.max(0, maxAttemptsOverride) : Integer.MAX_VALUE;
            log.info("Pricing sync started (trigger={}, maxAttempts={})", trigger, maxAttempts == Integer.MAX_VALUE ? "unlimited" : maxAttempts);
            int attempted = 0;
            int processed = 0;
            int budgetBlocked = 0;
            int successCount = 0;
            int unresolvedCount = 0;
            int errorCount = 0;
            int resolvedMappings = 0;
            int unresolvedMappings = 0;
            int pagesProcessed = 0;
            int episodePagesProcessed = 0;
            int episodeCardsPagesProcessed = 0;
            int unresolvedDiagnosticLogs = 0;
            Map<String, Integer> statusCounts = new LinkedHashMap<>();
            String lastErrorDetail = null;
            boolean configMissingBlocked = false;
            PricingSettingsService.CursorState cursor = pricingSettingsService.getCursor();
            if (cursor == null) {
                cursor = PricingSettingsService.CursorState.initial();
            }
            Deque<Long> callWindowMillis = new ArrayDeque<>();
            int minuteLimit = pricingSettingsService.getMinuteLimit();
            String stopReason = "COMPLETED";
            boolean stop = false;

            int episodePage = cursor.episodePage();
            long resumeEpisodeId = cursor.phase() == PricingSettingsService.CursorPhase.EPISODE_CARDS
                    ? cursor.episodeId() : 0L;
            int resumeEpisodeCardsPage = cursor.phase() == PricingSettingsService.CursorPhase.EPISODE_CARDS
                    ? cursor.episodeCardsPage() : 1;

            while (!stop && attempted < maxAttempts) {
                if (isMinuteLimitReached(callWindowMillis, minuteLimit)) {
                    stopReason = "MINUTE_LIMIT_REACHED";
                    stop = true;
                    break;
                }
                if (!pricingSettingsService.hasRemainingAttempts()) {
                    budgetBlocked++;
                    stopReason = "BUDGET_EXHAUSTED";
                    stop = true;
                    break;
                }

                PricingProviderClient.PagedResult episodesPage = pricingProviderClient.fetchEpisodesPage(episodePage);
                if (!episodesPage.callAttempted()) {
                    if ("CONFIG_MISSING".equals(normalizeStatus(episodesPage.status()))) {
                        configMissingBlocked = true;
                        stopReason = "PROVIDER_CONFIG_MISSING";
                        break;
                    }
                    stopReason = "EPISODES_REQUEST_SKIPPED";
                    break;
                }
                if (!pricingSettingsService.tryConsumeAttempt()) {
                    budgetBlocked++;
                    stopReason = "BUDGET_EXHAUSTED";
                    break;
                }
                attempted++;
                rememberCall(callWindowMillis);
                pagesProcessed++;
                episodePagesProcessed++;

                if (!episodesPage.success()) {
                    errorCount++;
                    statusCounts.merge(normalizeStatus(episodesPage.status()), 1, Integer::sum);
                    lastErrorDetail = episodesPage.details();
                    stopReason = "EPISODES_REQUEST_ERROR";
                    break;
                }

                List<Map<String, Object>> episodes = episodesPage.data();
                if (episodes == null || episodes.isEmpty()) {
                    pricingSettingsService.resetCursor();
                    stopReason = "COMPLETED";
                    break;
                }

                boolean waitingResumeEpisode = resumeEpisodeId > 0;
                for (Map<String, Object> episodePayload : episodes) {
                    Long episodeId = extractEpisodeId(episodePayload);
                    if (episodeId == null || episodeId <= 0) {
                        continue;
                    }
                    Integer episodeSetNumber = extractEpisodeSetNumber(episodePayload);
                    if (waitingResumeEpisode && resumeEpisodeId != episodeId) {
                        continue;
                    }
                    waitingResumeEpisode = false;

                    int episodeCardsPage = resumeEpisodeId > 0 && resumeEpisodeId == episodeId
                            ? Math.max(1, resumeEpisodeCardsPage) : 1;

                    while (!stop && attempted < maxAttempts) {
                        if (isMinuteLimitReached(callWindowMillis, minuteLimit)) {
                            stopReason = "MINUTE_LIMIT_REACHED";
                            stop = true;
                            break;
                        }
                        if (!pricingSettingsService.hasRemainingAttempts()) {
                            budgetBlocked++;
                            stopReason = "BUDGET_EXHAUSTED";
                            stop = true;
                            break;
                        }

                        PricingProviderClient.PagedResult cardsPage = pricingProviderClient.fetchEpisodeCardsPage(episodeId, episodeCardsPage, 100);
                        if (!cardsPage.callAttempted()) {
                            if ("CONFIG_MISSING".equals(normalizeStatus(cardsPage.status()))) {
                                configMissingBlocked = true;
                                stopReason = "PROVIDER_CONFIG_MISSING";
                                stop = true;
                                break;
                            }
                            stopReason = "EPISODE_CARDS_REQUEST_SKIPPED";
                            stop = true;
                            break;
                        }
                        if (!pricingSettingsService.tryConsumeAttempt()) {
                            budgetBlocked++;
                            stopReason = "BUDGET_EXHAUSTED";
                            stop = true;
                            break;
                        }

                        attempted++;
                        rememberCall(callWindowMillis);
                        pagesProcessed++;
                        episodeCardsPagesProcessed++;

                        pricingSettingsService.persistCursor(new PricingSettingsService.CursorState(
                                PricingSettingsService.CursorPhase.EPISODE_CARDS,
                                episodePage,
                                episodeId,
                                episodeCardsPage
                        ));

                        if (!cardsPage.success()) {
                            errorCount++;
                            statusCounts.merge(normalizeStatus(cardsPage.status()), 1, Integer::sum);
                            lastErrorDetail = cardsPage.details();
                            stopReason = "EPISODE_CARDS_REQUEST_ERROR";
                            stop = true;
                            break;
                        }

                        MappingBatchResult mappingResult = applyPricingFromProviderRows(cardsPage.data(), episodeSetNumber, statusCounts);
                        processed += mappingResult.updatedCards;
                        successCount += mappingResult.updatedCards;
                        unresolvedMappings += mappingResult.unresolvedRows;
                        resolvedMappings += mappingResult.updatedCards;
                        unresolvedCount += mappingResult.unresolvedRows;
                        if (mappingResult.unresolvedRows > 0 && unresolvedDiagnosticLogs < 5) {
                            unresolvedDiagnosticLogs++;
                            log.warn(
                                    "Pricing mapping diagnostics (trigger={}, episodeId={}, page={}, unresolvedRows={}, sampleMappings={}, samplePrices={})",
                                    trigger,
                                    episodeId,
                                    episodeCardsPage,
                                    mappingResult.unresolvedRows,
                                    mappingResult.mappingSamples,
                                    mappingResult.priceSamples
                            );
                        }

                        int currentCardsPage = cardsPage.paging() != null ? cardsPage.paging().current() : episodeCardsPage;
                        int totalCardsPages = cardsPage.paging() != null ? cardsPage.paging().total() : currentCardsPage;
                        if (currentCardsPage >= totalCardsPages) {
                            break;
                        }

                        episodeCardsPage = currentCardsPage + 1;
                        pricingSettingsService.persistCursor(new PricingSettingsService.CursorState(
                                PricingSettingsService.CursorPhase.EPISODE_CARDS,
                                episodePage,
                                episodeId,
                                episodeCardsPage
                        ));
                    }

                    if (stop || attempted >= maxAttempts) {
                        resumeEpisodeId = episodeId;
                        resumeEpisodeCardsPage = episodeCardsPage;
                        break;
                    }

                    resumeEpisodeId = 0;
                    resumeEpisodeCardsPage = 1;
                    pricingSettingsService.persistCursor(new PricingSettingsService.CursorState(
                            PricingSettingsService.CursorPhase.EPISODES,
                            episodePage,
                            0,
                            1
                    ));
                }

                if (stop || attempted >= maxAttempts) {
                    break;
                }

                int currentEpisodePage = episodesPage.paging() != null ? episodesPage.paging().current() : episodePage;
                int totalEpisodePages = episodesPage.paging() != null ? episodesPage.paging().total() : currentEpisodePage;
                if (currentEpisodePage >= totalEpisodePages) {
                    pricingSettingsService.resetCursor();
                    stopReason = "COMPLETED";
                    break;
                }

                episodePage = currentEpisodePage + 1;
                pricingSettingsService.persistCursor(new PricingSettingsService.CursorState(
                        PricingSettingsService.CursorPhase.EPISODES,
                        episodePage,
                        0,
                        1
                ));
            }

            if (attempted >= maxAttempts && maxAttempts != Integer.MAX_VALUE) {
                stopReason = "MAX_ATTEMPTS_REACHED";
            }

            pricingSettingsService.setLastStopReason(stopReason);

            Map<String, Object> budget = pricingSettingsService.getBudgetStatus();
            Map<String, Object> report = new LinkedHashMap<>();
            report.put("started", true);
            report.put("running", false);
            report.put("trigger", trigger);
            report.put("attempted", attempted);
            report.put("processed", processed);
            report.put("pagesProcessed", pagesProcessed);
            report.put("episodePagesProcessed", episodePagesProcessed);
            report.put("episodeCardsPagesProcessed", episodeCardsPagesProcessed);
            report.put("resolvedMappings", resolvedMappings);
            report.put("unresolvedMappings", unresolvedMappings);
            report.put("budgetBlocked", budgetBlocked > 0);
            report.put("successCount", successCount);
            report.put("unresolvedCount", unresolvedCount);
            report.put("errorCount", errorCount);
            report.put("statusCounts", statusCounts);
            report.put("dailyBudget", budget.get("dailyBudget"));
            report.put("dailyHardLimit", budget.get("dailyHardLimit"));
            report.put("dailySafetyMargin", budget.get("dailySafetyMargin"));
            report.put("effectiveDailyBudget", budget.get("effectiveDailyBudget"));
            report.put("minuteLimit", budget.get("minuteLimit"));
            report.put("usedAttempts", budget.get("usedAttempts"));
            report.put("remainingAttempts", budget.get("remainingAttempts"));
            long queueWithoutPrice = cardRepository.countByMarketPriceIsNull();
            long queueStaleOver7Days = cardRepository.findByMarketPriceIsNotNullAndLastPriceAtBeforeOrderByLastPriceAtAscIdAsc(LocalDateTime.now().minusDays(7)).size();
            long queueWithPrice = cardRepository.countByLastPriceAtIsNotNull();
            report.put("queueWithoutPrice", queueWithoutPrice);
            report.put("queueStaleOver7Days", queueStaleOver7Days);
            report.put("queueWithPrice", queueWithPrice);
            PricingSettingsService.CursorState reportCursor = pricingSettingsService.getCursor();
            if (reportCursor == null) {
                reportCursor = PricingSettingsService.CursorState.initial();
            }
            report.put("cursor", reportCursor.toMap());
            report.put("stopReason", stopReason);
            report.put("lastErrorDetail", lastErrorDetail);

            String reasonCode = "COMPLETED";
            String message = "Synchronisation pricing terminée.";
            if (configMissingBlocked) {
                reasonCode = "PROVIDER_CONFIG_MISSING";
                message = "Synchronisation interrompue: configuration provider incomplète (aucun appel effectué).";
            } else if (maxAttempts == 0) {
                reasonCode = "MAX_ATTEMPTS_ZERO";
                message = "Aucune tentative lancée: limite manuelle réglée à 0.";
            } else if ("MINUTE_LIMIT_REACHED".equals(stopReason)) {
                reasonCode = "MINUTE_LIMIT_REACHED";
                message = "Synchronisation interrompue: limite stricte de 30 appels/minute atteinte.";
            } else if (attempted == 0 && budgetBlocked > 0) {
                reasonCode = "BUDGET_EXHAUSTED";
                message = "Aucune tentative lancée: quota journalier épuisé.";
            } else if (attempted == 0 && queueWithoutPrice == 0 && queueWithPrice == 0) {
                reasonCode = "EMPTY_QUEUE";
                message = "Aucune carte à synchroniser.";
            } else if (attempted == 0) {
                reasonCode = "NO_ATTEMPT";
                message = "Synchronisation terminée sans tentative.";
            } else if (budgetBlocked > 0) {
                reasonCode = "BUDGET_EXHAUSTED_AFTER_ATTEMPTS";
                message = "Synchronisation interrompue: quota journalier épuisé en cours de run.";
            } else if ("MAX_ATTEMPTS_REACHED".equals(stopReason)) {
                reasonCode = "MAX_ATTEMPTS_REACHED";
                message = "Synchronisation partielle: limite manuelle de pages atteinte.";
            }

            String dominantIssue = dominantNonSuccessStatus(statusCounts);
            if (dominantIssue != null && unresolvedCount > 0) {
                message = message + " Cause dominante non résolue: " + dominantIssue + ".";
            }
            report.put("reasonCode", reasonCode);
            report.put("message", message);

            log.info(
                        "Pricing sync completed (trigger={}, attempted={}, success={}, unresolved={}, errors={}, budgetBlocked={}, remainingAttempts={}, reason={}, stopReason={}, statusCounts={}, lastErrorDetail={})",
                    trigger,
                    attempted,
                    successCount,
                    unresolvedCount,
                    errorCount,
                    budgetBlocked > 0,
                    budget.get("remainingAttempts"),
                        reasonCode,
                        stopReason,
                        statusCounts,
                        lastErrorDetail
            );
            if ("COMPLETED".equals(reasonCode) || "MAX_ATTEMPTS_REACHED".equals(reasonCode)) {
                collectionValueTrendService.persistSnapshotFromCurrentCollection();
            }
            return report;
        } catch (Exception ex) {
            log.error("Pricing sync run failed", ex);
            return Map.of(
                    "started", false,
                    "running", false,
                    "trigger", trigger,
                    "reasonCode", "ERROR",
                    "message", "Echec de la synchronisation pricing: " + ex.getMessage()
            );
        } finally {
            running.set(false);
        }
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>(pricingSettingsService.getBudgetStatus());
        status.put("running", running.get());
        status.put("queueWithoutPrice", cardRepository.countByMarketPriceIsNull());
        status.put("queueStaleOver7Days", cardRepository.findByMarketPriceIsNotNullAndLastPriceAtBeforeOrderByLastPriceAtAscIdAsc(LocalDateTime.now().minusDays(7)).size());
        status.put("queueWithPrice", cardRepository.countByLastPriceAtIsNotNull());
        status.put("cursor", pricingSettingsService.getCursor().toMap());
        return status;
    }

    /**
     * Temporary admin tool: apply pricing updates from a manually pasted provider
     * JSON payload (same shape as an episode-cards API response), without calling
     * the provider or consuming any daily/minute call budget.
     */
    @Transactional
    public Map<String, Object> applyManualPricingImport(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return Map.of("success", false, "message", "Le JSON fourni est vide.");
        }

        Object parsed;
        try {
            parsed = new tools.jackson.databind.ObjectMapper().readValue(rawJson, Object.class);
        } catch (Exception ex) {
            return Map.of("success", false, "message", "JSON invalide: " + ex.getMessage());
        }

        List<Map<String, Object>> rows = extractManualImportRows(parsed);
        if (rows.isEmpty()) {
            return Map.of("success", false, "message", "Aucune carte trouvée dans le JSON fourni.");
        }

        Map<String, Integer> statusCounts = new LinkedHashMap<>();
        MappingBatchResult result = applyPricingFromProviderRows(rows, null, statusCounts);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("success", true);
        report.put("rowsReceived", rows.size());
        report.put("updatedCount", result.updatedCards());
        report.put("unresolvedCount", result.unresolvedRows());
        report.put("mappingSamples", result.mappingSamples());
        report.put("priceSamples", result.priceSamples());
        report.put("message", result.updatedCards() + " carte(s) mise(s) à jour, "
                + result.unresolvedRows() + " non résolue(s) sur " + rows.size() + " ligne(s).");
        return report;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> extractManualImportRows(Object parsed) {
        List<?> rawList;
        if (parsed instanceof Map<?, ?> root && root.get("data") instanceof List<?> dataList) {
            rawList = dataList;
        } else if (parsed instanceof List<?> list) {
            rawList = list;
        } else if (parsed instanceof Map<?, ?> singleCard) {
            rawList = List.of(singleCard);
        } else {
            rawList = List.of();
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object item : rawList) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, Object> converted = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                converted.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            rows.add(converted);
        }
        return rows;
    }

    private MappingBatchResult applyPricingFromProviderRows(List<Map<String, Object>> rows,
                                                            Integer episodeSetNumber,
                                                            Map<String, Integer> statusCounts) {
        if (rows == null || rows.isEmpty()) {
            return new MappingBatchResult(0, 0, List.of(), List.of());
        }

        List<MappedRow> mappedRows = new ArrayList<>();
        int unresolved = 0;
        List<String> mappingSamples = new ArrayList<>();
        List<String> priceSamples = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            if (isPromoRarityRow(row)) {
                // Promo cards are never imported into the local catalog; skip before mapping and telemetry.
                continue;
            }
            Optional<Card> maybeCard = resolveCard(row, episodeSetNumber);
            if (maybeCard.isEmpty()) {
                unresolved++;
                statusCounts.merge("UNRESOLVED_MAPPING", 1, Integer::sum);
                if (mappingSamples.size() < 3) {
                    mappingSamples.add(buildRowDiagnostic(row));
                }
                if (pricingSettingsService.isUnresolvedMappingLogEnabled()) {
                    log.info("Unresolved mapping ({}, providerRow={})", buildRowDiagnostic(row), row);
                }
                continue;
            }
            BigDecimalPrice price = extractPriceFromRow(row);
            if (price.value == null) {
                unresolved++;
                statusCounts.merge("UNRESOLVED_PRICE", 1, Integer::sum);
                if (priceSamples.size() < 3) {
                    priceSamples.add(buildRowDiagnostic(row));
                }
                continue;
            }
            mappedRows.add(new MappedRow(maybeCard.get(), price.value, row));
        }

        int updated = 0;
        mappedRows.sort(Comparator.comparingInt(o -> priorityTier(o.card)));
        LocalDateTime now = LocalDateTime.now();
        String source = pricingSettingsService.getProviderName();
        String currency = pricingSettingsService.getProviderCurrency();
        for (MappedRow row : mappedRows) {
            Card card = row.card;
            card.setMarketPrice(row.price);
            card.setPriceCurrency(currency);
            card.setPriceSource(source);
            card.setLastPriceAt(now);
            card.setLastPriceStatus("SUCCESS");
            cardRepository.save(card);
            statusCounts.merge("SUCCESS", 1, Integer::sum);
            updated++;
            java.math.BigDecimal highPriceThreshold = java.math.BigDecimal.valueOf(pricingSettingsService.getHighPriceLogThreshold());
            if (pricingSettingsService.isHighPriceLogEnabled() && row.price.compareTo(highPriceThreshold) > 0) {
                log.info("High market price detected (cardId={}, externalId={}, price={}, providerRow={})",
                        card.getId(), card.getExternalId(), row.price, row.rawRow);
            }
            if (pricingSettingsService.isAbnormalPriceLogEnabled()) {
                String rowRarity = normalizeText(row.rawRow.get("rarity"));
                java.math.BigDecimal abnormalPriceThreshold = java.math.BigDecimal.valueOf(pricingSettingsService.getAbnormalPriceLogThreshold());
                if (rowRarity != null
                        && pricingSettingsService.getAbnormalPriceLogRarities().contains(rowRarity.toLowerCase(java.util.Locale.ROOT))
                        && row.price.compareTo(abnormalPriceThreshold) > 0) {
                    log.info("Abnormal price detected for low rarity card (cardId={}, externalId={}, rarity={}, computedPrice={}, providerRow={})",
                            card.getId(), card.getExternalId(), rowRarity, row.price, row.rawRow);
                }
            }
        }
        return new MappingBatchResult(updated, unresolved, mappingSamples, priceSamples);
    }

    private static String buildRowDiagnostic(Map<String, Object> row) {
        String setCode = normalizeText(firstPresent(row, "set_code", "setCode", "editionCode", "edition_code", "code"));
        Integer setNumber = parseInteger(firstPresent(row, "set_num", "setNumber", "set_number", "set_id"));
        Integer episodeCodeSetNumber = extractEpisodeCodeSetNumber(row);
        Integer cardNumber = parseInteger(firstPresent(row, "number", "card_number", "cardNumber", "collector_number", "card_num"));
        String externalId = normalizeText(firstPresent(row, "externalId", "external_id", "card_id", "cardId", "lorcana_id", "id"));
        String name = normalizeText(firstPresent(row, "name", "fullName", "card_name"));
        Object price = firstPresent(row, "marketPrice", "market_price", "price", "value", "amount", "eur", "usd");
        return "setCode=" + setCode
                + ", setNumber=" + setNumber
                + ", episodeCodeSetNumber=" + episodeCodeSetNumber
                + ", cardNumber=" + cardNumber
                + ", externalId=" + externalId
                + ", name=" + name
                + ", price=" + (price == null ? "null" : String.valueOf(price));
    }

    private Optional<Card> resolveCard(Map<String, Object> payload, Integer episodeSetNumber) {
        String editionCode = normalizeText(firstPresent(payload,
                "set_code", "setCode", "editionCode", "edition_code", "code"));
        Integer setNumber = parseInteger(firstPresent(payload,
                "set_num", "setNumber", "set_number", "set_id"));
        Integer episodeCodeSetNumber = extractEpisodeCodeSetNumber(payload);
        if (editionCode == null) {
            Object setNode = payload.get("set");
            if (setNode instanceof Map<?, ?> setMap) {
                editionCode = normalizeText(firstPresent(setMap, "code", "set_code", "setCode", "abbreviation", "slug"));
                if (setNumber == null) {
                    setNumber = parseInteger(firstPresent(setMap, "set_num", "setNumber", "set_number", "number", "id"));
                }
            } else if (setNode instanceof String s && !s.isBlank()) {
                editionCode = s.trim();
            }
        }

        if (setNumber == null && episodeCodeSetNumber != null && episodeCodeSetNumber > 0) {
            setNumber = episodeCodeSetNumber;
        }

        if (setNumber == null && episodeSetNumber != null && episodeSetNumber > 0) {
            setNumber = episodeSetNumber;
        }

        Integer cardNumber = parseInteger(firstPresent(payload,
                "number", "card_number", "cardNumber", "collector_number", "card_num"));

        if (setNumber != null && cardNumber != null) {
            Optional<Card> byEditionIdAndCard = cardRepository.findByCardNumberAndEditionId(cardNumber, Long.valueOf(setNumber));
            if (byEditionIdAndCard.isPresent()) {
                return byEditionIdAndCard;
            }

            Optional<Card> bySetNumberAndCard = cardRepository.findByEditionSetNumberAndCardNumber(setNumber, cardNumber);
            if (bySetNumberAndCard.isPresent()) {
                return bySetNumberAndCard;
            }
        }

        if (editionCode != null && cardNumber != null) {
            Optional<Card> byCodeNumber = cardRepository.findByEditionCodeAndCardNumber(editionCode, cardNumber);
            if (byCodeNumber.isPresent()) {
                return byCodeNumber;
            }
        }

        String externalId = normalizeText(firstPresent(payload,
                "externalId", "external_id", "card_id", "cardId", "lorcana_id", "id"));
        if (externalId != null) {
            Optional<Card> byExternalId = cardRepository.findByExternalId(externalId);
            if (byExternalId.isPresent()) {
                return byExternalId;
            }
        }

        return Optional.empty();
    }

    private BigDecimalPrice extractPriceFromRow(Map<String, Object> payload) {
        java.math.BigDecimal parsed = extractCardmarketPreferredPrice(payload);
        if (parsed != null) {
            parsed = parsed.setScale(2, java.math.RoundingMode.HALF_UP);
        }
        return new BigDecimalPrice(parsed);
    }

    /**
     * Ordered price source priority: cardmarket 7d/30d averages, then FR, FR EU-only, and generic
     * near-mint, then tcg_player market price as last resort. Each container's currency is checked
     * once (all cardmarket-sourced candidates share cardmarket.currency); no further fallback is
     * attempted if none of the ordered candidates are usable. The two average-based candidates are
     * additionally rejected as implausible if they fall outside 1/5x-5x of the row's reference median
     * (only when at least {@link #MEDIAN_POOL_MINIMUM_SIZE} pooled price fields are available).
     */
    private java.math.BigDecimal extractCardmarketPreferredPrice(Map<String, Object> payload) {
        Object pricesNode = payload.get("prices");
        if (!(pricesNode instanceof Map<?, ?> pricesMap)) {
            return null;
        }

        Object cardmarketNode = pricesMap.get("cardmarket");
        if (cardmarketNode instanceof Map<?, ?> cardmarketMap && isAcceptableCurrency(cardmarketMap)) {
            Optional<java.math.BigDecimal> referenceMedian = computeCardmarketReferenceMedian(cardmarketMap);
            for (String key : CARDMARKET_CANDIDATE_KEYS) {
                java.math.BigDecimal value = extractPriceNode(cardmarketMap.get(key));
                if (value == null) {
                    continue;
                }
                if (CARDMARKET_AVERAGE_KEYS.contains(key) && !isPlausibleAverage(value, referenceMedian)) {
                    continue;
                }
                return value;
            }
        }

        Object tcgPlayerNode = pricesMap.get("tcg_player");
        if (tcgPlayerNode instanceof Map<?, ?> tcgPlayerMap && isAcceptableCurrency(tcgPlayerMap)) {
            return extractPriceNode(tcgPlayerMap.get("market_price"));
        }

        return null;
    }

    private Optional<java.math.BigDecimal> computeCardmarketReferenceMedian(Map<?, ?> cardmarketMap) {
        List<java.math.BigDecimal> pool = new ArrayList<>();
        for (String key : CARDMARKET_MEDIAN_POOL_KEYS) {
            java.math.BigDecimal value = extractPriceNode(cardmarketMap.get(key));
            if (value != null && value.compareTo(java.math.BigDecimal.ZERO) != 0) {
                pool.add(value);
            }
        }
        if (pool.size() < MEDIAN_POOL_MINIMUM_SIZE) {
            return Optional.empty();
        }
        pool.sort(Comparator.naturalOrder());
        int size = pool.size();
        java.math.BigDecimal median;
        if (size % 2 == 1) {
            median = pool.get(size / 2);
        } else {
            java.math.BigDecimal lower = pool.get(size / 2 - 1);
            java.math.BigDecimal upper = pool.get(size / 2);
            median = lower.add(upper).divide(java.math.BigDecimal.valueOf(2), 10, java.math.RoundingMode.HALF_UP);
        }
        return Optional.of(median);
    }

    private static boolean isPlausibleAverage(java.math.BigDecimal value, Optional<java.math.BigDecimal> medianOpt) {
        if (medianOpt.isEmpty()) {
            return true;
        }
        java.math.BigDecimal median = medianOpt.get();
        java.math.BigDecimal lowerBound = median.divide(java.math.BigDecimal.valueOf(PLAUSIBILITY_FACTOR), 10, java.math.RoundingMode.HALF_UP);
        java.math.BigDecimal upperBound = median.multiply(java.math.BigDecimal.valueOf(PLAUSIBILITY_FACTOR));
        return value.compareTo(lowerBound) >= 0 && value.compareTo(upperBound) <= 0;
    }

    private boolean isAcceptableCurrency(Map<?, ?> priceContainerMap) {
        Object currencyNode = priceContainerMap.get("currency");
        if (currencyNode == null) {
            return true;
        }
        String expected = pricingSettingsService.getProviderCurrency();
        return expected == null || expected.isBlank() || String.valueOf(currencyNode).trim().equalsIgnoreCase(expected);
    }

    private java.math.BigDecimal extractPriceNode(Object node) {
        if (node instanceof Number n) {
            return java.math.BigDecimal.valueOf(n.doubleValue());
        }
        if (node instanceof String s) {
            return parseDecimalString(s);
        }
        return null;
    }

    private static java.math.BigDecimal parseDecimalString(String raw) {
        if (raw == null) {
            return null;
        }

        String cleaned = raw.trim();
        if (cleaned.isEmpty()) {
            return null;
        }

        cleaned = cleaned.replaceAll("[^0-9,.-]", "");
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals(".") || cleaned.equals(",")) {
            return null;
        }

        boolean hasComma = cleaned.contains(",");
        boolean hasDot = cleaned.contains(".");
        if (hasComma && hasDot) {
            cleaned = cleaned.replace(",", "");
        } else if (hasComma) {
            cleaned = cleaned.replace(',', '.');
        }

        try {
            return new java.math.BigDecimal(cleaned);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isPromoRarityRow(Map<String, Object> row) {
        String rarity = normalizeText(row.get("rarity"));
        return rarity != null && "Promo".equalsIgnoreCase(rarity);
    }

    private static Object firstPresent(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key) && map.get(key) != null) {
                return map.get(key);
            }
        }
        return null;
    }

    private static Integer parseInteger(Object raw) {
        if (raw instanceof Number n) {
            return n.intValue();
        }
        if (raw instanceof String s) {
            String normalized = s.trim().replaceAll("[^0-9]", "");
            if (normalized.isEmpty()) {
                return null;
            }
            try {
                return Integer.parseInt(normalized);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static String normalizeText(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? null : value;
    }

    private static Long extractEpisodeId(Map<String, Object> episodePayload) {
        Object id = firstPresent(episodePayload, "id", "episodeId", "episode_id");
        if (id instanceof Number n) {
            return n.longValue();
        }
        if (id instanceof String s) {
            try {
                return Long.parseLong(s.trim());
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }

    private static Integer extractEpisodeSetNumber(Map<String, Object> episodePayload) {
        Integer setNumber = parseInteger(firstPresent(episodePayload,
                "set_num", "setNumber", "set_number", "number", "set_id"));
        if (setNumber != null) {
            return setNumber;
        }

        Integer fromCode = parseLeadingInteger(firstPresent(episodePayload, "code", "episode_code", "episodeCode"));
        if (fromCode != null) {
            return fromCode;
        }

        Object setNode = episodePayload.get("set");
        if (setNode instanceof Map<?, ?> setMap) {
            return parseInteger(firstPresent(setMap, "set_num", "setNumber", "set_number", "number", "id"));
        }

        return null;
    }

    private static Integer extractEpisodeCodeSetNumber(Map<String, Object> payload) {
        Object episodeNode = payload.get("episode");
        if (episodeNode instanceof Map<?, ?> episodeMap) {
            Integer fromEpisodeCode = parseLeadingInteger(firstPresent(episodeMap, "code", "episode_code", "episodeCode"));
            if (fromEpisodeCode != null) {
                return fromEpisodeCode;
            }
        }

        return parseLeadingInteger(firstPresent(payload, "episode_code", "episodeCode"));
    }

    private static Integer parseLeadingInteger(Object raw) {
        if (raw instanceof Number n) {
            return n.intValue();
        }
        if (!(raw instanceof String s)) {
            return null;
        }

        String normalized = s.trim();
        if (normalized.isEmpty() || !Character.isDigit(normalized.charAt(0))) {
            return null;
        }

        int idx = 0;
        while (idx < normalized.length() && Character.isDigit(normalized.charAt(idx))) {
            idx++;
        }

        try {
            return Integer.parseInt(normalized.substring(0, idx));
        } catch (Exception ignored) {
            return null;
        }
    }

    private static boolean isMinuteLimitReached(Deque<Long> callWindowMillis, int minuteLimit) {
        long now = System.currentTimeMillis();
        while (!callWindowMillis.isEmpty() && now - callWindowMillis.peekFirst() >= 60_000) {
            callWindowMillis.removeFirst();
        }
        return callWindowMillis.size() >= minuteLimit;
    }

    private static void rememberCall(Deque<Long> callWindowMillis) {
        callWindowMillis.addLast(System.currentTimeMillis());
    }

    private static int priorityTier(Card card) {
        if (card.getMarketPrice() == null) {
            return 0;
        }
        LocalDateTime lastPriceAt = card.getLastPriceAt();
        if (lastPriceAt == null || lastPriceAt.isBefore(LocalDateTime.now().minusDays(7))) {
            return 1;
        }
        return 2;
    }

    private record MappingBatchResult(int updatedCards,
                                      int unresolvedRows,
                                      List<String> mappingSamples,
                                      List<String> priceSamples) {
    }

    private record MappedRow(Card card, java.math.BigDecimal price, Map<String, Object> rawRow) {
    }

    private record BigDecimalPrice(java.math.BigDecimal value) {
    }

    private static String normalizeStatus(String status) {
        return status == null || status.isBlank() ? "UNKNOWN" : status.trim();
    }

    private static String dominantNonSuccessStatus(Map<String, Integer> statusCounts) {
        return statusCounts.entrySet().stream()
                .filter(e -> !"SUCCESS".equals(e.getKey()))
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(null);
    }
}
