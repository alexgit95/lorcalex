package com.alexgit95.service;

import com.alexgit95.dto.CardDTO;
import com.alexgit95.dto.PricingInsightsDTO;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PricingInsightsServiceTest {

    @Mock private CardRepository cardRepository;
    @Mock private UserCollectionRepository collectionRepository;
    @Mock private EditionRepository editionRepository;
    @Mock private CardService cardService;
    @Mock private StatisticsService statisticsService;

    private PricingInsightsService service;

    @BeforeEach
    void setUp() {
        service = new PricingInsightsService(cardRepository, collectionRepository, editionRepository, cardService, statisticsService);
    }

    @Test
    @DisplayName("computes valuation with quantity plus foilQuantity and tracks exclusions")
    void computesValuationAndExclusions() {
        Edition tracked = edition(1L, "TFC", "Premier", 1);
        Edition ignored = edition(2L, "ROF", "Floodborn", 2);

        Card priced = card(10L, tracked, new BigDecimal("2.50"), "EUR", LocalDateTime.now().minusHours(1));
        Card noPrice = card(11L, tracked, null, "EUR", LocalDateTime.now().minusHours(2));
        Card nonEur = card(12L, tracked, new BigDecimal("3.00"), "USD", LocalDateTime.now().minusHours(3));
        Card otherEdition = card(13L, ignored, new BigDecimal("9.00"), "EUR", LocalDateTime.now().minusHours(4));

        when(statisticsService.resolveEnabledSetIds()).thenReturn(Set.of(1L));
        when(editionRepository.findAll()).thenReturn(List.of(tracked, ignored));
        when(cardRepository.findByLastPriceAtIsNotNullOrderByLastPriceAtDescIdDesc(any(Pageable.class)))
                .thenReturn(List.of(priced, nonEur));

        when(cardService.toDTO(any(Card.class), any())).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0, Card.class);
            CardDTO dto = new CardDTO();
            dto.setId(card.getId());
            dto.setName(card.getName());
            dto.setEditionCode(card.getEdition().getCode());
            dto.setMarketPrice(card.getMarketPrice());
            dto.setPriceCurrency(card.getPriceCurrency());
            dto.setLastPriceAt(card.getLastPriceAt());
            UserCollection collection = invocation.getArgument(1, UserCollection.class);
            if (collection != null) {
                dto.setOwned(true);
                dto.setQuantity(collection.getQuantity());
                dto.setFoilQuantity(collection.getFoilQuantity());
            }
            return dto;
        });

        when(collectionRepository.findAllWithCardAndEdition()).thenReturn(List.of(
                collection(priced, 2, 1),
                collection(noPrice, 1, 0),
                collection(nonEur, 1, 1),
                collection(otherEdition, 1, 0)
        ));

        PricingInsightsDTO result = service.getInsights();

        assertThat(result.getCurrency()).isEqualTo("EUR");
        assertThat(result.getLatestPricedCards()).hasSize(1);
        assertThat(result.getLatestPricedCards().get(0).getId()).isEqualTo(10L);
        assertThat(result.getOwnedCardPriceRanking()).hasSize(1);
        assertThat(result.getOwnedCardPriceRanking().get(0).getId()).isEqualTo(10L);
        assertThat(result.getOwnedCardPriceRanking().get(0).getQuantity()).isEqualTo(2);
        assertThat(result.getOwnedCardPriceRanking().get(0).getFoilQuantity()).isEqualTo(1);
        assertThat(result.getEditionValuations()).hasSize(1);
        assertThat(result.getEditionValuations().get(0).getTotalValueEur()).isEqualByComparingTo("7.50");
        assertThat(result.getTotalCollectionValueEur()).isEqualByComparingTo("7.50");
        assertThat(result.getExcludedNoPrice()).isEqualTo(1);
        assertThat(result.getExcludedNonEur()).isEqualTo(1);
    }

    @Test
    @DisplayName("ranks eligible owned cards by unit price with stable ties and a 100-card cap")
    void ranksOwnedCardsByUnitPriceWithTieBreakingAndCap() {
        Edition tracked = edition(1L, "TFC", "Premier", 1);
        List<UserCollection> ownedCards = new ArrayList<>();
        ownedCards.add(collection(card(2L, tracked, new BigDecimal("100.00"), "EUR", LocalDateTime.now()), 1, 0));
        ownedCards.add(collection(card(1L, tracked, new BigDecimal("100.00"), "EUR", LocalDateTime.now()), 1, 0));
        for (long id = 3; id <= 102; id++) {
            ownedCards.add(collection(card(id, tracked, BigDecimal.valueOf(103 - id), "EUR", LocalDateTime.now()), 1, 0));
        }
        ownedCards.add(collection(card(103L, tracked, new BigDecimal("200.00"), "USD", LocalDateTime.now()), 1, 0));
        ownedCards.add(collection(card(104L, tracked, null, "EUR", LocalDateTime.now()), 1, 0));
        ownedCards.add(collection(card(105L, tracked, new BigDecimal("150.00"), "EUR", LocalDateTime.now()), 0, 0));

        when(statisticsService.resolveEnabledSetIds()).thenReturn(Set.of(1L));
        when(editionRepository.findAll()).thenReturn(List.of(tracked));
        when(cardRepository.findByLastPriceAtIsNotNullOrderByLastPriceAtDescIdDesc(any(Pageable.class)))
                .thenReturn(List.of());
        when(cardService.toDTO(any(Card.class), any())).thenAnswer(invocation -> {
            Card card = invocation.getArgument(0, Card.class);
            UserCollection collection = invocation.getArgument(1, UserCollection.class);
            CardDTO dto = new CardDTO();
            dto.setId(card.getId());
            dto.setMarketPrice(card.getMarketPrice());
            dto.setPriceCurrency(card.getPriceCurrency());
            dto.setQuantity(collection != null ? collection.getQuantity() : 0);
            dto.setFoilQuantity(collection != null ? collection.getFoilQuantity() : 0);
            return dto;
        });
        when(collectionRepository.findAllWithCardAndEdition()).thenReturn(ownedCards);

        PricingInsightsDTO result = service.getInsights();

        assertThat(result.getOwnedCardPriceRanking()).hasSize(100);
        assertThat(result.getOwnedCardPriceRanking().get(0).getId()).isEqualTo(1L);
        assertThat(result.getOwnedCardPriceRanking().get(1).getId()).isEqualTo(2L);
        assertThat(result.getOwnedCardPriceRanking())
                .noneMatch(card -> List.of(103L, 104L, 105L).contains(card.getId()));
    }

    private static Edition edition(Long id, String code, String name, Integer setNumber) {
        Edition e = new Edition();
        e.setId(id);
        e.setCode(code);
        e.setName(name);
        e.setSetNumber(setNumber);
        return e;
    }

    private static Card card(Long id, Edition edition, BigDecimal price, String currency, LocalDateTime lastPriceAt) {
        Card c = new Card();
        c.setId(id);
        c.setName("card-" + id);
        c.setEdition(edition);
        c.setMarketPrice(price);
        c.setPriceCurrency(currency);
        c.setLastPriceAt(lastPriceAt);
        return c;
    }

    private static UserCollection collection(Card card, int quantity, int foilQuantity) {
        UserCollection uc = new UserCollection();
        uc.setCard(card);
        uc.setQuantity(quantity);
        uc.setFoilQuantity(foilQuantity);
        uc.setFoil(foilQuantity > 0);
        return uc;
    }
}
