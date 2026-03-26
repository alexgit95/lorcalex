package com.alexgit95.controller;

import com.alexgit95.model.AppSettings;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import com.alexgit95.service.ExternalApiService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AppSettingsRepository settingsRepository;
    private final ExternalApiService externalApiService;
    private final UserCollectionRepository userCollectionRepository;
    private final CardRepository cardRepository;
    private final EditionRepository editionRepository;

    public AdminController(AppSettingsRepository settingsRepository,
                           ExternalApiService externalApiService,
                           UserCollectionRepository userCollectionRepository,
                           CardRepository cardRepository,
                           EditionRepository editionRepository) {
        this.settingsRepository = settingsRepository;
        this.externalApiService = externalApiService;
        this.userCollectionRepository = userCollectionRepository;
        this.cardRepository = cardRepository;
        this.editionRepository = editionRepository;
    }

    @GetMapping("/settings")
    public ResponseEntity<List<AppSettings>> getSettings() {
        return ResponseEntity.ok(settingsRepository.findAll());
    }

    @PutMapping("/settings/{key}")
    public ResponseEntity<AppSettings> updateSetting(
            @PathVariable String key,
            @RequestBody Map<String, String> body) {
        AppSettings setting = settingsRepository.findBySettingKey(key)
                .orElse(new AppSettings(key, null, null));
        setting.setSettingValue(body.get("value"));
        return ResponseEntity.ok(settingsRepository.save(setting));
    }

    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncCards() {
        try {
            int count = externalApiService.syncCards();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "syncedCards", count,
                    "message", "Sync completed: " + count + " cards synced"
            ));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Sync failed: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/api-status")
    public ResponseEntity<Map<String, Object>> getApiStatus() {
        return ResponseEntity.ok(Map.of(
                "enabled", externalApiService.isApiEnabled(),
                "url", externalApiService.getApiUrl()
        ));
    }

    @GetMapping("/export")
    public ResponseEntity<Map<String, Object>> exportCollection() {
        List<Map<String, Object>> items = userCollectionRepository.findAll().stream()
                .map(uc -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    entry.put("cardNumber", uc.getCard().getCardNumber());
                    entry.put("editionCode", uc.getCard().getEdition().getCode());
                    entry.put("cardName", uc.getCard().getName());
                    entry.put("rarity", uc.getCard().getRarity() != null ? uc.getCard().getRarity() : "");
                    entry.put("quantity", uc.getQuantity());
                    return entry;
                })
                .collect(Collectors.toList());

        Map<String, Object> export = new LinkedHashMap<>();
        export.put("exportDate", LocalDateTime.now().toString());
        export.put("version", "1");
        export.put("totalEntries", items.size());
        export.put("collection", items);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"lorcalex-export.json\"")
                .body(export);
    }

    @SuppressWarnings("unchecked")
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importCollection(@RequestBody Map<String, Object> body) {
        List<Map<String, Object>> items;
        try {
            items = (List<Map<String, Object>>) body.get("collection");
            if (items == null) throw new IllegalArgumentException("Missing 'collection' key");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Format invalide : clé 'collection' manquante ou incorrecte."
            ));
        }

        int imported = 0;
        int skipped = 0;

        for (Map<String, Object> entry : items) {
            try {
                Object rawNumber = entry.get("cardNumber");
                String editionCode = (String) entry.get("editionCode");
                Object rawQty = entry.get("quantity");

                if (rawNumber == null || editionCode == null || rawQty == null) { skipped++; continue; }

                int cardNumber = ((Number) rawNumber).intValue();
                int quantity   = ((Number) rawQty).intValue();
                if (quantity <= 0) { skipped++; continue; }

                Optional<Edition> editionOpt = editionRepository.findByCode(editionCode);
                if (editionOpt.isEmpty()) { skipped++; continue; }

                Optional<Card> cardOpt = cardRepository.findByCardNumberAndEdition(cardNumber, editionOpt.get());
                if (cardOpt.isEmpty()) { skipped++; continue; }

                Card card = cardOpt.get();
                UserCollection uc = userCollectionRepository.findByCardId(card.getId())
                        .orElse(new UserCollection());
                uc.setCard(card);
                uc.setQuantity(quantity);
                userCollectionRepository.save(uc);
                imported++;
            } catch (Exception e) {
                skipped++;
            }
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "imported", imported,
                "skipped", skipped,
                "message", imported + " carte(s) importée(s), " + skipped + " ignorée(s)."
        ));
    }
}
