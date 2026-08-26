package com.alexgit95;

import com.alexgit95.model.AppSettings;
import com.alexgit95.model.User;
import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final AppSettingsRepository settingsRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username:admin}")
    private String adminUsername;

    @Value("${app.admin.password:admin}")
    private String adminPassword;

    public DataInitializer(UserRepository userRepository,
                           AppSettingsRepository settingsRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.settingsRepository = settingsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        initUser();
        initSettings();
    }

    private void initUser() {
        if (!userRepository.existsByUsername(adminUsername)) {
            User admin = new User();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(adminPassword));
            admin.setRole("ADMIN");
            userRepository.save(admin);
            log.info("Created admin user: {}", adminUsername);
        }
    }

    private void initSettings() {
        createSettingIfAbsent("lorcajson_url", "https://lorcanajson.org/files/current/fr/allCards.json",
                "URL du fichier LorcaJson (allCards.json) pour l'import des cartes");
        createSettingIfAbsent("pricing_sync_enabled", "true",
            "Active ou désactive la synchronisation pricing en tâche de fond");
        createSettingIfAbsent("pricing_daily_budget", "100",
            "Compat: budget journalier pricing historique (utiliser pricing_daily_hard_limit)");
        createSettingIfAbsent("pricing_daily_hard_limit", "100",
            "Limite quotidienne stricte d'appels provider (hard cap, max 100)");
        createSettingIfAbsent("pricing_daily_safety_margin", "5",
            "Marge de sécurité soustraite au hard cap pour le budget opérationnel");
        createSettingIfAbsent("pricing_minute_limit", "30",
            "Limite stricte d'appels provider par minute (max 30)");
        createSettingIfAbsent("pricing_used_attempts", "0",
            "Nombre d'appels provider pricing déjà consommés pour la date d'usage");
        createSettingIfAbsent("pricing_usage_date", "",
            "Date d'usage (yyyy-MM-dd) du compteur d'appels pricing");
        createSettingIfAbsent("pricing_schedule_cron", "0 0 2 * * *",
            "Expression cron de la synchronisation pricing quotidienne");
        createSettingIfAbsent("pricing_last_scheduled_run_date", "",
            "Date du dernier run pricing planifié (yyyy-MM-dd)");
        createSettingIfAbsent("pricing_provider", "rapidapi-lorcana-prices",
            "Source fournisseur de prix Lorcana");
        createSettingIfAbsent("pricing_provider_host", "lorcana-api-by-tcggo.p.rapidapi.com",
            "Host RapidAPI du fournisseur Lorcana Prices");
        createSettingIfAbsent("pricing_provider_path", "/cards/search",
            "Compat: ancien endpoint de recherche de prix carte à carte");
        createSettingIfAbsent("pricing_provider_episodes_path", "/episodes",
            "Endpoint provider paginé des sets/episodes");
        createSettingIfAbsent("pricing_provider_episode_cards_path_template", "/episodes/{episodeId}/cards",
            "Template endpoint provider paginé des cartes d'un episode");
        createSettingIfAbsent("pricing_provider_api_key", "",
            "Clé API RapidAPI utilisée pour les tentatives pricing");
        createSettingIfAbsent("pricing_provider_currency", "EUR",
            "Devise cible de valorisation pour les cartes");
        createSettingIfAbsent("pricing_cursor_phase", "EPISODES",
            "Phase courante du curseur pricing");
        createSettingIfAbsent("pricing_cursor_episode_page", "1",
            "Page episodes courante du curseur pricing");
        createSettingIfAbsent("pricing_cursor_episode_id", "0",
            "Episode courant du curseur pricing");
        createSettingIfAbsent("pricing_cursor_episode_cards_page", "1",
            "Page cartes episode courante du curseur pricing");
        createSettingIfAbsent("pricing_last_stop_reason", "",
            "Derniere raison d'arret de synchronisation pricing");
    }

    private void createSettingIfAbsent(String key, String defaultValue, String description) {
        if (settingsRepository.findBySettingKey(key).isEmpty()) {
            settingsRepository.save(new AppSettings(key, defaultValue, description));
        }
    }
}
