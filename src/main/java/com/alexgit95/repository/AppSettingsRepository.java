package com.alexgit95.repository;

import com.alexgit95.model.AppSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppSettingsRepository extends JpaRepository<AppSettings, String> {
    Optional<AppSettings> findBySettingKey(String settingKey);
}
