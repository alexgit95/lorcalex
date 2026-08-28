package com.alexgit95.service;

import com.alexgit95.dto.CardDTO;
import com.alexgit95.model.Card;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for CardService#setWanted — persists independently of UserCollection.
 */
@ExtendWith(MockitoExtension.class)
class CardServiceWantedTest {

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
        card = new Card();
        card.setId(10L);
        card.setName("Elsa");
        card.setWanted(false);
    }

    @Test
    @DisplayName("setWanted — not-owned card is marked wanted without touching UserCollection")
    void setWanted_notOwnedCard_persistsFlagWithoutCollectionChange() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.empty());

        Optional<CardDTO> result = cardService.setWanted(10L, true);

        ArgumentCaptor<Card> captor = ArgumentCaptor.forClass(Card.class);
        verify(cardRepository).save(captor.capture());
        assertThat(captor.getValue().getWanted()).isTrue();
        verify(collectionRepository, never()).save(any());

        assertThat(result).isPresent();
        assertThat(result.get().getWanted()).isTrue();
        assertThat(result.get().getOwned()).isFalse();
    }

    @Test
    @DisplayName("setWanted — flag remains true after ownership added then removed")
    void setWanted_survivesOwnershipRemoval() {
        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));
        when(cardRepository.save(any(Card.class))).thenAnswer(inv -> inv.getArgument(0));
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.empty());

        cardService.setWanted(10L, true);
        assertThat(card.getWanted()).isTrue();

        // Simulate ownership added then fully removed elsewhere (UserCollection row gone),
        // card.wanted must remain true since it is never touched by collection logic.
        UserCollection uc = new UserCollection();
        uc.setCard(card);
        uc.setQuantity(1);
        uc.setFoilQuantity(0);
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.of(uc));
        Optional<CardDTO> whileOwned = cardService.getCardById(10L);
        assertThat(whileOwned).isPresent();
        assertThat(whileOwned.get().getWanted()).isTrue();
        assertThat(whileOwned.get().getOwned()).isTrue();

        // Ownership removed: UserCollection row deleted (existing invariant), card.wanted untouched.
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.empty());
        Optional<CardDTO> afterRemoval = cardService.getCardById(10L);
        assertThat(afterRemoval).isPresent();
        assertThat(afterRemoval.get().getWanted()).isTrue();
        assertThat(afterRemoval.get().getOwned()).isFalse();
    }

    @Test
    @DisplayName("setWanted — unknown card id returns empty")
    void setWanted_unknownCard_returnsEmpty() {
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<CardDTO> result = cardService.setWanted(99L, true);

        assertThat(result).isEmpty();
    }
}
