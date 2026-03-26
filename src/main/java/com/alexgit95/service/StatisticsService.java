package com.alexgit95.service;

import com.alexgit95.dto.EditionStatDTO;
import com.alexgit95.dto.RarityStatDTO;
import com.alexgit95.dto.StatisticsDTO;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    private static final List<String> RARITIES = Arrays.asList(
            "Common", "Uncommon", "Rare", "Super Rare", "Legendary", "Enchanted"
    );

    private final CardRepository cardRepository;
    private final EditionRepository editionRepository;
    private final UserCollectionRepository collectionRepository;

    public StatisticsService(CardRepository cardRepository,
                             EditionRepository editionRepository,
                             UserCollectionRepository collectionRepository) {
        this.cardRepository = cardRepository;
        this.editionRepository = editionRepository;
        this.collectionRepository = collectionRepository;
    }

    public StatisticsDTO getStatistics() {
        long totalCards = cardRepository.count();
        long ownedCards = collectionRepository.count();
        long missingCards = totalCards - ownedCards;

        List<Edition> editions = editionRepository.findAll();
        List<EditionStatDTO> byEdition = editions.stream()
                .map(this::buildEditionStat)
                .collect(Collectors.toList());

        StatisticsDTO stats = new StatisticsDTO();
        stats.setTotalCards(totalCards);
        stats.setOwnedCards(ownedCards);
        stats.setMissingCards(missingCards);
        stats.setCompletionPercentage(totalCards > 0 ? (double) ownedCards / totalCards * 100 : 0);
        stats.setByEdition(byEdition);
        return stats;
    }

    private EditionStatDTO buildEditionStat(Edition edition) {
        long total = cardRepository.countByEdition(edition);
        long owned = collectionRepository.countByEditionId(edition.getId());
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

    private List<RarityStatDTO> buildRarityStats(Edition edition, List<Card> cards) {
        return RARITIES.stream()
                .map(rarity -> {
                    long total = cards.stream().filter(c -> rarity.equals(c.getRarity())).count();
                    if (total == 0) return null;
                    long owned = collectionRepository.countByEditionIdAndRarity(edition.getId(), rarity);
                    return new RarityStatDTO(rarity, total, owned, total - owned);
                })
                .filter(dto -> dto != null)
                .collect(Collectors.toList());
    }
}
