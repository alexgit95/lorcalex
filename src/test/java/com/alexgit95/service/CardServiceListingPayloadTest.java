package com.alexgit95.service;

import com.alexgit95.dto.CardDTO;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CardService — listing methods omit rules text, detail keeps it.
 */
@ExtendWith(MockitoExtension.class)
class CardServiceListingPayloadTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private EditionRepository editionRepository;
    @Mock
    private UserCollectionRepository collectionRepository;

    @InjectMocks
    private CardService cardService;

    private Card card;

    @BeforeEach
    void setup() {
        Edition ed = new Edition();
        ed.setId(1L);
        ed.setCode("TFC");
        ed.setName("Premier Chapitre");

        card = new Card();
        card.setId(10L);
        card.setName("Elsa");
        card.setEdition(ed);
        card.setCardNumber(42);
        card.setBodyText("Corps de texte des règles");
        card.setFlavorText("Texte d'ambiance");
        card.setImageUrl("https://example.com/full.jpg");
        card.setThumbnailUrl("https://example.com/thumb.jpg");
    }

    @Test
    @DisplayName("getCardsByEdition omits rules text and includes thumbnailUrl")
    void getCardsByEdition_omitsRulesText() {
        when(cardRepository.findByEditionIdOrderByCardNumberAsc(1L)).thenReturn(List.of(card));
        when(collectionRepository.findByEditionId(1L)).thenReturn(List.of());

        List<CardDTO> result = cardService.getCardsByEdition(1L);

        assertThat(result).hasSize(1);
        CardDTO dto = result.get(0);
        assertThat(dto.getBodyText()).isNull();
        assertThat(dto.getFlavorText()).isNull();
        assertThat(dto.getThumbnailUrl()).isEqualTo("https://example.com/thumb.jpg");
        assertThat(dto.getImageUrl()).isEqualTo("https://example.com/full.jpg");
    }

    @Test
    @DisplayName("getAllCardsWithCollection omits rules text and includes thumbnailUrl")
    void getAllCardsWithCollection_omitsRulesText() {
        when(cardRepository.findAll()).thenReturn(List.of(card));
        when(collectionRepository.findAll()).thenReturn(List.of());

        List<CardDTO> result = cardService.getAllCardsWithCollection();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBodyText()).isNull();
        assertThat(result.get(0).getFlavorText()).isNull();
        assertThat(result.get(0).getThumbnailUrl()).isEqualTo("https://example.com/thumb.jpg");
    }

    @Test
    @DisplayName("searchCards omits rules text and includes thumbnailUrl")
    void searchCards_omitsRulesText() {
        when(cardRepository.searchByName("Elsa")).thenReturn(List.of(card));
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.empty());

        List<CardDTO> result = cardService.searchCards("Elsa");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBodyText()).isNull();
        assertThat(result.get(0).getFlavorText()).isNull();
        assertThat(result.get(0).getThumbnailUrl()).isEqualTo("https://example.com/thumb.jpg");
    }

    @Test
    @DisplayName("getCardById keeps rules text (full detail)")
    void getCardById_keepsRulesText() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.empty());

        Optional<CardDTO> result = cardService.getCardById(10L);

        assertThat(result).isPresent();
        assertThat(result.get().getBodyText()).isEqualTo("Corps de texte des règles");
        assertThat(result.get().getFlavorText()).isEqualTo("Texte d'ambiance");
        assertThat(result.get().getThumbnailUrl()).isEqualTo("https://example.com/thumb.jpg");
    }
}
