package com.alexgit95.config;

import com.alexgit95.model.Card;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.UserCollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionIntegrityRepairTest {

    @Mock
    private UserCollectionRepository userCollectionRepository;

    private CollectionIntegrityRepair repair;

    @BeforeEach
    void setUp() {
        repair = new CollectionIntegrityRepair(userCollectionRepository);
    }

    @Test
    @DisplayName("Repairing an inconsistent foil flag does not change lastAddedAt")
    void repairFoilInvariant_doesNotBumpLastAddedAt() {
        Card card = new Card();
        card.setId(1L);

        LocalDateTime staleDate = LocalDateTime.now().minusDays(30);
        UserCollection uc = new UserCollection();
        uc.setCard(card);
        uc.setQuantity(2);
        uc.setFoilQuantity(3);
        uc.setFoil(false); // inconsistent: foilQuantity > 0 but foil is false
        uc.setLastAddedAt(staleDate);

        when(userCollectionRepository.findAll()).thenReturn(List.of(uc));

        repair.repairFoilInvariantIfNeeded();

        ArgumentCaptor<List<UserCollection>> captor = ArgumentCaptor.forClass(List.class);
        verify(userCollectionRepository).saveAll(captor.capture());

        UserCollection repaired = captor.getValue().get(0);
        assertThat(repaired.getFoil()).isTrue();
        assertThat(repaired.getLastAddedAt()).isEqualTo(staleDate);
    }
}
