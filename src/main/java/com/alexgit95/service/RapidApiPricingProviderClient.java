package com.alexgit95.service;

import com.alexgit95.model.Card;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class RapidApiPricingProviderClient implements PricingProviderClient {

    private static final Logger log = LoggerFactory.getLogger(RapidApiPricingProviderClient.class);

    private final WebClient.Builder webClientBuilder;
    private final PricingSettingsService settingsService;

    public RapidApiPricingProviderClient(WebClient.Builder webClientBuilder,
                                         PricingSettingsService settingsService) {
        this.webClientBuilder = webClientBuilder;
        this.settingsService = settingsService;
    }

    @Override
    public PricingFetchResult fetchPrice(Card card) {
        String apiKey = settingsService.getProviderApiKey();
        String host = settingsService.getProviderHost();
        String path = settingsService.getProviderPath();
        String currency = settingsService.getProviderCurrency();
        String source = settingsService.getProviderName();

        if (apiKey.isBlank() || host.isBlank() || path.isBlank()) {
            return PricingFetchResult.configMissing("Provider settings are incomplete");
        }

        try {
            String deterministicQuery = buildDeterministicQuery(card);
            Object payload = webClientBuilder
                    .baseUrl("https://" + host)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(path)
                    .queryParam("q", deterministicQuery)
                            .queryParam("name", card.getName())
                            .queryParamIfPresent("number", java.util.Optional.ofNullable(card.getCardNumber()))
                    .queryParamIfPresent("edition", java.util.Optional.ofNullable(card.getEdition()).map(e -> e.getCode()))
                    .queryParamIfPresent("externalId", java.util.Optional.ofNullable(card.getExternalId()))
                            .build())
                    .header("X-RapidAPI-Key", apiKey)
                    .header("X-RapidAPI-Host", host)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block(Duration.ofSeconds(20));

            BigDecimal value = extractPrice(payload);
            if (value == null) {
                return PricingFetchResult.unresolved("UNRESOLVED", "Price value not found in provider response");
            }

            return PricingFetchResult.success(value.setScale(2, RoundingMode.HALF_UP), currency, source);
        } catch (WebClientResponseException ex) {
            String status = "HTTP_" + ex.getStatusCode().value();
            return PricingFetchResult.error(status, truncate(ex.getResponseBodyAsString()));
        } catch (Exception ex) {
            log.warn("Failed to fetch pricing for card {}", card.getExternalId(), ex);
            return PricingFetchResult.error("ERROR", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    @Override
    public PagedResult fetchEpisodesPage(int page) {
        String apiKey = settingsService.getProviderApiKey();
        String host = settingsService.getProviderHost();
        String episodesPath = settingsService.getProviderEpisodesPath();

        if (apiKey.isBlank() || host.isBlank() || episodesPath.isBlank()) {
            return PagedResult.configMissing("Provider settings are incomplete");
        }

        try {
            Object payload = webClientBuilder
                    .baseUrl("https://" + host)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(episodesPath)
                            .queryParam("page", Math.max(1, page))
                            .build())
                    .header("X-RapidAPI-Key", apiKey)
                    .header("X-RapidAPI-Host", host)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block(Duration.ofSeconds(20));

            return parsePagedPayload(payload);
        } catch (WebClientResponseException ex) {
            String status = "HTTP_" + ex.getStatusCode().value();
            return PagedResult.error(status, truncate(ex.getResponseBodyAsString()));
        } catch (Exception ex) {
            log.warn("Failed to fetch provider episodes page {}", page, ex);
            return PagedResult.error("ERROR", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    @Override
    public PagedResult fetchEpisodeCardsPage(long episodeId, int page, int perPage) {
        String apiKey = settingsService.getProviderApiKey();
        String host = settingsService.getProviderHost();
        String episodeCardsPathTemplate = settingsService.getProviderEpisodeCardsPathTemplate();

        if (apiKey.isBlank() || host.isBlank() || episodeCardsPathTemplate.isBlank()) {
            return PagedResult.configMissing("Provider settings are incomplete");
        }

        try {
            String resolvedPath = episodeCardsPathTemplate.replace("{episodeId}", String.valueOf(episodeId));
            Object payload = webClientBuilder
                    .baseUrl("https://" + host)
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path(resolvedPath)
                            .queryParam("page", Math.max(1, page))
                            .queryParam("per_page", Math.max(1, perPage))
                            .queryParam("sort", "card_number_highest")
                            .build())
                    .header("X-RapidAPI-Key", apiKey)
                    .header("X-RapidAPI-Host", host)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .block(Duration.ofSeconds(20));

            return parsePagedPayload(payload);
        } catch (WebClientResponseException ex) {
            String status = "HTTP_" + ex.getStatusCode().value();
            return PagedResult.error(status, truncate(ex.getResponseBodyAsString()));
        } catch (Exception ex) {
            log.warn("Failed to fetch provider episode cards (episode={}, page={}, perPage={})", episodeId, page, perPage, ex);
            return PagedResult.error("ERROR", ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private PagedResult parsePagedPayload(Object payload) {
        if (!(payload instanceof Map<?, ?> root)) {
            return PagedResult.error("UNRESOLVED", "Provider payload root is not an object");
        }

        List<Map<String, Object>> rows = new ArrayList<>();
        Object dataNode = root.get("data");
        if (dataNode instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    Map<String, Object> normalized = new LinkedHashMap<>();
                    for (Map.Entry<?, ?> entry : m.entrySet()) {
                        normalized.put(String.valueOf(entry.getKey()), entry.getValue());
                    }
                    rows.add(normalized);
                }
            }
        }

        PricingProviderClient.Paging paging = null;
        Object pagingNode = root.get("paging");
        if (pagingNode instanceof Map<?, ?> p) {
            int current = parseNumber(p.get("current"), 1);
            int total = parseNumber(p.get("total"), Math.max(1, current));
            int perPage = parseNumber(p.get("per_page"), rows.size());
            paging = new PricingProviderClient.Paging(current, total, Math.max(1, perPage));
        }

        if (paging == null) {
            paging = new PricingProviderClient.Paging(1, 1, Math.max(1, rows.size()));
        }

        return PagedResult.success(rows, paging);
    }

    private String buildDeterministicQuery(Card card) {
        StringBuilder query = new StringBuilder();
        if (card.getName() != null && !card.getName().isBlank()) {
            query.append(card.getName().trim());
        }
        if (card.getCardNumber() != null) {
            if (!query.isEmpty()) {
                query.append(" ");
            }
            query.append("#").append(card.getCardNumber());
        }
        if (card.getEdition() != null && card.getEdition().getCode() != null && !card.getEdition().getCode().isBlank()) {
            if (!query.isEmpty()) {
                query.append(" ");
            }
            query.append("[").append(card.getEdition().getCode().trim()).append("]");
        }
        if (query.isEmpty() && card.getExternalId() != null) {
            query.append(card.getExternalId());
        }
        return query.toString();
    }

    private BigDecimal extractPrice(Object payload) {
        if (payload == null) {
            return null;
        }
        if (payload instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (payload instanceof String text) {
            try {
                return new BigDecimal(text.trim());
            } catch (Exception ignored) {
                return null;
            }
        }
        if (payload instanceof Map<?, ?> map) {
            BigDecimal hinted = extractFromKnownKeys(map);
            if (hinted != null) {
                return hinted;
            }
            for (Object value : map.values()) {
                BigDecimal nested = extractPrice(value);
                if (nested != null) {
                    return nested;
                }
            }
        }
        if (payload instanceof List<?> list) {
            for (Object value : list) {
                BigDecimal nested = extractPrice(value);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private BigDecimal extractFromKnownKeys(Map<?, ?> map) {
        List<String> candidateKeys = List.of(
                "marketPrice", "market_price", "price", "value", "amount", "eur", "usd"
        );

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey()).toLowerCase(Locale.ROOT);
            boolean likelyPrice = candidateKeys.stream().anyMatch(key::contains);
            if (!likelyPrice) {
                continue;
            }
            BigDecimal parsed = extractPrice(entry.getValue());
            if (parsed != null) {
                return parsed;
            }
        }
        return null;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        return value.length() <= 300 ? value : value.substring(0, 300);
    }

    private int parseNumber(Object value, int fallback) {
        if (value instanceof Number n) {
            return n.intValue();
        }
        if (value instanceof String s) {
            try {
                return Integer.parseInt(s.trim());
            } catch (Exception ignored) {
                return fallback;
            }
        }
        return fallback;
    }
}
