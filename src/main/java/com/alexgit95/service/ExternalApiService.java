package com.alexgit95.service;

import com.alexgit95.model.AppSettings;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ExternalApiService {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiService.class);
    private static final String KEY_API_ENABLED = "external_api_enabled";
    private static final String KEY_API_URL = "external_api_url";

    private final AppSettingsRepository settingsRepository;
    private final CardRepository cardRepository;
    private final EditionRepository editionRepository;
    private final WebClient.Builder webClientBuilder;

    public ExternalApiService(AppSettingsRepository settingsRepository,
                              CardRepository cardRepository,
                              EditionRepository editionRepository,
                              WebClient.Builder webClientBuilder) {
        this.settingsRepository = settingsRepository;
        this.cardRepository = cardRepository;
        this.editionRepository = editionRepository;
        this.webClientBuilder = webClientBuilder;
    }

    public boolean isApiEnabled() {
        return settingsRepository.findBySettingKey(KEY_API_ENABLED)
                .map(s -> "true".equalsIgnoreCase(s.getSettingValue()))
                .orElse(false);
    }

    public String getApiUrl() {
        return settingsRepository.findBySettingKey(KEY_API_URL)
                .map(AppSettings::getSettingValue)
                .orElse("https://api.lorcana-api.com/cards/all");
    }

    @Transactional
    @SuppressWarnings("unchecked")
    public int syncCards() {
        if (!isApiEnabled()) {
            throw new IllegalStateException("External API is disabled. Enable it in Admin settings first.");
        }

        String apiUrl = getApiUrl();
        log.info("Syncing cards from external API: {}", apiUrl);

        List<Map<String, Object>> apiCards;
        try {
            apiCards = webClientBuilder.build()
                    .get()
                    .uri(apiUrl)
                    .retrieve()
                    .bodyToFlux(Map.class)
                    .map(m -> (Map<String, Object>) m)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.error("Failed to fetch from external API: {}", e.getMessage());
            throw new RuntimeException("Failed to fetch cards from external API: " + e.getMessage());
        }

        if (apiCards == null || apiCards.isEmpty()) {
            return 0;
        }

        int syncedCount = 0;
        for (Map<String, Object> apiCard : apiCards) {
            try {
                syncCard(apiCard);
                syncedCount++;
            } catch (Exception e) {
                log.warn("Failed to sync card: {} - {}", apiCard.get("Name"), e.getMessage());
            }
        }
        log.info("Synced {} cards", syncedCount);
        return syncedCount;
    }

    private void syncCard(Map<String, Object> apiCard) {
        String setName = getStr(apiCard, "Set_Name", "set_name");
        String setCode = getStr(apiCard, "Set_ID", "set_id");
        String cardName = getStr(apiCard, "Name", "name");
        Object cardNumObj = apiCard.getOrDefault("Card_Num", apiCard.get("card_num"));
        if (cardName == null || cardNumObj == null) return;

        Edition edition = findOrCreateEdition(setName, setCode, apiCard);

        int cardNumber;
        try {
            cardNumber = Integer.parseInt(String.valueOf(cardNumObj));
        } catch (NumberFormatException e) {
            return;
        }

        Optional<Card> existing = cardRepository.findByCardNumberAndEdition(cardNumber, edition);
        Card card = existing.orElse(new Card());

        card.setName(cardName);
        card.setCardNumber(cardNumber);
        card.setEdition(edition);
        card.setRarity(getStr(apiCard, "Rarity", "rarity"));
        card.setInkColor(getStr(apiCard, "Color", "color"));
        card.setType(getStr(apiCard, "Type", "type"));
        card.setSubtypes(getStr(apiCard, "Classifications", "classifications"));
        card.setBodyText(getStr(apiCard, "Body_Text", "body_text"));
        card.setFlavorText(getStr(apiCard, "Flavor_Text", "flavor_text"));
        card.setImageUrl(getStr(apiCard, "Image", "image"));
        card.setArtist(getStr(apiCard, "Artist", "artist"));
        Object inkable = apiCard.getOrDefault("Inkable", apiCard.get("inkable"));
        if (inkable != null) card.setInkable(Boolean.parseBoolean(String.valueOf(inkable)));
        Object costObj = apiCard.getOrDefault("Cost", apiCard.get("cost"));
        if (costObj != null) {
            try { card.setCost(Integer.parseInt(String.valueOf(costObj))); } catch (Exception ignored) {}
        }

        cardRepository.save(card);
    }

    private Edition findOrCreateEdition(String name, String code, Map<String, Object> apiCard) {
        if (code != null) {
            Optional<Edition> byCode = editionRepository.findByCode(code);
            if (byCode.isPresent()) return byCode.get();
        }
        Edition edition = new Edition();
        edition.setName(name != null ? name : "Unknown Set");
        edition.setCode(code);
        Object setNum = apiCard.getOrDefault("Set_Num", apiCard.get("set_num"));
        if (setNum != null) {
            try { edition.setTotalCards(Integer.parseInt(String.valueOf(setNum))); } catch (Exception ignored) {}
        }
        return editionRepository.save(edition);
    }

    private String getStr(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object val = map.get(key);
            if (val != null && !String.valueOf(val).isBlank()) return String.valueOf(val);
        }
        return null;
    }
}
