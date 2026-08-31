package com.alexgit95.service;

import com.alexgit95.model.AppSettings;
import com.alexgit95.repository.AppSettingsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingSettingsServiceTest {

    @Mock
    private AppSettingsRepository repository;

    private PricingSettingsService service;
    private Map<String, AppSettings> store;

    @BeforeEach
    void setUp() {
        store = new LinkedHashMap<>();

        lenient().when(repository.findBySettingKey(anyString())).thenAnswer(invocation -> {
            String key = invocation.getArgument(0, String.class);
            return Optional.ofNullable(store.get(key));
        });

        lenient().when(repository.save(any(AppSettings.class))).thenAnswer(invocation -> {
            AppSettings setting = invocation.getArgument(0, AppSettings.class);
            store.put(setting.getSettingKey(), setting);
            return setting;
        });

        service = new PricingSettingsService(repository);
    }

    @Test
    @DisplayName("tryConsumeAttempt resets old usage date and consumes one attempt")
    void tryConsumeAttempt_resetsAndConsumesOnDateRollover() {
        put(PricingSettingsService.KEY_DAILY_BUDGET, "2");
        put(PricingSettingsService.KEY_DAILY_HARD_LIMIT, "2");
        put(PricingSettingsService.KEY_DAILY_SAFETY_MARGIN, "0");
        put(PricingSettingsService.KEY_USED_ATTEMPTS, "2");
        put(PricingSettingsService.KEY_USAGE_DATE, LocalDate.now().minusDays(1).toString());

        boolean consumed = service.tryConsumeAttempt();

        assertThat(consumed).isTrue();
        Map<String, Object> status = service.getBudgetStatus();
        assertThat(status.get("usedAttempts")).isEqualTo(1);
        assertThat(status.get("remainingAttempts")).isEqualTo(1);
        assertThat(status.get("usageDate")).isEqualTo(LocalDate.now().toString());
    }

    @Test
    @DisplayName("tryConsumeAttempt returns false when budget is reached")
    void tryConsumeAttempt_returnsFalseWhenBudgetReached() {
        put(PricingSettingsService.KEY_DAILY_BUDGET, "3");
        put(PricingSettingsService.KEY_DAILY_HARD_LIMIT, "3");
        put(PricingSettingsService.KEY_DAILY_SAFETY_MARGIN, "0");
        put(PricingSettingsService.KEY_USED_ATTEMPTS, "3");
        put(PricingSettingsService.KEY_USAGE_DATE, LocalDate.now().toString());

        boolean consumed = service.tryConsumeAttempt();

        assertThat(consumed).isFalse();
        Map<String, Object> status = service.getBudgetStatus();
        assertThat(status.get("usedAttempts")).isEqualTo(3);
        assertThat(status.get("remainingAttempts")).isEqualTo(0);
    }

    @Test
    @DisplayName("isHighPriceLogEnabled defaults to true and honors explicit setting")
    void isHighPriceLogEnabled_defaultsTrueAndRespectsSetting() {
        assertThat(service.isHighPriceLogEnabled()).isTrue();

        put(PricingSettingsService.KEY_LOG_HIGH_PRICE_ENABLED, "false");
        assertThat(service.isHighPriceLogEnabled()).isFalse();
    }

    @Test
    @DisplayName("getHighPriceLogThreshold defaults to 5, honors explicit value, and clamps negative values to 0")
    void getHighPriceLogThreshold_defaultsAndClamps() {
        assertThat(service.getHighPriceLogThreshold()).isEqualTo(5);

        put(PricingSettingsService.KEY_LOG_HIGH_PRICE_THRESHOLD, "20");
        assertThat(service.getHighPriceLogThreshold()).isEqualTo(20);

        put(PricingSettingsService.KEY_LOG_HIGH_PRICE_THRESHOLD, "-3");
        assertThat(service.getHighPriceLogThreshold()).isEqualTo(0);
    }

    @Test
    @DisplayName("isAbnormalPriceLogEnabled defaults to false and honors explicit setting")
    void isAbnormalPriceLogEnabled_defaultsFalseAndRespectsSetting() {
        assertThat(service.isAbnormalPriceLogEnabled()).isFalse();

        put(PricingSettingsService.KEY_LOG_ABNORMAL_PRICE_ENABLED, "true");
        assertThat(service.isAbnormalPriceLogEnabled()).isTrue();
    }

    @Test
    @DisplayName("getAbnormalPriceLogThreshold defaults to 5, honors explicit value, and clamps negative values to 0")
    void getAbnormalPriceLogThreshold_defaultsAndClamps() {
        assertThat(service.getAbnormalPriceLogThreshold()).isEqualTo(5);

        put(PricingSettingsService.KEY_LOG_ABNORMAL_PRICE_THRESHOLD, "12");
        assertThat(service.getAbnormalPriceLogThreshold()).isEqualTo(12);

        put(PricingSettingsService.KEY_LOG_ABNORMAL_PRICE_THRESHOLD, "-1");
        assertThat(service.getAbnormalPriceLogThreshold()).isEqualTo(0);
    }

    @Test
    @DisplayName("getAbnormalPriceLogRarities defaults to the base rarities and parses custom CSV, trimmed and lowercased")
    void getAbnormalPriceLogRarities_defaultsAndParsesCsv() {
        assertThat(service.getAbnormalPriceLogRarities())
                .containsExactlyInAnyOrder("common", "uncommon", "rare", "super_rare");

        put(PricingSettingsService.KEY_LOG_ABNORMAL_PRICE_RARITIES, " Common,  , RARE ,Legendary");
        assertThat(service.getAbnormalPriceLogRarities())
                .containsExactlyInAnyOrder("common", "rare", "legendary");
    }

    @Test
    @DisplayName("isUnresolvedMappingLogEnabled defaults to false and honors explicit setting")
    void isUnresolvedMappingLogEnabled_defaultsFalseAndRespectsSetting() {
        assertThat(service.isUnresolvedMappingLogEnabled()).isFalse();

        put(PricingSettingsService.KEY_LOG_UNRESOLVED_MAPPING_ENABLED, "true");
        assertThat(service.isUnresolvedMappingLogEnabled()).isTrue();
    }

    private void put(String key, String value) {
        store.put(key, new AppSettings(key, value, "test"));
    }
}
