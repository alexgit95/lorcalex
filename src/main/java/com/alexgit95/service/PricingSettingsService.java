package com.alexgit95.service;

import com.alexgit95.model.AppSettings;
import com.alexgit95.repository.AppSettingsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PricingSettingsService {

    public static final String KEY_SYNC_ENABLED = "pricing_sync_enabled";
    public static final String KEY_DAILY_BUDGET = "pricing_daily_budget";
    public static final String KEY_DAILY_HARD_LIMIT = "pricing_daily_hard_limit";
    public static final String KEY_DAILY_SAFETY_MARGIN = "pricing_daily_safety_margin";
    public static final String KEY_MINUTE_LIMIT = "pricing_minute_limit";
    public static final String KEY_USED_ATTEMPTS = "pricing_used_attempts";
    public static final String KEY_USAGE_DATE = "pricing_usage_date";
    public static final String KEY_PROVIDER = "pricing_provider";
    public static final String KEY_PROVIDER_HOST = "pricing_provider_host";
    public static final String KEY_PROVIDER_PATH = "pricing_provider_path";
    public static final String KEY_PROVIDER_EPISODES_PATH = "pricing_provider_episodes_path";
    public static final String KEY_PROVIDER_EPISODE_CARDS_PATH_TEMPLATE = "pricing_provider_episode_cards_path_template";
    public static final String KEY_PROVIDER_API_KEY = "pricing_provider_api_key";
    public static final String KEY_PROVIDER_CURRENCY = "pricing_provider_currency";
    public static final String KEY_SCHEDULE_CRON = "pricing_schedule_cron";
    public static final String KEY_LAST_SCHEDULED_RUN_DATE = "pricing_last_scheduled_run_date";
    public static final String KEY_CURSOR_PHASE = "pricing_cursor_phase";
    public static final String KEY_CURSOR_EPISODE_PAGE = "pricing_cursor_episode_page";
    public static final String KEY_CURSOR_EPISODE_ID = "pricing_cursor_episode_id";
    public static final String KEY_CURSOR_EPISODE_CARDS_PAGE = "pricing_cursor_episode_cards_page";
    public static final String KEY_LAST_STOP_REASON = "pricing_last_stop_reason";

    private static final int MAX_HARD_LIMIT = 100;
    private static final int MAX_MINUTE_LIMIT = 30;

    private final AppSettingsRepository settingsRepository;

    public PricingSettingsService(AppSettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public synchronized boolean isSyncEnabled() {
        return parseBoolean(getValueOrDefault(KEY_SYNC_ENABLED, "true"), true);
    }

    public synchronized int getDailyBudget() {
        return getEffectiveDailyBudget();
    }

    public synchronized int getDailyHardLimit() {
        int fromHardLimit = parseInt(getValueOrDefault(KEY_DAILY_HARD_LIMIT, "100"), 100);
        int legacy = parseInt(getValueOrDefault(KEY_DAILY_BUDGET, String.valueOf(fromHardLimit)), fromHardLimit);
        int selected = Math.min(fromHardLimit, legacy);
        return clamp(selected, 0, MAX_HARD_LIMIT);
    }

    public synchronized int getDailySafetyMargin() {
        int safety = parseInt(getValueOrDefault(KEY_DAILY_SAFETY_MARGIN, "0"), 0);
        return clamp(safety, 0, getDailyHardLimit());
    }

    public synchronized int getEffectiveDailyBudget() {
        return Math.max(0, getDailyHardLimit() - getDailySafetyMargin());
    }

    public synchronized int getMinuteLimit() {
        int configured = parseInt(getValueOrDefault(KEY_MINUTE_LIMIT, "30"), 30);
        return clamp(configured, 1, MAX_MINUTE_LIMIT);
    }

    public synchronized String getProviderName() {
        return getValueOrDefault(KEY_PROVIDER, "rapidapi-lorcana-prices").trim();
    }

    public synchronized String getProviderHost() {
        return getValueOrDefault(KEY_PROVIDER_HOST, "lorcana-api-by-tcggo.p.rapidapi.com").trim();
    }

    public synchronized String getProviderPath() {
        return getValueOrDefault(KEY_PROVIDER_PATH, "/cards/search").trim();
    }

    public synchronized String getProviderEpisodesPath() {
        return getValueOrDefault(KEY_PROVIDER_EPISODES_PATH, "/episodes").trim();
    }

    public synchronized String getProviderEpisodeCardsPathTemplate() {
        return getValueOrDefault(KEY_PROVIDER_EPISODE_CARDS_PATH_TEMPLATE, "/episodes/{episodeId}/cards").trim();
    }

    public synchronized String getProviderApiKey() {
        return getValueOrDefault(KEY_PROVIDER_API_KEY, "").trim();
    }

    public synchronized String getProviderCurrency() {
        return getValueOrDefault(KEY_PROVIDER_CURRENCY, "EUR").trim();
    }

    public synchronized boolean isProviderConfigured() {
        return !getProviderApiKey().isBlank()
                && !getProviderHost().isBlank()
                && !getProviderEpisodesPath().isBlank()
                && !getProviderEpisodeCardsPathTemplate().isBlank();
    }

    public synchronized String getScheduleCron() {
        return getValueOrDefault(KEY_SCHEDULE_CRON, "0 0 2 * * *").trim();
    }

    public synchronized LocalDate getLastScheduledRunDate() {
        String raw = getOptionalValue(KEY_LAST_SCHEDULED_RUN_DATE);
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    @Transactional
    public synchronized void setLastScheduledRunDate(LocalDate date) {
        if (date == null) {
            return;
        }
        upsert(KEY_LAST_SCHEDULED_RUN_DATE,
                date.toString(),
                "Date du dernier run pricing planifié (yyyy-MM-dd)");
    }

    @Transactional
    public synchronized boolean tryConsumeAttempt() {
        return tryConsumeCall();
    }

    @Transactional
    public synchronized boolean tryConsumeCall() {
        Usage usage = loadAndNormalizeUsage(LocalDate.now());
        int budget = getEffectiveDailyBudget();
        if (usage.usedAttempts >= budget) {
            return false;
        }

        usage.usedAttempts += 1;
        persistUsage(usage);
        return true;
    }

    @Transactional
    public synchronized boolean hasRemainingAttempts() {
        return hasRemainingCalls();
    }

    @Transactional
    public synchronized boolean hasRemainingCalls() {
        Usage usage = loadAndNormalizeUsage(LocalDate.now());
        int budget = getEffectiveDailyBudget();
        return usage.usedAttempts < budget;
    }

    @Transactional
    public synchronized Map<String, Object> getBudgetStatus() {
        Usage usage = loadAndNormalizeUsage(LocalDate.now());
        int hardLimit = getDailyHardLimit();
        int safetyMargin = getDailySafetyMargin();
        int budget = getEffectiveDailyBudget();
        int remaining = Math.max(0, budget - usage.usedAttempts);

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("syncEnabled", isSyncEnabled());
        status.put("dailyHardLimit", hardLimit);
        status.put("dailySafetyMargin", safetyMargin);
        status.put("effectiveDailyBudget", budget);
        status.put("minuteLimit", getMinuteLimit());
        status.put("dailyBudget", budget);
        status.put("usedAttempts", usage.usedAttempts);
        status.put("remainingAttempts", remaining);
        status.put("usageDate", usage.usageDate.toString());
        status.put("provider", getProviderName());
        status.put("providerHost", getProviderHost());
        status.put("providerEpisodesPath", getProviderEpisodesPath());
        status.put("providerEpisodeCardsPathTemplate", getProviderEpisodeCardsPathTemplate());
        status.put("providerConfigured", isProviderConfigured());
        status.put("scheduleCron", getScheduleCron());
        status.put("cursor", getCursor().toMap());
        status.put("lastStopReason", getLastStopReason());
        LocalDate lastRun = getLastScheduledRunDate();
        status.put("lastScheduledRunDate", lastRun != null ? lastRun.toString() : "");
        return status;
    }

    public synchronized CursorState getCursor() {
        String phaseRaw = getValueOrDefault(KEY_CURSOR_PHASE, CursorPhase.EPISODES.name());
        CursorPhase phase;
        try {
            phase = CursorPhase.valueOf(phaseRaw.trim().toUpperCase());
        } catch (Exception ignored) {
            phase = CursorPhase.EPISODES;
        }

        int episodePage = Math.max(1, parseInt(getValueOrDefault(KEY_CURSOR_EPISODE_PAGE, "1"), 1));
        long episodeId = Math.max(0, parseLong(getValueOrDefault(KEY_CURSOR_EPISODE_ID, "0"), 0));
        int episodeCardsPage = Math.max(1, parseInt(getValueOrDefault(KEY_CURSOR_EPISODE_CARDS_PAGE, "1"), 1));
        return new CursorState(phase, episodePage, episodeId, episodeCardsPage);
    }

    @Transactional
    public synchronized void persistCursor(CursorState state) {
        CursorState safe = state == null ? CursorState.initial() : state;
        upsert(KEY_CURSOR_PHASE, safe.phase.name(), "Phase courante du curseur pricing");
        upsert(KEY_CURSOR_EPISODE_PAGE, String.valueOf(Math.max(1, safe.episodePage)), "Page episodes courante du curseur pricing");
        upsert(KEY_CURSOR_EPISODE_ID, String.valueOf(Math.max(0, safe.episodeId)), "Episode courant du curseur pricing");
        upsert(KEY_CURSOR_EPISODE_CARDS_PAGE, String.valueOf(Math.max(1, safe.episodeCardsPage)), "Page cartes episode courante du curseur pricing");
    }

    @Transactional
    public synchronized void resetCursor() {
        persistCursor(CursorState.initial());
    }

    public synchronized String getLastStopReason() {
        return getValueOrDefault(KEY_LAST_STOP_REASON, "").trim();
    }

    @Transactional
    public synchronized void setLastStopReason(String reasonCode) {
        upsert(KEY_LAST_STOP_REASON,
                reasonCode == null ? "" : reasonCode.trim(),
                "Derniere raison d'arret de synchronisation pricing");
    }

    private Usage loadAndNormalizeUsage(LocalDate today) {
        LocalDate usageDate = parseDate(getValueOrDefault(KEY_USAGE_DATE, ""), today);
        int usedAttempts = Math.max(0, parseInt(getValueOrDefault(KEY_USED_ATTEMPTS, "0"), 0));

        Usage usage = new Usage(usageDate, usedAttempts);
        if (!today.equals(usage.usageDate)) {
            usage.usageDate = today;
            usage.usedAttempts = 0;
            persistUsage(usage);
        }
        return usage;
    }

    private void persistUsage(Usage usage) {
        upsert(KEY_USAGE_DATE, usage.usageDate.toString(), "Date d'usage (yyyy-MM-dd) du compteur de tentatives pricing");
        upsert(KEY_USED_ATTEMPTS, String.valueOf(usage.usedAttempts), "Nombre de tentatives pricing déjà consommées pour la date d'usage");
    }

    private String getValueOrDefault(String key, String defaultValue) {
        return settingsRepository.findBySettingKey(key)
                .map(AppSettings::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValue);
    }

    private String getOptionalValue(String key) {
        return settingsRepository.findBySettingKey(key)
                .map(AppSettings::getSettingValue)
                .orElse(null);
    }

    private void upsert(String key, String value, String description) {
        AppSettings setting = settingsRepository.findBySettingKey(key)
                .orElse(new AppSettings(key, null, description));
        setting.setSettingValue(value);
        if (setting.getDescription() == null || setting.getDescription().isBlank()) {
            setting.setDescription(description);
        }
        settingsRepository.save(setting);
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Integer.parseInt(raw.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static long parseLong(String raw, long fallback) {
        try {
            return Long.parseLong(raw.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static boolean parseBoolean(String raw, boolean fallback) {
        if (raw == null) {
            return fallback;
        }
        if ("true".equalsIgnoreCase(raw) || "1".equals(raw)) {
            return true;
        }
        if ("false".equalsIgnoreCase(raw) || "0".equals(raw)) {
            return false;
        }
        return fallback;
    }

    private static LocalDate parseDate(String raw, LocalDate fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(raw.trim());
        } catch (DateTimeParseException ignored) {
            return fallback;
        }
    }

    private static final class Usage {
        private LocalDate usageDate;
        private int usedAttempts;

        private Usage(LocalDate usageDate, int usedAttempts) {
            this.usageDate = usageDate;
            this.usedAttempts = usedAttempts;
        }
    }

    public enum CursorPhase {
        EPISODES,
        EPISODE_CARDS
    }

    public static final class CursorState {
        private final CursorPhase phase;
        private final int episodePage;
        private final long episodeId;
        private final int episodeCardsPage;

        public CursorState(CursorPhase phase, int episodePage, long episodeId, int episodeCardsPage) {
            this.phase = phase == null ? CursorPhase.EPISODES : phase;
            this.episodePage = Math.max(1, episodePage);
            this.episodeId = Math.max(0, episodeId);
            this.episodeCardsPage = Math.max(1, episodeCardsPage);
        }

        public static CursorState initial() {
            return new CursorState(CursorPhase.EPISODES, 1, 0, 1);
        }

        public CursorPhase phase() {
            return phase;
        }

        public int episodePage() {
            return episodePage;
        }

        public long episodeId() {
            return episodeId;
        }

        public int episodeCardsPage() {
            return episodeCardsPage;
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("phase", phase.name());
            map.put("episodePage", episodePage);
            map.put("episodeId", episodeId);
            map.put("episodeCardsPage", episodeCardsPage);
            return map;
        }
    }

    private static int clamp(int value, int min, int max) {
        return Math.min(max, Math.max(min, value));
    }
}
