package com.alexgit95.service;

import com.alexgit95.model.Card;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface PricingProviderClient {

    PricingFetchResult fetchPrice(Card card);

    PagedResult fetchEpisodesPage(int page);

    PagedResult fetchEpisodeCardsPage(long episodeId, int page, int perPage);

    record PagedResult(
            boolean success,
            String status,
            String details,
            boolean callAttempted,
            List<Map<String, Object>> data,
            Paging paging
    ) {
        public static PagedResult success(List<Map<String, Object>> data, Paging paging) {
            return new PagedResult(true, "SUCCESS", null, true, data, paging);
        }

        public static PagedResult error(String status, String details) {
            return new PagedResult(false, status, details, true, List.of(), null);
        }

        public static PagedResult configMissing(String details) {
            return new PagedResult(false, "CONFIG_MISSING", details, false, List.of(), null);
        }
    }

    record Paging(int current, int total, int perPage) {
    }

    record PricingFetchResult(
            boolean success,
            BigDecimal marketPrice,
            String currency,
            String source,
            String status,
            String details,
            boolean callAttempted
    ) {
        public static PricingFetchResult success(BigDecimal marketPrice, String currency, String source) {
            return new PricingFetchResult(true, marketPrice, currency, source, "SUCCESS", null, true);
        }

        public static PricingFetchResult unresolved(String status, String details) {
            return new PricingFetchResult(false, null, null, null, status, details, true);
        }

        public static PricingFetchResult error(String status, String details) {
            return new PricingFetchResult(false, null, null, null, status, details, true);
        }

        public static PricingFetchResult configMissing(String details) {
            return new PricingFetchResult(false, null, null, null, "CONFIG_MISSING", details, false);
        }
    }
}
