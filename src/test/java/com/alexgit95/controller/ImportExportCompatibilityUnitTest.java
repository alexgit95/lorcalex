package com.alexgit95.controller;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImportExportCompatibilityUnitTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("N fixture exposes version=2 and required top-level keys")
    void currentFixture_hasExpectedContract() throws Exception {
        String json = new ClassPathResource("compat/backup-v2-current-minimal.json")
                .getContentAsString(StandardCharsets.UTF_8);

        Map<String, Object> payload = objectMapper.readValue(json, new TypeReference<>() {});

        assertThat(payload.get("version")).isEqualTo("2");
        assertThat(payload.keySet()).contains(
                "backupDate", "version", "totalEditions", "totalCards", "totalCollection",
                "editions", "cards", "collection", "settings");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> cards = (List<Map<String, Object>>) payload.get("cards");
        assertThat(cards).hasSize(1);
        assertThat(cards.get(0)).containsKeys(
            "marketPrice", "priceCurrency", "priceSource", "lastPriceAt", "lastPriceStatus");
    }

    @Test
    @DisplayName("N-1 fixture remains parsable and versioned")
    void legacyFixture_remainsReadable() throws Exception {
        String json = new ClassPathResource("lorcalex-backup-2026-04-01.json")
                .getContentAsString(StandardCharsets.UTF_8);

        Map<String, Object> payload = objectMapper.readValue(json, new TypeReference<>() {});

        assertThat(payload.get("version")).isEqualTo("2");
        assertThat(payload).containsKeys("editions", "cards", "collection", "settings");
    }
}
