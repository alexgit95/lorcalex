package com.alexgit95.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PricingScheduleServiceTest {

    @Test
    @DisplayName("resolveCron keeps valid configured cron")
    void resolveCron_keepsValidCron() {
        PricingScheduleService.CronResolution resolution = PricingScheduleService.resolveCron("0 15 3 * * *");

        assertThat(resolution.valid()).isTrue();
        assertThat(resolution.effectiveCron()).isEqualTo("0 15 3 * * *");
    }

    @Test
    @DisplayName("resolveCron falls back to safe default when cron is invalid")
    void resolveCron_fallsBackForInvalidCron() {
        PricingScheduleService.CronResolution resolution = PricingScheduleService.resolveCron("invalid-cron");

        assertThat(resolution.valid()).isFalse();
        assertThat(resolution.effectiveCron()).isEqualTo("0 0 2 * * *");
    }
}
