package com.alexgit95.service;

import com.alexgit95.model.AppSettings;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Service
public class LorcaJsonService {

    private static final Logger log = LoggerFactory.getLogger(LorcaJsonService.class);
    private static final String KEY_LORCAJSON_URL = "lorcajson_url";
    private static final String DEFAULT_URL = "https://lorcanajson.org/files/current/fr/allCards.json";

    private final AppSettingsRepository settingsRepository;
    private final CardRepository cardRepository;
    private final EditionRepository editionRepository;
    private final UserCollectionRepository userCollectionRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    private final AtomicReference<ProgressInfo> progress =
            new AtomicReference<>(ProgressInfo.idle());

    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "lorca-sync");
        t.setDaemon(true);
        return t;
    });

    public LorcaJsonService(AppSettingsRepository settingsRepository,
                            CardRepository cardRepository,
                            EditionRepository editionRepository,
                            UserCollectionRepository userCollectionRepository,
                            WebClient.Builder webClientBuilder,
                            ObjectMapper objectMapper) {
        this.settingsRepository = settingsRepository;
        this.cardRepository = cardRepository;
        this.editionRepository = editionRepository;
        this.userCollectionRepository = userCollectionRepository;
        this.webClientBuilder = webClientBuilder;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    // ─── Progress tracking ────────────────────────────────────────────────────

    public static class ProgressInfo {
        public final String phase;
        public final int current;
        public final int total;
        public final String message;
        public final boolean running;
        public final boolean error;

        public ProgressInfo(String phase, int current, int total, String message,
                            boolean running, boolean error) {
            this.phase = phase;
            this.current = current;
            this.total = total;
            this.message = message;
            this.running = running;
            this.error = error;
        }

        public int percent() {
            return total > 0 ? Math.min(100, current * 100 / total) : (running ? 0 : 100);
        }

        public Map<String, Object> toMap() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("phase", phase);
            m.put("current", current);
            m.put("total", total);
            m.put("percent", percent());
            m.put("message", message);
            m.put("running", running);
            m.put("error", error);
            return m;
        }

        static ProgressInfo idle() {
            return new ProgressInfo("idle", 0, 0, "En attente", false, false);
        }
    }

    public ProgressInfo getProgress() {
        return progress.get();
    }

    public boolean isRunning() {
        return progress.get().running;
    }

    // ─── Public async API ─────────────────────────────────────────────────────

    /** Asynchronously fetch and sync cards from a LorcaJson URL. */
    public void startSyncFromUrl(String url) {
        if (isRunning()) return;
        progress.set(new ProgressInfo("downloading", 0, 0, "Téléchargement du fichier…", true, false));
        executor.submit(() -> {
            try {
                String targetUrl = (url != null && !url.isBlank()) ? url : getLorcaJsonUrl();
                log.info("Syncing from URL: {}", targetUrl);
                String json = webClientBuilder.build().get().uri(targetUrl)
                        .retrieve().bodyToMono(String.class).block();
                if (json == null || json.isBlank())
                    throw new RuntimeException("Réponse vide depuis l'URL.");
                doSync(json);
            } catch (Exception e) {
                log.error("Sync from URL failed: {}", e.getMessage());
                progress.set(new ProgressInfo("error", 0, 0,
                        "Erreur : " + e.getMessage(), false, true));
            }
        });
    }

    /** Asynchronously sync cards from a pre-loaded JSON string. */
    public void startSyncFromContent(String jsonContent) {
        if (isRunning()) return;
        progress.set(new ProgressInfo("parsing", 0, 0, "Analyse du fichier JSON…", true, false));
        executor.submit(() -> {
            try {
                doSync(jsonContent);
            } catch (Exception e) {
                log.error("Sync from content failed: {}", e.getMessage());
                progress.set(new ProgressInfo("error", 0, 0,
                        "Erreur : " + e.getMessage(), false, true));
            }
        });
    }

    /** Asynchronously compute perceptual hashes for cards that don't have one yet. */
    public void startComputeHashes() {
        if (isRunning()) return;
        progress.set(new ProgressInfo("hashing", 0, 0,
                "Démarrage du calcul des empreintes…", true, false));
        executor.submit(() -> {
            try {
                doComputeHashes();
            } catch (Exception e) {
                log.error("Hash computation failed: {}", e.getMessage());
                progress.set(new ProgressInfo("error", 0, 0,
                        "Erreur : " + e.getMessage(), false, true));
            }
        });
    }

    /** Asynchronously import collection from Lorcana Companion export file. */
    public void startCompanionImportFromContent(String jsonContent, boolean mergeMode) {
        if (isRunning()) return;
        progress.set(new ProgressInfo("companion_parsing", 0, 0,
                "Analyse de l'export Companion…", true, false));
        executor.submit(() -> {
            try {
                doCompanionImport(jsonContent, mergeMode);
            } catch (Exception e) {
                log.error("Companion import failed: {}", e.getMessage());
                progress.set(new ProgressInfo("error", 0, 0,
                        "Erreur : " + e.getMessage(), false, true));
            }
        });
    }

    // ─── Internal sync ────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void doSync(String jsonContent) throws Exception {
        progress.set(new ProgressInfo("parsing", 0, 0, "Analyse du fichier JSON…", true, false));
        Map<String, Object> data;
        try {
            data = objectMapper.readValue(jsonContent, objectMapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, Object.class));
        } catch (Exception e) {
            throw new RuntimeException("Format JSON invalide : " + e.getMessage());
        }

        Map<String, Object> setsMap = (Map<String, Object>) data.get("sets");
        List<Map<String, Object>> cards = (List<Map<String, Object>>) data.get("cards");
        if (setsMap == null || cards == null)
            throw new RuntimeException("Format LorcaJson invalide : clés 'sets' ou 'cards' manquantes.");

        int total = cards.size();
        int synced = 0;
        log.info("Processing {} cards from LorcaJson", total);
        progress.set(new ProgressInfo("sync", 0, total,
                "Synchronisation (0/" + total + ")…", true, false));

        for (int i = 0; i < total; i++) {
            try {
                processCard(cards.get(i), setsMap);
                synced++;
            } catch (Exception e) {
                log.warn("Failed to process card {}: {}", cards.get(i).get("fullName"), e.getMessage());
            }
            progress.set(new ProgressInfo("sync", i + 1, total,
                    "Synchronisation (" + (i + 1) + "/" + total + ")…", true, false));
        }

        editionRepository.findAll().forEach(edition -> {
            long count = cardRepository.countByEdition(edition);
            edition.setTotalCards((int) count);
            editionRepository.save(edition);
        });

        int finalSynced = synced;
        log.info("Synced {} cards", finalSynced);
        progress.set(new ProgressInfo("done", total, total,
                finalSynced + " carte(s) synchronisée(s). Vous pouvez maintenant lancer l'Étape 2 pour calculer les empreintes.",
                false, false));
    }

    // ─── Internal hash computation ────────────────────────────────────────────

    private void doComputeHashes() {
        List<Card> cardsNeedingHash = cardRepository.findByImageHashIsNull();
        int total = cardsNeedingHash.size();
        log.info("Computing hashes for {} cards", total);

        if (total == 0) {
            progress.set(new ProgressInfo("done", 0, 0,
                    "Toutes les empreintes sont déjà calculées.", false, false));
            return;
        }

        progress.set(new ProgressInfo("hashing", 0, total,
                "Empreintes (0/" + total + ")…", true, false));

        int done = 0;
        int failed = 0;
        for (Card card : cardsNeedingHash) {
            try {
                String hashUrl = card.getThumbnailUrl() != null
                        ? card.getThumbnailUrl() : card.getImageUrl();
                if (hashUrl != null) {
                    Long hash = computeAverageHash(hashUrl);
                    if (hash != null) {
                        card.setImageHash(hash);
                        cardRepository.save(card);
                        done++;
                    } else {
                        failed++;
                    }
                } else {
                    failed++;
                }
            } catch (Exception e) {
                failed++;
                log.warn("Hash failed for card id={}: {}", card.getId(), e.getMessage());
            }
            int processed = done + failed;
            progress.set(new ProgressInfo("hashing", processed, total,
                    "Empreintes : " + processed + "/" + total
                            + (failed > 0 ? " (" + failed + " échecs)" : "") + "…",
                    true, false));
        }

        String msg = done + " empreinte(s) calculée(s)"
                + (failed > 0 ? ", " + failed + " échec(s)" : "") + ".";
        log.info("Hash computation complete: {}", msg);
        progress.set(new ProgressInfo("done", total, total, msg, false, false));
    }

    @SuppressWarnings("unchecked")
    private void doCompanionImport(String jsonContent, boolean mergeMode) throws Exception {
        progress.set(new ProgressInfo("companion_parsing", 0, 0,
                "Analyse de l'export Companion…", true, false));

        Map<String, Object> payload;
        try {
            payload = objectMapper.readValue(jsonContent, objectMapper.getTypeFactory()
                    .constructMapType(Map.class, String.class, Object.class));
        } catch (Exception e) {
            throw new RuntimeException("Format JSON invalide : " + e.getMessage());
        }

        Object rawEntries = payload.get("OwnedCardQuantitiesV2");
        if (!(rawEntries instanceof List<?> entries)) {
            throw new RuntimeException("Format Companion invalide : clé 'OwnedCardQuantitiesV2' absente.");
        }

        // Separate Regular and Foiled quantities
        Map<String, Integer> regularQtiesByExternalId = new LinkedHashMap<>();
        Map<String, Integer> foilQtiesByExternalId = new LinkedHashMap<>();
        int skippedInvalidRows = 0;
        for (Object raw : entries) {
            if (!(raw instanceof Map<?, ?> row)) {
                skippedInvalidRows++;
                continue;
            }
            Object idRaw = row.get("Id");
            Object quantityRaw = row.get("Quantity");
            Object typeRaw = row.get("Type");
            if (!(idRaw instanceof Number) || !(quantityRaw instanceof Number)) {
                skippedInvalidRows++;
                continue;
            }

            int quantity = ((Number) quantityRaw).intValue();
            if (quantity <= 0) continue;

            String externalId = String.valueOf(((Number) idRaw).intValue());
            String type = typeRaw != null ? typeRaw.toString() : "Regular";
            
            // Accumulate by type
            if ("Foiled".equalsIgnoreCase(type)) {
                foilQtiesByExternalId.merge(externalId, quantity, Integer::sum);
            } else {
                regularQtiesByExternalId.merge(externalId, quantity, Integer::sum);
            }
        }

        // Merge all external IDs (both Regular and Foiled)
        Map<String, Integer> allExternalIds = new LinkedHashMap<>(regularQtiesByExternalId);
        foilQtiesByExternalId.forEach((id, qty) -> allExternalIds.putIfAbsent(id, 0));

        if (allExternalIds.isEmpty()) {
            throw new RuntimeException("Aucune carte exploitable trouvée dans 'OwnedCardQuantitiesV2'.");
        }

        List<Card> matchedCards = cardRepository.findByExternalIdIn(allExternalIds.keySet());
        Map<String, Card> cardsByExternalId = matchedCards.stream()
                .collect(Collectors.toMap(Card::getExternalId, c -> c, (a, b) -> a));

        int total = allExternalIds.size();
        int processed = 0;
        int imported = 0;
        int skippedUnknown = 0;
        progress.set(new ProgressInfo("companion_import", 0, total,
                "Import Companion (0/" + total + ")…", true, false));

        for (String externalId : allExternalIds.keySet()) {
            Card card = cardsByExternalId.get(externalId);
            if (card == null) {
                skippedUnknown++;
            } else {
                UserCollection uc = userCollectionRepository.findByCardId(card.getId())
                        .orElseGet(UserCollection::new);
                uc.setCard(card);
                
                int incomingRegularQty = regularQtiesByExternalId.getOrDefault(externalId, 0);
                int incomingFoilQty = foilQtiesByExternalId.getOrDefault(externalId, 0);
                
                // Calculate new quantities based on merge mode
                int newRegularQty, newFoilQty;
                if (mergeMode) {
                    if (uc.getId() == null) {
                        // New entry
                        newRegularQty = incomingRegularQty;
                        newFoilQty = incomingFoilQty;
                    } else {
                        // Existing entry: add to what's there
                        newRegularQty = (uc.getQuantity() != null ? uc.getQuantity() : 0) + incomingRegularQty;
                        newFoilQty = (uc.getFoilQuantity() != null ? uc.getFoilQuantity() : 0) + incomingFoilQty;
                    }
                } else {
                    // Replace mode: completely replace with incoming quantities
                    newRegularQty = incomingRegularQty;
                    newFoilQty = incomingFoilQty;
                }
                
                uc.setQuantity(newRegularQty);
                uc.setFoilQuantity(newFoilQty);
                userCollectionRepository.save(uc);
                imported++;
            }

            processed++;
            progress.set(new ProgressInfo("companion_import", processed, total,
                    "Import Companion (" + processed + "/" + total + ")…", true, false));
        }

        String modeLabel = mergeMode ? "fusion" : "remplacement";
        String message = imported + " carte(s) importée(s) depuis Companion en mode " + modeLabel
                + ", " + skippedUnknown + " non trouvée(s)"
                + (skippedInvalidRows > 0 ? ", " + skippedInvalidRows + " ligne(s) invalide(s)" : "") + ".";
        if (imported == 0) {
            message += " Vérifiez que le catalogue a été re-synchronisé avec la dernière version LorcaJson.";
        }

        progress.set(new ProgressInfo("done", total, total, message, false, false));
    }

    // ─── Card entity processing ───────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void processCard(Map<String, Object> c, Map<String, Object> setsMap) {
        String setCode = getStr(c, "setCode");
        if (setCode == null) return;

        // Skip quest sets — only sync main sets
        Map<String, Object> setInfo = (Map<String, Object>) setsMap.get(setCode);
        if (setInfo != null) {
            String setType = getStr(setInfo, "type");
            if ("quest".equalsIgnoreCase(setType)) return;
        }

        // Skip promo cards entirely
        if (c.get("promoGrouping") != null) return;

        Object numObj = c.get("number");
        if (numObj == null) return;
        int cardNumber;
        try {
            cardNumber = ((Number) numObj).intValue();
        } catch (Exception e) {
            return;
        }

        String fullName = getStr(c, "fullName");
        if (fullName == null) {
            String name = getStr(c, "name");
            String version = getStr(c, "version");
            fullName = (name != null && version != null) ? name + " - " + version : name;
        }
        if (fullName == null) return;

        Edition edition = findOrCreateEdition(setCode, setsMap);

        Optional<Card> existing = cardRepository.findByCardNumberAndEdition(cardNumber, edition);
        Card card = existing.orElse(new Card());

        card.setName(fullName);
        card.setCardNumber(cardNumber);
        card.setEdition(edition);
        card.setRarity(getStr(c, "rarity"));
        card.setInkColor(getStr(c, "color"));
        card.setType(getStr(c, "type"));

        Object subtypesObj = c.get("subtypes");
        if (subtypesObj instanceof List<?> subtypeList) {
            card.setSubtypes(String.join(" • ", subtypeList.stream()
                    .map(Object::toString).toList()));
        }

        card.setBodyText(getStr(c, "fullText"));
        card.setFlavorText(getStr(c, "flavorText"));
        card.setArtist(getStr(c, "artistsText"));

        Object inkwellObj = c.get("inkwell");
        if (inkwellObj != null) card.setInkable(Boolean.TRUE.equals(inkwellObj));

        Object costObj = c.get("cost");
        if (costObj instanceof Number) card.setCost(((Number) costObj).intValue());

        Map<String, Object> images = (Map<String, Object>) c.get("images");
        if (images != null) {
            String thumbnailUrl = (String) images.get("thumbnail");
            String fullImageUrl = (String) images.get("full");
            card.setImageUrl(fullImageUrl != null ? fullImageUrl : thumbnailUrl);
            card.setThumbnailUrl(thumbnailUrl);
        }

        card.setExternalId(resolveExternalId(c, setCode, cardNumber));
        // Hash is NOT computed here — use startComputeHashes() (Étape 2)
        cardRepository.save(card);
    }

    private String resolveExternalId(Map<String, Object> cardData, String setCode, int cardNumber) {
        String[] candidateKeys = {
                "culture_invariant_id",
                "cultureInvariantId",
                "cardTraderId",
                "id"
        };
        for (String key : candidateKeys) {
            Object raw = cardData.get(key);
            if (raw == null) continue;
            String value = String.valueOf(raw).trim();
            if (!value.isEmpty()) return value;
        }
        return setCode + "/" + cardNumber;
    }

    // ─── Edition lookup ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Edition findOrCreateEdition(String setCode, Map<String, Object> setsMap) {
        Optional<Edition> existing = editionRepository.findByCode(setCode);
        if (existing.isPresent()) return existing.get();

        Edition edition = new Edition();
        edition.setCode(setCode);
        Map<String, Object> setInfo = (Map<String, Object>) setsMap.get(setCode);
        if (setInfo != null) {
            Object nameObj = setInfo.get("name");
            edition.setName(nameObj != null ? nameObj.toString() : "Set " + setCode);
            Object numObj = setInfo.get("number");
            if (numObj instanceof Number) edition.setSetNumber(((Number) numObj).intValue());
            Object rdObj = setInfo.get("releaseDate");
            if (rdObj != null) edition.setReleaseDate(rdObj.toString());
        } else {
            edition.setName("Set " + setCode);
        }
        return editionRepository.save(edition);
    }

    // ─── Configuration ────────────────────────────────────────────────────────

    public String getLorcaJsonUrl() {
        return settingsRepository.findBySettingKey(KEY_LORCAJSON_URL)
                .map(AppSettings::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(DEFAULT_URL);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null || String.valueOf(val).isBlank()) return null;
        return String.valueOf(val);
    }

    private Long computeAverageHash(String imageUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(imageUrl))
                    .timeout(Duration.ofSeconds(10))
                    .GET().build();
            HttpResponse<byte[]> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) return null;

            BufferedImage original = ImageIO.read(new ByteArrayInputStream(response.body()));
            if (original == null) return null;

            BufferedImage small = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = small.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(original.getScaledInstance(8, 8, Image.SCALE_SMOOTH), 0, 0, null);
            g.dispose();

            int[] pixels = new int[64];
            int sum = 0;
            for (int y = 0; y < 8; y++) {
                for (int x = 0; x < 8; x++) {
                    int rgb = small.getRGB(x, y);
                    int r = (rgb >> 16) & 0xff;
                    int green = (rgb >> 8) & 0xff;
                    int b = rgb & 0xff;
                    int gray = (int) (0.299 * r + 0.587 * green + 0.114 * b);
                    pixels[y * 8 + x] = gray;
                    sum += gray;
                }
            }
            int avg = sum / 64;
            long hash = 0L;
            for (int i = 0; i < 64; i++) {
                if (pixels[i] >= avg) hash |= (1L << i);
            }
            return hash;
        } catch (Exception e) {
            log.debug("Could not compute hash for {}: {}", imageUrl, e.getMessage());
            return null;
        }
    }
}

