package com.alexgit95.service;

import com.alexgit95.dto.EditionStatDTO;
import com.alexgit95.dto.RarityStatDTO;
import com.alexgit95.dto.StatisticsDTO;
import com.alexgit95.model.AppSettings;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private static final String KEY_STATS_ENABLED_SETS = "stats_enabled_sets";

    private static final List<String> RARITIES = Arrays.asList(
            "Commune", "Inhabituelle", "Rare", "Très Rare", "Légendaire"
    );

    private final AppSettingsRepository settingsRepository;
    private final CardRepository cardRepository;
    private final EditionRepository editionRepository;
    private final UserCollectionRepository collectionRepository;

    public StatisticsService(AppSettingsRepository settingsRepository,
                             CardRepository cardRepository,
                             EditionRepository editionRepository,
                             UserCollectionRepository collectionRepository) {
        this.settingsRepository = settingsRepository;
        this.cardRepository = cardRepository;
        this.editionRepository = editionRepository;
        this.collectionRepository = collectionRepository;
    }

    public StatisticsDTO getStatistics() {
        Set<Long> enabledSetIds = resolveEnabledSetIds();
        List<Edition> editions = editionRepository.findAll().stream()
                .filter(e -> enabledSetIds == null || enabledSetIds.contains(e.getId()))
                .collect(Collectors.toList());

        List<EditionStatDTO> byEdition = editions.stream()
                .map(this::buildEditionStat)
                .collect(Collectors.toList());

        long totalCards = byEdition.stream().mapToLong(EditionStatDTO::getTotalCards).sum();
        long ownedCards = byEdition.stream().mapToLong(EditionStatDTO::getOwnedCards).sum();
        long missingCards = byEdition.stream().mapToLong(EditionStatDTO::getMissingCards).sum();

        StatisticsDTO stats = new StatisticsDTO();
        stats.setTotalCards(totalCards);
        stats.setOwnedCards(ownedCards);
        stats.setMissingCards(missingCards);
        stats.setCompletionPercentage(totalCards > 0 ? (double) ownedCards / totalCards * 100 : 0);
        stats.setByEdition(byEdition);
        return stats;
    }

    private EditionStatDTO buildEditionStat(Edition edition) {
        long total = cardRepository.countByEditionAndRarityIn(edition, RARITIES);
        long owned = collectionRepository.countByEditionIdAndRarityIn(edition.getId(), RARITIES);
        long missing = total - owned;

        List<RarityStatDTO> byRarity = buildRarityStats(edition, cardRepository.findByEditionOrderByCardNumberAsc(edition));

        EditionStatDTO dto = new EditionStatDTO();
        dto.setEditionId(edition.getId());
        dto.setEditionName(edition.getName());
        dto.setEditionCode(edition.getCode());
        dto.setTotalCards(total);
        dto.setOwnedCards(owned);
        dto.setMissingCards(missing);
        dto.setCompletionPercentage(total > 0 ? (double) owned / total * 100 : 0);
        dto.setByRarity(byRarity);
        return dto;
    }

    public Set<Long> resolveEnabledSetIds() {
        // null means no filter configured yet -> all sets enabled by default.
        AppSettings setting = settingsRepository.findBySettingKey(KEY_STATS_ENABLED_SETS).orElse(null);
        if (setting == null) return null;

        String raw = setting.getSettingValue();
        if (raw == null || raw.isBlank()) return Set.of();

        Set<Long> parsed = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(s -> {
                    try {
                        return Long.valueOf(s);
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(v -> v != null)
                .collect(Collectors.toSet());

        return parsed;
    }

    private List<RarityStatDTO> buildRarityStats(Edition edition, List<Card> cards) {
        return RARITIES.stream()
                .map(rarity -> {
                    long total = cards.stream()
                            .filter(c -> rarity.equals(c.getRarity()))
                            .count();
                    if (total == 0) return null;
                    long owned = collectionRepository.countByEditionIdAndRarity(edition.getId(), rarity);
                    return new RarityStatDTO(rarity, total, owned, total - owned);
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
}
