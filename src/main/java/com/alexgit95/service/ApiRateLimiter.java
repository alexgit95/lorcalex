package com.alexgit95.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe rate limiter pour l'API RapidAPI.
 * 
 * Contraintes strictes (JAMAIS DÉPASSÉES) :
 * - 100 appels par jour MAXIMUM
 * - 30 appels par minute MAXIMUM
 * 
 * Cette classe garantit que même en cas d'erreur, le budget n'est pas dépassé.
 * Les appels échoués COMPTENT dans le budget (car ils génèrent une requête HTTP).
 */
@Component
public class ApiRateLimiter {

    private static final Logger log = LoggerFactory.getLogger(ApiRateLimiter.class);

    private static final int MAX_DAILY_CALLS = 100;
    private static final int MAX_CALLS_PER_MINUTE = 30;
    private static final long MINUTE_IN_MILLIS = 60_000;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private LocalDateTime dailyResetTime;
    private int dailyCallCount = 0;
    private long lastCallTime = 0;
    private int callsInCurrentMinute = 0;

    public ApiRateLimiter() {
        this.dailyResetTime = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
    }

    /**
     * Vérifie si un appel peut être effectué.
     * 
     * @return true si l'appel peut être fait, false si budget épuisé
     */
    public boolean canMakeCall() {
        lock.readLock().lock();
        try {
            resetDailyIfNeeded();
            
            // Vérifier budget journalier
            if (dailyCallCount >= MAX_DAILY_CALLS) {
                log.warn("API rate limit: daily budget exhausted ({}/{})", dailyCallCount, MAX_DAILY_CALLS);
                return false;
            }

            // Vérifier budget par minute
            long now = System.currentTimeMillis();
            if (now - lastCallTime >= MINUTE_IN_MILLIS) {
                // Nouvelle minute
                callsInCurrentMinute = 0;
            }
            if (callsInCurrentMinute >= MAX_CALLS_PER_MINUTE) {
                log.warn("API rate limit: minute budget exhausted ({}/{})", callsInCurrentMinute, MAX_CALLS_PER_MINUTE);
                return false;
            }

            return true;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Enregistre un appel API effectué.
     * À appeler APRÈS que l'appel HTTP soit envoyé (succès ou erreur).
     * 
     * @throws IllegalStateException si le budget est déjà épuisé
     */
    public void recordCall() {
        lock.writeLock().lock();
        try {
            resetDailyIfNeeded();

            if (dailyCallCount >= MAX_DAILY_CALLS) {
                throw new IllegalStateException(
                    String.format("Cannot record call: daily budget exhausted (%d/%d)", 
                        dailyCallCount, MAX_DAILY_CALLS)
                );
            }

            long now = System.currentTimeMillis();
            
            // Reset minute counter si nouvelle minute
            if (now - lastCallTime >= MINUTE_IN_MILLIS) {
                callsInCurrentMinute = 0;
            }

            if (callsInCurrentMinute >= MAX_CALLS_PER_MINUTE) {
                throw new IllegalStateException(
                    String.format("Cannot record call: minute budget exhausted (%d/%d)",
                        callsInCurrentMinute, MAX_CALLS_PER_MINUTE)
                );
            }

            dailyCallCount++;
            callsInCurrentMinute++;
            lastCallTime = now;

            log.debug("API call recorded: daily {}/{}, minute {}/{}",
                dailyCallCount, MAX_DAILY_CALLS, callsInCurrentMinute, MAX_CALLS_PER_MINUTE);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Obtient le nombre d'appels restants pour aujourd'hui.
     */
    public int getRemainingDailyCalls() {
        lock.readLock().lock();
        try {
            resetDailyIfNeeded();
            return Math.max(0, MAX_DAILY_CALLS - dailyCallCount);
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Obtient le nombre total d'appels utilisés aujourd'hui.
     */
    public int getUsedDailyCalls() {
        lock.readLock().lock();
        try {
            resetDailyIfNeeded();
            return dailyCallCount;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Obtient le statut du rate limiter.
     */
    public RateLimitStatus getStatus() {
        lock.readLock().lock();
        try {
            resetDailyIfNeeded();
            return new RateLimitStatus(
                dailyCallCount,
                MAX_DAILY_CALLS,
                callsInCurrentMinute,
                MAX_CALLS_PER_MINUTE,
                dailyResetTime
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Réinitialise le budget journalier si minuit est passé.
     * À appeler TOUJOURS avant toute opération (lecture ou écriture).
     */
    private void resetDailyIfNeeded() {
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        if (!now.isEqual(dailyResetTime)) {
            dailyResetTime = now;
            dailyCallCount = 0;
            callsInCurrentMinute = 0;
            lastCallTime = 0;
            log.info("API daily budget reset: {} calls available", MAX_DAILY_CALLS);
        }
    }

    /**
     * Statut du rate limiter.
     */
    public static class RateLimitStatus {
        public final int usedDaily;
        public final int maxDaily;
        public final int usedMinute;
        public final int maxMinute;
        public final LocalDateTime resetTime;

        public RateLimitStatus(int usedDaily, int maxDaily, int usedMinute, int maxMinute, LocalDateTime resetTime) {
            this.usedDaily = usedDaily;
            this.maxDaily = maxDaily;
            this.usedMinute = usedMinute;
            this.maxMinute = maxMinute;
            this.resetTime = resetTime;
        }

        public int getRemainingDaily() {
            return Math.max(0, maxDaily - usedDaily);
        }

        @Override
        public String toString() {
            return String.format("RateLimitStatus{daily=%d/%d, minute=%d/%d, reset=%s}",
                usedDaily, maxDaily, usedMinute, maxMinute, resetTime);
        }
    }
}
