package com.alexgit95.service;

import com.alexgit95.model.AppSettings;
import com.alexgit95.model.Card;
import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.profiles.active=test")
@TestPropertySource(locations = "classpath:application-test.properties")
class PricingSyncIntegrationTest {

    @Autowired
    private PricingSyncService pricingSyncService;

    @Autowired
    private PricingSettingsService pricingSettingsService;

    @Autowired
    private PricingScheduleService pricingScheduleService;

    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private UserCollectionRepository userCollectionRepository;

    @Autowired
    private AppSettingsRepository settingsRepository;

    @BeforeEach
    void setUp() {
        userCollectionRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        settingsRepository.deleteAllInBatch();

        putSetting(PricingSettingsService.KEY_SYNC_ENABLED, "true");
        putSetting(PricingSettingsService.KEY_PROVIDER_API_KEY, "");
        putSetting(PricingSettingsService.KEY_PROVIDER_HOST, "lorcana-api-by-tcggo.p.rapidapi.com");
        putSetting(PricingSettingsService.KEY_PROVIDER_PATH, "/cards/search");
        putSetting(PricingSettingsService.KEY_PROVIDER_CURRENCY, "EUR");
    }

    @Test
    @DisplayName("queue prioritizes missing price first then oldest refreshed")
    void runSync_prioritizesMissingThenOldest() {
        putSetting(PricingSettingsService.KEY_DAILY_BUDGET, "10");
        putSetting(PricingSettingsService.KEY_DAILY_SAFETY_MARGIN, "0");
        putSetting(PricingSettingsService.KEY_USED_ATTEMPTS, "0");
        putSetting(PricingSettingsService.KEY_USAGE_DATE, LocalDate.now().toString());

        createCard("missing", null);
        createCard("oldest", LocalDateTime.now().minusDays(10));
        createCard("newest", LocalDateTime.now().minusDays(1));

        Map<String, Object> report = pricingSyncService.runSync("manual", 2);

        Card missing = cardRepository.findByExternalId("missing").orElseThrow();
        Card oldest = cardRepository.findByExternalId("oldest").orElseThrow();
        Card newest = cardRepository.findByExternalId("newest").orElseThrow();

        assertThat(report.get("attempted")).isEqualTo(0);
        assertThat(report.get("processed")).isEqualTo(0);
        assertThat(report.get("unresolvedCount")).isEqualTo(0);
        assertThat(report.get("reasonCode")).isEqualTo("PROVIDER_CONFIG_MISSING");

        assertThat(missing.getLastPriceStatus()).isNull();
        assertThat(oldest.getLastPriceStatus()).isNull();
        assertThat(newest.getLastPriceStatus()).isNull();
    }

    @Test
    @DisplayName("sync stops when budget is exhausted")
    void runSync_stopsOnBudgetExhaustion() {
        putSetting(PricingSettingsService.KEY_DAILY_BUDGET, "1");
        putSetting(PricingSettingsService.KEY_DAILY_SAFETY_MARGIN, "0");
        putSetting(PricingSettingsService.KEY_USED_ATTEMPTS, "0");
        putSetting(PricingSettingsService.KEY_USAGE_DATE, LocalDate.now().toString());

        createCard("c1", null);
        createCard("c2", null);

        Map<String, Object> report = pricingSyncService.runSync("manual", null);

        long touched = cardRepository.findAll().stream()
                .filter(c -> c.getLastPriceStatus() != null)
                .count();

        assertThat(report.get("attempted")).isEqualTo(0);
        assertThat(report.get("reasonCode")).isEqualTo("PROVIDER_CONFIG_MISSING");
        assertThat(touched).isEqualTo(0);
    }

    @Test
    @DisplayName("daily rollover resets used attempts before sync")
    void runSync_rolloverResetsCounters() {
        putSetting(PricingSettingsService.KEY_DAILY_BUDGET, "2");
        putSetting(PricingSettingsService.KEY_DAILY_SAFETY_MARGIN, "0");
        putSetting(PricingSettingsService.KEY_USED_ATTEMPTS, "2");
        putSetting(PricingSettingsService.KEY_USAGE_DATE, LocalDate.now().minusDays(1).toString());

        createCard("rollover", null);

        Map<String, Object> report = pricingSyncService.runSync("manual", 1);
        Map<String, Object> status = pricingSettingsService.getBudgetStatus();

        assertThat(report.get("attempted")).isEqualTo(0);
        assertThat(status.get("usedAttempts")).isEqualTo(0);
        assertThat(status.get("remainingAttempts")).isEqualTo(2);
        assertThat(status.get("usageDate")).isEqualTo(LocalDate.now().toString());
    }

