package com.alexgit95.service;

import com.alexgit95.dto.CardDTO;
import com.alexgit95.dto.PricingEditionValuationDTO;
import com.alexgit95.dto.PricingInsightsDTO;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PricingInsightsService {

    private static final String TARGET_CURRENCY = "EUR";
    private static final int LATEST_PRICED_FETCH_SIZE = 200;
    private static final int LATEST_PRICED_LIMIT = 20;

    private final CardRepository cardRepository;
    private final UserCollectionRepository collectionRepository;
    private final EditionRepository editionRepository;
    private final CardService cardService;
    private final StatisticsService statisticsService;

    public PricingInsightsService(CardRepository cardRepository,
                                  UserCollectionRepository collectionRepository,
                                  EditionRepository editionRepository,
                                  CardService cardService,
                                  StatisticsService statisticsService) {
        this.cardRepository = cardRepository;
        this.collectionRepository = collectionRepository;
        this.editionRepository = editionRepository;
        this.cardService = cardService;
        this.statisticsService = statisticsService;
    }

    public PricingInsightsDTO getInsights() {
        Set<Long> enabledSetIds = statisticsService.resolveEnabledSetIds();

        List<CardDTO> latestPricedCards = cardRepository
                .findByLastPriceAtIsNotNullOrderByLastPriceAtDescIdDesc(PageRequest.of(0, LATEST_PRICED_FETCH_SIZE))
                .stream()
                .filter(this::isEurCard)
                .limit(LATEST_PRICED_LIMIT)
                .map(card -> cardService.toDTO(card, null))
                .toList();

        Map<Long, PricingEditionValuationDTO> byEdition = initTrackedEditions(enabledSetIds);

        int excludedNoPrice = 0;
        int excludedNonEur = 0;

        for (UserCollection uc : collectionRepository.findAllWithCardAndEdition()) {
            Card card = uc.getCard();
            Edition edition = card != null ? card.getEdition() : null;
            if (card == null || edition == null) {
                continue;
            }
            if (enabledSetIds != null && !enabledSetIds.contains(edition.getId())) {
                continue;
            }

            int quantity = safeInt(uc.getQuantity()) + safeInt(uc.getFoilQuantity());
            if (quantity <= 0) {
                continue;
            }

            BigDecimal price = card.getMarketPrice();
            if (price == null) {
                excludedNoPrice++;
                continue;
            }
            if (!TARGET_CURRENCY.equalsIgnoreCase(card.getPriceCurrency())) {
                excludedNonEur++;
                continue;
            }

            PricingEditionValuationDTO dto = byEdition.get(edition.getId());
            if (dto == null) {
                dto = new PricingEditionValuationDTO();
                dto.setEditionId(edition.getId());
                dto.setEditionName(edition.getName());
                dto.setEditionCode(edition.getCode());
                dto.setEditionSetNumber(edition.getSetNumber());
                dto.setTotalValueEur(BigDecimal.ZERO);
                byEdition.put(edition.getId(), dto);
            }

            BigDecimal lineValue = price.multiply(BigDecimal.valueOf(quantity));
            dto.setTotalValueEur(dto.getTotalValueEur().add(lineValue));
        }

        List<PricingEditionValuationDTO> valuations = byEdition.values().stream()
                .peek(v -> v.setTotalValueEur(v.getTotalValueEur().setScale(2, RoundingMode.HALF_UP)))
                .sorted(Comparator
                        .comparing((PricingEditionValuationDTO v) -> v.getEditionSetNumber() != null ? v.getEditionSetNumber() : Integer.MAX_VALUE)
                        .thenComparing(v -> v.getEditionName() != null ? v.getEditionName() : ""))
                .toList();

        BigDecimal total = valuations.stream()
                .map(PricingEditionValuationDTO::getTotalValueEur)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        PricingInsightsDTO dto = new PricingInsightsDTO();
        dto.setCurrency(TARGET_CURRENCY);
        dto.setTotalCollectionValueEur(total);
        dto.setExcludedNoPrice(excludedNoPrice);
        dto.setExcludedNonEur(excludedNonEur);
        dto.setLatestPricedCards(latestPricedCards);
        dto.setEditionValuations(valuations);
        return dto;
    }

    private Map<Long, PricingEditionValuationDTO> initTrackedEditions(Set<Long> enabledSetIds) {
        List<Edition> trackedEditions = editionRepository.findAll().stream()
                .filter(e -> enabledSetIds == null || enabledSetIds.contains(e.getId()))
                .toList();

        Map<Long, PricingEditionValuationDTO> byEdition = new LinkedHashMap<>();
        for (Edition edition : trackedEditions) {
            PricingEditionValuationDTO dto = new PricingEditionValuationDTO();
            dto.setEditionId(edition.getId());
            dto.setEditionName(edition.getName());
            dto.setEditionCode(edition.getCode());
            dto.setEditionSetNumber(edition.getSetNumber());
            dto.setTotalValueEur(BigDecimal.ZERO);
            byEdition.put(edition.getId(), dto);
        }
        return byEdition;
    }

    private boolean isEurCard(Card card) {
        return card.getMarketPrice() != null
                && card.getPriceCurrency() != null
                && TARGET_CURRENCY.equalsIgnoreCase(card.getPriceCurrency());
    }

    private static int safeInt(Integer value) {
        return value != null ? Math.max(0, value) : 0;
    }
}
