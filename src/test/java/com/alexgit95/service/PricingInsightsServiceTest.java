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
        assertThat(result.getEditionValuations()).hasSize(1);
        assertThat(result.getEditionValuations().get(0).getTotalValueEur()).isEqualByComparingTo("7.50");
        assertThat(result.getTotalCollectionValueEur()).isEqualByComparingTo("7.50");
        assertThat(result.getExcludedNoPrice()).isEqualTo(1);
        assertThat(result.getExcludedNonEur()).isEqualTo(1);
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
