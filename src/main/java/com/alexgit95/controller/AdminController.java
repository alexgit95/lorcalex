package com.alexgit95.controller;

import com.alexgit95.model.AppSettings;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import com.alexgit95.service.LorcaJsonService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
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
    private final LorcaJsonService lorcaJsonService;
    private final UserCollectionRepository userCollectionRepository;
    private final CardRepository cardRepository;
    private final EditionRepository editionRepository;

    public AdminController(AppSettingsRepository settingsRepository,
                           LorcaJsonService lorcaJsonService,
                           UserCollectionRepository userCollectionRepository,
                           CardRepository cardRepository,
                           EditionRepository editionRepository) {
        this.settingsRepository = settingsRepository;
        this.lorcaJsonService = lorcaJsonService;
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

    @GetMapping("/progress")
    public ResponseEntity<Map<String, Object>> getProgress() {
        return ResponseEntity.ok(lorcaJsonService.getProgress().toMap());
    }

    /** Asynchronously import cards from a LorcaJson URL. */
    @PostMapping("/sync/url")
    public ResponseEntity<Map<String, Object>> syncFromUrl(@RequestBody(required = false) Map<String, String> body) {
        if (lorcaJsonService.isRunning()) {
            return ResponseEntity.ok(Map.of(
                    "started", false, "running", true,
                    "message", "Une opération est déjà en cours."));
        }
        String url = (body != null) ? body.get("url") : null;
        lorcaJsonService.startSyncFromUrl(url);
        return ResponseEntity.ok(Map.of("started", true, "message", "Synchronisation démarrée."));
    }

    /** Asynchronously import cards from an uploaded allCards.json file. */
    @PostMapping("/sync/file")
    public ResponseEntity<Map<String, Object>> syncFromFile(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "started", false, "message", "Aucun fichier fourni."));
        }
        if (lorcaJsonService.isRunning()) {
            return ResponseEntity.ok(Map.of(
                    "started", false, "running", true,
                    "message", "Une opération est déjà en cours."));
        }
        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            lorcaJsonService.startSyncFromContent(content);
            return ResponseEntity.ok(Map.of("started", true, "message", "Synchronisation démarrée depuis le fichier."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "started", false, "message", e.getMessage()));
        }
    }

    /** Asynchronously compute perceptual hashes for cards without one. */
    @PostMapping("/compute-hashes")
    public ResponseEntity<Map<String, Object>> computeHashes() {
        if (lorcaJsonService.isRunning()) {
            return ResponseEntity.ok(Map.of(
                    "started", false, "running", true,
                    "message", "Une opération est déjà en cours."));
        }
        lorcaJsonService.startComputeHashes();
        return ResponseEntity.ok(Map.of("started", true, "message", "Calcul des empreintes démarré."));
    }

    @GetMapping("/lorcajson-url")
    public ResponseEntity<Map<String, Object>> getLorcaJsonUrl() {
        return ResponseEntity.ok(Map.of("url", lorcaJsonService.getLorcaJsonUrl()));
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

    @PostMapping("/import/companion")
    public ResponseEntity<Map<String, Object>> importCompanionCollection(
            @RequestParam("file") MultipartFile file,
            @RequestParam(name = "merge", defaultValue = "true") boolean mergeMode) {
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Aucun fichier fourni."
            ));
        }

        if (lorcaJsonService.isRunning()) {
            return ResponseEntity.ok(Map.of(
                    "started", false,
                    "running", true,
                    "message", "Une opération est déjà en cours."
            ));
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            lorcaJsonService.startCompanionImportFromContent(content, mergeMode);
            return ResponseEntity.ok(Map.of(
                    "started", true,
                    "message", "Import Companion démarré en mode " + (mergeMode ? "fusion" : "remplacement") + "."
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "started", false,
                    "message", "Impossible de lire le fichier Companion : " + e.getMessage()
            ));
        }
    }
}
