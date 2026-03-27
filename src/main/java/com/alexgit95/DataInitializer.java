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
    }

    private void createSettingIfAbsent(String key, String defaultValue, String description) {
        if (settingsRepository.findBySettingKey(key).isEmpty()) {
            settingsRepository.save(new AppSettings(key, defaultValue, description));
        }
    }
}