    @Test
    @DisplayName("budget usage persists across consecutive runs")
    void runSync_budgetPersistsAcrossRuns() {
        putSetting(PricingSettingsService.KEY_DAILY_BUDGET, "1");
        putSetting(PricingSettingsService.KEY_DAILY_SAFETY_MARGIN, "0");
        putSetting(PricingSettingsService.KEY_USED_ATTEMPTS, "0");
        putSetting(PricingSettingsService.KEY_USAGE_DATE, LocalDate.now().toString());

        createCard("first", null);
        createCard("second", null);

        Map<String, Object> firstRun = pricingSyncService.runSync("manual", null);
        Map<String, Object> secondRun = pricingSyncService.runSync("manual", null);

        assertThat(firstRun.get("attempted")).isEqualTo(0);
        assertThat(secondRun.get("attempted")).isEqualTo(0);
        assertThat(secondRun.get("reasonCode")).isEqualTo("PROVIDER_CONFIG_MISSING");
    }

    @Test
    @DisplayName("startup catch-up runs at most once per day")
    void startupCatchup_runsOncePerDay() {
        putSetting(PricingSettingsService.KEY_DAILY_BUDGET, "2");
        putSetting(PricingSettingsService.KEY_DAILY_SAFETY_MARGIN, "0");
        putSetting(PricingSettingsService.KEY_USED_ATTEMPTS, "0");
        putSetting(PricingSettingsService.KEY_USAGE_DATE, LocalDate.now().toString());
        putSetting(PricingSettingsService.KEY_LAST_SCHEDULED_RUN_DATE, LocalDate.now().minusDays(1).toString());

        createCard("catchup", null);

        pricingScheduleService.runStartupCatchupIfNeeded();
        pricingScheduleService.runStartupCatchupIfNeeded();

        Card catchup = cardRepository.findByExternalId("catchup").orElseThrow();
        Map<String, Object> status = pricingSettingsService.getBudgetStatus();

        assertThat(catchup.getLastPriceStatus()).isNull();
        assertThat(status.get("usedAttempts")).isEqualTo(0);
        assertThat(pricingSettingsService.getLastScheduledRunDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("startup catch-up is skipped when day already processed")
    void startupCatchup_skippedWhenAlreadyProcessedToday() {
        putSetting(PricingSettingsService.KEY_DAILY_BUDGET, "2");
        putSetting(PricingSettingsService.KEY_DAILY_SAFETY_MARGIN, "0");
        putSetting(PricingSettingsService.KEY_USED_ATTEMPTS, "0");
        putSetting(PricingSettingsService.KEY_USAGE_DATE, LocalDate.now().toString());
        putSetting(PricingSettingsService.KEY_LAST_SCHEDULED_RUN_DATE, LocalDate.now().toString());

        createCard("already-processed", null);

        pricingScheduleService.runStartupCatchupIfNeeded();

        Card card = cardRepository.findByExternalId("already-processed").orElseThrow();
        Map<String, Object> status = pricingSettingsService.getBudgetStatus();

        assertThat(card.getLastPriceStatus()).isNull();
        assertThat(status.get("usedAttempts")).isEqualTo(0);
    }

    private void createCard(String externalId, LocalDateTime lastPriceAt) {
        Card card = new Card();
        card.setExternalId(externalId);
        card.setName("Card " + externalId);
        card.setLastPriceAt(lastPriceAt);
        cardRepository.save(card);
    }

    private void putSetting(String key, String value) {
        AppSettings setting = settingsRepository.findBySettingKey(key)
                .orElse(new AppSettings(key, null, "test"));
        setting.setSettingValue(value);
        settingsRepository.save(setting);
    }
}
