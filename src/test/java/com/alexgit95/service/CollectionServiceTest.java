package com.alexgit95.service;

import com.alexgit95.dto.CardDTO;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.CardRepository;
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
import static org.mockito.Mockito.*;

/**
 * Unit tests for CollectionService — foil flag and quantity logic.
 */
@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock
    private CardRepository cardRepository;
    @Mock
    private UserCollectionRepository collectionRepository;
    @Mock
    private CardService cardService;

    @InjectMocks
    private CollectionService collectionService;

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

        when(cardRepository.findById(10L)).thenReturn(Optional.of(card));

        // toDTO stub: maps the key fields we assert on
        when(cardService.toDTO(any(Card.class), any())).thenAnswer(inv -> {
            Card c   = inv.getArgument(0);
            UserCollection uc = inv.getArgument(1);
            CardDTO dto = new CardDTO();
            dto.setId(c.getId());
            dto.setOwned(uc != null);
            dto.setQuantity(uc != null ? uc.getQuantity() : 0);
            dto.setFoilQuantity(uc != null ? uc.getFoilQuantity() : 0);
            dto.setFoil(uc != null && Boolean.TRUE.equals(uc.getFoil()));
            return dto;
        });
    }

    // ─── addCard ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("addCard — new card, foil=false → persisted with foil false")
    void addCard_newCard_foilFalse() {
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.empty());
        when(collectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CardDTO result = collectionService.addCard(10L, 1, 0, false);

        ArgumentCaptor<UserCollection> captor = ArgumentCaptor.forClass(UserCollection.class);
        verify(collectionRepository).save(captor.capture());

        assertThat(captor.getValue().getFoil()).isFalse();
        assertThat(captor.getValue().getQuantity()).isEqualTo(1);
        assertThat(result.getFoil()).isFalse();
        assertThat(result.getOwned()).isTrue();
    }

    @Test
    @DisplayName("addCard — new card, foilQuantity=0 keeps foil false even if foil=true input")
    void addCard_newCard_foilInputIgnoredWhenNoFoilQuantity() {
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.empty());
        when(collectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CardDTO result = collectionService.addCard(10L, 2, 0, true);

        ArgumentCaptor<UserCollection> captor = ArgumentCaptor.forClass(UserCollection.class);
        verify(collectionRepository).save(captor.capture());

        assertThat(captor.getValue().getFoil()).isFalse();
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);
        assertThat(result.getFoil()).isFalse();
    }

    @Test
    @DisplayName("addCard — foilQuantity>0 forces foil=true")
    void addCard_newCard_positiveFoilQuantityForcesFoilTrue() {
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.empty());
        when(collectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CardDTO result = collectionService.addCard(10L, 0, 2, false);

        ArgumentCaptor<UserCollection> captor = ArgumentCaptor.forClass(UserCollection.class);
        verify(collectionRepository).save(captor.capture());

        assertThat(captor.getValue().getFoil()).isTrue();
        assertThat(captor.getValue().getFoilQuantity()).isEqualTo(2);
        assertThat(result.getFoil()).isTrue();
    }

    @Test
    @DisplayName("addCard — existing card: quantity incremented and foil follows foilQuantity")
    void addCard_existingCard_incrementsQuantityAndFoilFollowsFoilQuantity() {
        UserCollection existing = new UserCollection();
        existing.setCard(card);
        existing.setQuantity(2);
        existing.setFoil(false);
        existing.setFoilQuantity(0);
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.of(existing));
        when(collectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        collectionService.addCard(10L, 1, 0, true);

        ArgumentCaptor<UserCollection> captor = ArgumentCaptor.forClass(UserCollection.class);
        verify(collectionRepository).save(captor.capture());

        assertThat(captor.getValue().getQuantity()).isEqualTo(3);
        assertThat(captor.getValue().getFoil()).isFalse();
    }

    // ─── updateQuantity ───────────────────────────────────────────────────────

    @Test
    @DisplayName("updateQuantity — foil invariant is derived from foilQuantity")
    void updateQuantity_foilInvariantDerivedFromFoilQuantity() {
        UserCollection existing = new UserCollection();
        existing.setCard(card);
        existing.setQuantity(1);
        existing.setFoil(false);
        existing.setFoilQuantity(0);
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.of(existing));
        when(collectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CardDTO result = collectionService.updateQuantity(10L, 3, 0, true);

        ArgumentCaptor<UserCollection> captor = ArgumentCaptor.forClass(UserCollection.class);
        verify(collectionRepository).save(captor.capture());

        assertThat(captor.getValue().getQuantity()).isEqualTo(3);
        assertThat(captor.getValue().getFoil()).isFalse();
        assertThat(result.getFoil()).isFalse();
    }

    @Test
    @DisplayName("updateQuantity — foil value follows foilQuantity even when foil input is null")
    void updateQuantity_foilNull_stillFollowsFoilQuantity() {
        UserCollection existing = new UserCollection();
        existing.setCard(card);
        existing.setQuantity(1);
        existing.setFoil(true);
        existing.setFoilQuantity(3);
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.of(existing));
        when(collectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        collectionService.updateQuantity(10L, 2, 0, null);

        ArgumentCaptor<UserCollection> captor = ArgumentCaptor.forClass(UserCollection.class);
        verify(collectionRepository).save(captor.capture());

        assertThat(captor.getValue().getFoil()).isFalse();
        assertThat(captor.getValue().getQuantity()).isEqualTo(2);
    }

    @Test
    @DisplayName("updateQuantity — quantity ≤ 0: card is removed from collection")
    void updateQuantity_zeroQuantity_removesCard() {
        UserCollection existing = new UserCollection();
        existing.setCard(card);
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.of(existing));

        collectionService.updateQuantity(10L, 0, 0, null);

        verify(collectionRepository).delete(existing);
        verify(collectionRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateQuantity — new entry computes foil from foilQuantity")
    void updateQuantity_noExistingEntry_createsNew() {
        when(collectionRepository.findByCardId(10L)).thenReturn(Optional.empty());
        when(collectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        collectionService.updateQuantity(10L, 5, 0, true);

        ArgumentCaptor<UserCollection> captor = ArgumentCaptor.forClass(UserCollection.class);
        verify(collectionRepository).save(captor.capture());

        assertThat(captor.getValue().getQuantity()).isEqualTo(5);
        assertThat(captor.getValue().getFoil()).isFalse();
    }
}
