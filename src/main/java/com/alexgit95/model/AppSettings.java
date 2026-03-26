package com.alexgit95.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "app_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppSettings {

    @Id
    @Column(name = "setting_key")
    private String settingKey;

    @Column(columnDefinition = "TEXT")
    private String settingValue;

    private String description;
}
