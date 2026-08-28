package com.alexgit95.controller;

import com.alexgit95.model.AppSettings;
import com.alexgit95.model.Card;
import com.alexgit95.model.CollectionValueSnapshot;
import com.alexgit95.model.Edition;
import com.alexgit95.model.EditionValueSnapshot;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.CollectionValueSnapshotRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.EditionValueSnapshotRepository;
import com.alexgit95.repository.UserCollectionRepository;
import com.alexgit95.service.LorcaJsonService;
import com.alexgit95.service.PricingScheduleService;
import com.alexgit95.service.PricingSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
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
    private final PricingSyncService pricingSyncService;
    private final PricingScheduleService pricingScheduleService;
    private final CollectionValueSnapshotRepository collectionValueSnapshotRepository;
    private final EditionValueSnapshotRepository editionValueSnapshotRepository;

    public AdminController(AppSettingsRepository settingsRepository,
                           LorcaJsonService lorcaJsonService,
                           UserCollectionRepository userCollectionRepository,
                           CardRepository cardRepository,
                           EditionRepository editionRepository,
                           PricingSyncService pricingSyncService,
                           PricingScheduleService pricingScheduleService,
                           CollectionValueSnapshotRepository collectionValueSnapshotRepository,
                           EditionValueSnapshotRepository editionValueSnapshotRepository) {
        this.settingsRepository = settingsRepository;
        this.lorcaJsonService = lorcaJsonService;
        this.userCollectionRepository = userCollectionRepository;
        this.cardRepository = cardRepository;
        this.editionRepository = editionRepository;
        this.pricingSyncService = pricingSyncService;
        this.pricingScheduleService = pricingScheduleService;
        this.collectionValueSnapshotRepository = collectionValueSnapshotRepository;
        this.editionValueSnapshotRepository = editionValueSnapshotRepository;
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
        AppSettings saved = settingsRepository.save(setting);
        pricingScheduleService.onSettingUpdated(key);
        return ResponseEntity.ok(saved);
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

    @GetMapping("/pricing/status")
    public ResponseEntity<Map<String, Object>> getPricingStatus() {
        Map<String, Object> status = new LinkedHashMap<>(pricingSyncService.getStatus());
        status.putAll(pricingScheduleService.getScheduleStatus());
        return ResponseEntity.ok(status);
    }

    @PostMapping("/pricing/run")
    public ResponseEntity<Map<String, Object>> runPricingSync(
            @RequestBody(required = false) Map<String, Object> body) {
        Integer maxAttempts = null;
        if (body != null && body.get("maxAttempts") instanceof Number n) {
            maxAttempts = n.intValue();
        }
        return ResponseEntity.ok(pricingSyncService.runSync("manual", maxAttempts));
    }

    /**
     * Temporary tool: simulate a provider episode-cards API response by applying
     * pricing from a manually pasted JSON payload, without calling the provider.
     */
    @PostMapping("/pricing/simulate-import")
    public ResponseEntity<Map<String, Object>> simulatePricingImport(@RequestBody Map<String, String> body) {
        String json = body != null ? body.get("json") : null;
        return ResponseEntity.ok(pricingSyncService.applyManualPricingImport(json));
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

    // ─────────────────────────────────────────────────────────────────────────
    // BACKUP COMPLET (éditions + cartes + collection + settings)
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping("/backup")
    public ResponseEntity<Map<String, Object>> fullBackup() {
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
                    m.put("marketPrice", c.getMarketPrice());
                    m.put("priceCurrency", c.getPriceCurrency());
                    m.put("priceSource", c.getPriceSource());
                    m.put("lastPriceAt", c.getLastPriceAt() != null ? c.getLastPriceAt().toString() : null);
                    m.put("lastPriceStatus", c.getLastPriceStatus());
                    m.put("wanted", Boolean.TRUE.equals(c.getWanted()));
                    return m;
                }).collect(Collectors.toList());

        List<Map<String, Object>> collectionData = userCollectionRepository.findAllWithCard().stream()
                .map(uc -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("externalId", uc.getCard().getExternalId());
                    m.put("cardNumber", uc.getCard().getCardNumber());
                    m.put("editionCode", uc.getCard().getEdition() != null ? uc.getCard().getEdition().getCode() : null);
                    m.put("quantity", uc.getQuantity());
                    m.put("foilQuantity", uc.getFoilQuantity() != null ? uc.getFoilQuantity() : 0);
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

        List<Map<String, Object>> collectionSnapshotsData = collectionValueSnapshotRepository.findAll().stream()
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("recordedAt", s.getRecordedAt() != null ? s.getRecordedAt().toString() : null);
                    m.put("totalCollectionValueEur", s.getTotalCollectionValueEur());
                    m.put("currency", s.getCurrency());
                    m.put("source", s.getSource());
                    return m;
                }).collect(Collectors.toList());

        List<Map<String, Object>> editionSnapshotsData = editionValueSnapshotRepository.findAll().stream()
                .map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("recordedAt", s.getRecordedAt() != null ? s.getRecordedAt().toString() : null);
                    m.put("editionId", s.getEditionId());
                    m.put("editionCode", s.getEditionCode());
                    m.put("editionName", s.getEditionName());
                    m.put("totalValueEur", s.getTotalValueEur());
                    return m;
                }).collect(Collectors.toList());

        Map<String, Object> valueHistory = new LinkedHashMap<>();
        valueHistory.put("collectionSnapshots", collectionSnapshotsData);
        valueHistory.put("editionSnapshots", editionSnapshotsData);

        Map<String, Object> backup = new LinkedHashMap<>();
        backup.put("backupDate", LocalDateTime.now().toString());
        backup.put("version", "2");
        backup.put("totalEditions", editionsData.size());
        backup.put("totalCards", cardsData.size());
        backup.put("totalCollection", collectionData.size());
        backup.put("editions", editionsData);
        backup.put("cards", cardsData);
        backup.put("collection", collectionData);
        backup.put("settings", settingsData);
        backup.put("valueHistory", valueHistory);

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"lorcalex-backup.json\"")
                .body(backup);
    }

    @SuppressWarnings("unchecked")
    @Transactional
    @PostMapping("/restore")
    public ResponseEntity<Map<String, Object>> fullRestore(@RequestBody Map<String, Object> body) {
        List<Map<String, Object>> editionsRaw    = (List<Map<String, Object>>) body.getOrDefault("editions", List.of());
        List<Map<String, Object>> cardsRaw       = (List<Map<String, Object>>) body.getOrDefault("cards",    List.of());
        List<Map<String, Object>> collectionRaw  = (List<Map<String, Object>>) body.getOrDefault("collection", List.of());
        List<Map<String, Object>> settingsRaw    = (List<Map<String, Object>>) body.getOrDefault("settings", List.of());
        Map<String, Object> valueHistoryRaw = (Map<String, Object>) body.getOrDefault("valueHistory", Map.of());
        List<Map<String, Object>> collectionSnapshotsRaw =
                (List<Map<String, Object>>) valueHistoryRaw.getOrDefault("collectionSnapshots", List.of());
        List<Map<String, Object>> editionSnapshotsRaw =
                (List<Map<String, Object>>) valueHistoryRaw.getOrDefault("editionSnapshots", List.of());

        // 1. Supprimer dans l'ordre des dépendances
        userCollectionRepository.deleteAllInBatch();
        cardRepository.deleteAllInBatch();
        editionRepository.deleteAllInBatch();
        settingsRepository.deleteAllInBatch();
        collectionValueSnapshotRepository.deleteAllInBatch();
        editionValueSnapshotRepository.deleteAllInBatch();

        // 2. Restaurer les éditions — conserver le mapping ancien ID → nouvelle Edition
        Map<Long, Edition> oldIdToNewEdition = new LinkedHashMap<>();
        Map<String, Edition> editionByCode   = new LinkedHashMap<>();

        for (Map<String, Object> e : editionsRaw) {
            Edition edition = new Edition();
            edition.setCode((String) e.get("code"));
            edition.setName((String) e.get("name"));
            if (e.get("totalCards") != null)  edition.setTotalCards(((Number) e.get("totalCards")).intValue());
            if (e.get("setNumber")  != null)  edition.setSetNumber(((Number) e.get("setNumber")).intValue());
            edition.setReleaseDate((String) e.get("releaseDate"));
            edition.setLogoUrl((String) e.get("logoUrl"));
            edition = editionRepository.save(edition);
            if (e.get("id") != null) oldIdToNewEdition.put(((Number) e.get("id")).longValue(), edition);
            if (edition.getCode() != null) editionByCode.put(edition.getCode(), edition);
        }

        // 3. Restaurer les cartes
        Map<String, Card> cardByExternalId     = new LinkedHashMap<>();
        Map<String, Card> cardByNumberAndCode  = new LinkedHashMap<>();

        for (Map<String, Object> c : cardsRaw) {
            String edCode = (String) c.get("editionCode");
            Edition edition = edCode != null ? editionByCode.get(edCode) : null;
            if (edition == null) continue;

            Card card = new Card();
            card.setName((String) c.get("name"));
            card.setEdition(edition);
            card.setExternalId((String) c.get("externalId"));
            if (c.get("cardNumber") != null) card.setCardNumber(((Number) c.get("cardNumber")).intValue());
            card.setRarity((String) c.get("rarity"));
            if (c.get("cost")      != null) card.setCost(((Number) c.get("cost")).intValue());
            card.setInkColor((String) c.get("inkColor"));
            card.setType((String) c.get("type"));
            card.setSubtypes((String) c.get("subtypes"));
            card.setBodyText((String) c.get("bodyText"));
            card.setFlavorText((String) c.get("flavorText"));
            card.setImageUrl((String) c.get("imageUrl"));
            card.setThumbnailUrl((String) c.get("thumbnailUrl"));
            card.setArtist((String) c.get("artist"));
            if (c.get("inkable")   != null) card.setInkable((Boolean) c.get("inkable"));
            if (c.get("imageHash") != null) card.setImageHash(((Number) c.get("imageHash")).longValue());
            card.setMarketPrice(toBigDecimal(c.get("marketPrice")));
            card.setPriceCurrency((String) c.get("priceCurrency"));
            card.setPriceSource((String) c.get("priceSource"));
            card.setLastPriceAt(toLocalDateTime(c.get("lastPriceAt")));
            card.setLastPriceStatus((String) c.get("lastPriceStatus"));
            card.setWanted(c.get("wanted") instanceof Boolean b && b);
            card = cardRepository.save(card);
            if (card.getExternalId() != null) cardByExternalId.put(card.getExternalId(), card);
            if (card.getCardNumber() != null && edCode != null)
                cardByNumberAndCode.put(card.getCardNumber() + ":" + edCode, card);
        }

        // 4. Restaurer la collection
        int collectionRestored = 0;
        for (Map<String, Object> entry : collectionRaw) {
            String exId   = (String) entry.get("externalId");
            String edCode = (String) entry.get("editionCode");
            Object rawNum = entry.get("cardNumber");
            Object rawQty = entry.get("quantity");
            if (rawQty == null) continue;

            Card card = null;
            if (exId != null) card = cardByExternalId.get(exId);
            if (card == null && rawNum != null && edCode != null)
                card = cardByNumberAndCode.get(((Number) rawNum).intValue() + ":" + edCode);
            if (card == null) continue;

            UserCollection uc = new UserCollection();
            uc.setCard(card);
            uc.setQuantity(((Number) rawQty).intValue());
            if (entry.get("foilQuantity") instanceof Number n) uc.setFoilQuantity(n.intValue());
            if (entry.get("foil") instanceof Boolean b) uc.setFoil(b);
            // Restore original dates so @PrePersist null-checks will not overwrite them
            if (entry.get("firstAddedAt") instanceof String s) {
                try { uc.setFirstAddedAt(java.time.LocalDateTime.parse(s)); } catch (Exception ignored) {}
            }
            if (entry.get("lastAddedAt") instanceof String s) {
                try { uc.setLastAddedAt(java.time.LocalDateTime.parse(s)); } catch (Exception ignored) {}
            }
            userCollectionRepository.save(uc);
            collectionRestored++;
        }

        // 5. Restaurer les settings et remap les IDs d'éditions dans stats_enabled_sets
        for (Map<String, Object> s : settingsRaw) {
            String key   = (String) s.get("key");
            String value = (String) s.get("value");
            if (key == null) continue;
            if ("stats_enabled_sets".equals(key) && value != null && !oldIdToNewEdition.isEmpty()) {
                value = Arrays.stream(value.split(","))
                        .map(String::trim).filter(p -> !p.isEmpty())
                        .map(p -> {
                            try {
                                Edition newEd = oldIdToNewEdition.get(Long.parseLong(p));
                                return newEd != null ? newEd.getId().toString() : p;
                            } catch (NumberFormatException ex) { return p; }
                        })
                        .collect(Collectors.joining(","));
            }
            settingsRepository.save(new AppSettings(key, value, (String) s.get("description")));
        }

        // 6. Restaurer l'historique de valeur (curseur collection tel quel, éditions remappées vers les nouveaux ids)
        for (Map<String, Object> s : collectionSnapshotsRaw) {
            CollectionValueSnapshot snap = new CollectionValueSnapshot();
            snap.setRecordedAt(toLocalDateTime(s.get("recordedAt")));
            snap.setTotalCollectionValueEur(toBigDecimal(s.get("totalCollectionValueEur")));
            if (s.get("currency") != null) snap.setCurrency((String) s.get("currency"));
            if (s.get("source") != null) snap.setSource((String) s.get("source"));
            collectionValueSnapshotRepository.save(snap);
        }
        for (Map<String, Object> s : editionSnapshotsRaw) {
            Object rawEditionId = s.get("editionId");
            if (!(rawEditionId instanceof Number)) continue;
            Edition newEdition = oldIdToNewEdition.get(((Number) rawEditionId).longValue());
            if (newEdition == null) continue;
            EditionValueSnapshot snap = new EditionValueSnapshot();
            snap.setRecordedAt(toLocalDateTime(s.get("recordedAt")));
            snap.setEditionId(newEdition.getId());
            snap.setEditionCode((String) s.get("editionCode"));
            snap.setEditionName((String) s.get("editionName"));
            snap.setTotalValueEur(toBigDecimal(s.get("totalValueEur")));
            editionValueSnapshotRepository.save(snap);
        }

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", editionsRaw.size() + " édition(s), " + cardsRaw.size() + " carte(s), "
                        + collectionRestored + " entrée(s) de collection, "
                        + settingsRaw.size() + " paramètre(s) restaurés."
        ));
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return new BigDecimal(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private static LocalDateTime toLocalDateTime(Object value) {
        if (value instanceof String text && !text.isBlank()) {
            try {
                return LocalDateTime.parse(text);
            } catch (Exception ignored) {
                return null;
            }
        }
        return null;
    }
}
