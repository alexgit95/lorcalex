package com.alexgit95.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;

@Service
public class PricingScheduleService {

    private static final Logger log = LoggerFactory.getLogger(PricingScheduleService.class);
    private static final String DEFAULT_CRON = "0 0 2 * * *";

    private final PricingSyncService pricingSyncService;
    private final PricingSettingsService pricingSettingsService;

    private final ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    private ScheduledFuture<?> scheduledFuture;

    private String configuredCron = DEFAULT_CRON;
    private String effectiveCron = DEFAULT_CRON;
    private boolean cronValid = true;

    public PricingScheduleService(PricingSyncService pricingSyncService,
                                  PricingSettingsService pricingSettingsService) {
        this.pricingSyncService = pricingSyncService;
        this.pricingSettingsService = pricingSettingsService;
    }

    @PostConstruct
    void init() {
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("pricing-schedule-");
        scheduler.initialize();
        refreshSchedule();
    }

    @PreDestroy
    void shutdown() {
        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }
        scheduler.shutdown();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        runStartupCatchupIfNeeded();
    }

    public synchronized void onSettingUpdated(String key) {
        if (PricingSettingsService.KEY_SCHEDULE_CRON.equals(key)
                || PricingSettingsService.KEY_SYNC_ENABLED.equals(key)) {
            refreshSchedule();
        }
    }

    public synchronized void refreshSchedule() {
        String settingCron = pricingSettingsService.getScheduleCron();
        CronResolution resolution = resolveCron(settingCron);

        boolean changed = !Objects.equals(configuredCron, settingCron)
                || !Objects.equals(effectiveCron, resolution.effectiveCron())
                || cronValid != resolution.valid();

        configuredCron = settingCron;
        effectiveCron = resolution.effectiveCron();
        cronValid = resolution.valid();

        if (!changed && scheduledFuture != null) {
            return;
        }

        if (scheduledFuture != null) {
            scheduledFuture.cancel(false);
        }

        scheduledFuture = scheduler.schedule(this::runScheduledSync, new CronTrigger(effectiveCron));
    }

    public synchronized Map<String, Object> getScheduleStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("scheduleCron", configuredCron);
        status.put("scheduleEffectiveCron", effectiveCron);
        status.put("scheduleValid", cronValid);
        status.put("scheduleFallbackUsed", !cronValid);
        status.put("scheduleNextRun", nextRun(effectiveCron));

        LocalDate lastRunDate = pricingSettingsService.getLastScheduledRunDate();
        status.put("lastScheduledRunDate", lastRunDate != null ? lastRunDate.toString() : "");
        return status;
    }

    public void runStartupCatchupIfNeeded() {
        if (!pricingSettingsService.isSyncEnabled()) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate lastRunDate = pricingSettingsService.getLastScheduledRunDate();
        if (today.equals(lastRunDate)) {
            return;
        }

        Map<String, Object> result = pricingSyncService.runSync("startup_catchup", null);
        if (Boolean.TRUE.equals(result.get("started"))) {
            pricingSettingsService.setLastScheduledRunDate(today);
        }
    }

    private void runScheduledSync() {
        if (!pricingSettingsService.isSyncEnabled()) {
            return;
        }

        Map<String, Object> result = pricingSyncService.runSync("scheduled", null);
        if (Boolean.TRUE.equals(result.get("started"))) {
            pricingSettingsService.setLastScheduledRunDate(LocalDate.now());
        }
    }

    static CronResolution resolveCron(String configured) {
        String candidate = configured == null || configured.isBlank() ? DEFAULT_CRON : configured.trim();
        try {
            CronExpression.parse(candidate);
            return new CronResolution(candidate, true);
        } catch (Exception ignored) {
            return new CronResolution(DEFAULT_CRON, false);
        }
    }

    private static String nextRun(String cron) {
        try {
            ZonedDateTime next = CronExpression.parse(cron).next(ZonedDateTime.now());
            return next != null ? next.toString() : "";
        } catch (Exception ignored) {
            return "";
        }
    }

    record CronResolution(String effectiveCron, boolean valid) {
    }
}
