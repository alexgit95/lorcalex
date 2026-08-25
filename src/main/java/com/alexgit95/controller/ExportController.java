package com.alexgit95.controller;

import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Endpoint public d'export de la collection, accessible via clé API ({@code ?apiKey=…}).
 * <p>
 * Retourne exactement le même payload que {@code GET /api/admin/backup}.
 * Aucun JWT requis : la validation est effectuée en amont par {@code ApiKeyAuthFilter}.
 */
@RestController
@RequestMapping("/api/export")
public class ExportController {

    private final EditionRepository editionRepository;
    private final CardRepository cardRepository;
    private final UserCollectionRepository userCollectionRepository;
    private final AppSettingsRepository settingsRepository;

    public ExportController(EditionRepository editionRepository,
                            CardRepository cardRepository,
                            UserCollectionRepository userCollectionRepository,
                            AppSettingsRepository settingsRepository) {
        this.editionRepository = editionRepository;
        this.cardRepository = cardRepository;
        this.userCollectionRepository = userCollectionRepository;
        this.settingsRepository = settingsRepository;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> export() {
        List<Map<String, Object>> editionsData = editionRepository.findAll().stream()
                .map(e -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", e.getId());
                    m.put("code", e.getCode());
                    m.put("name", e.getName());
                    m.put("totalCards", e.getTotalCards());
                    m.put("releaseDate", e.getReleaseDate());
                    m.put("logoUrl", e.getLogoUrl());
                    m.put("setNumber", e.getSetNumber());
                    return m;
                }).collect(Collectors.toList());

        List<Map<String, Object>> cardsData = cardRepository.findAll().stream()
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("externalId", c.getExternalId());
                    m.put("cardNumber", c.getCardNumber());
                    m.put("editionCode", c.getEdition() != null ? c.getEdition().getCode() : null);
                    m.put("name", c.getName());
                    m.put("rarity", c.getRarity());
                    m.put("cost", c.getCost());
                    m.put("inkColor", c.getInkColor());
                    m.put("type", c.getType());
                    m.put("subtypes", c.getSubtypes());
                    m.put("bodyText", c.getBodyText());
                    m.put("flavorText", c.getFlavorText());
                    m.put("imageUrl", c.getImageUrl());
                    m.put("thumbnailUrl", c.getThumbnailUrl());
                    m.put("artist", c.getArtist());
                    m.put("inkable", c.getInkable());
                    m.put("imageHash", c.getImageHash());
                    return m;
                }).collect(Collectors.toList());

        List<Map<String, Object>> collectionData = userCollectionRepository.findAllWithCard().stream()
                .map(uc -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("externalId", uc.getCard().getExternalId());
                    m.put("cardNumber", uc.getCard().getCardNumber());
                    m.put("editionCode", uc.getCard().getEdition() != null ? uc.getCard().getEdition().getCode() : null);
                    m.put("quantity", uc.getQuantity());
                    m.put("foilQuantity", uc.getFoilQuantity());
                    m.put("foil", uc.getFoil() != null && uc.getFoil());
                    m.put("firstAddedAt", uc.getFirstAddedAt() != null ? uc.getFirstAddedAt().toString() : null);
                    m.put("lastAddedAt", uc.getLastAddedAt() != null ? uc.getLastAddedAt().toString() : null);
                    return m;
                }).collect(Collectors.toList());

        List<Map<String, Object>> settingsData = settingsRepository.findAll().stream()
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("key", s.getSettingKey());
                    m.put("value", s.getSettingValue());
                    m.put("description", s.getDescription());
                    return m;
                }).collect(Collectors.toList());

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportDate", LocalDateTime.now().toString());
        export.put("version", "2");
        export.put("totalEditions", editionsData.size());
        export.put("totalCards", cardsData.size());
        export.put("totalCollection", collectionData.size());
        export.put("editions", editionsData);
        export.put("cards", cardsData);
        export.put("collection", collectionData);
        export.put("settings", settingsData);

        return ResponseEntity.ok(export);
    }
}
