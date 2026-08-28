package com.alexgit95.service;

import com.alexgit95.dto.EditionStatDTO;
import com.alexgit95.dto.MissingByColorDTO;
import com.alexgit95.dto.StatisticsDTO;
import com.alexgit95.model.AppSettings;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.AppSettingsRepository;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticsServiceTest {

    @Mock
    private AppSettingsRepository settingsRepository;
    @Mock
    private CardRepository cardRepository;
    @Mock
    private EditionRepository editionRepository;
    @Mock
    private UserCollectionRepository collectionRepository;

    private StatisticsService service;

    @BeforeEach
    void setUp() {
        lenient().when(settingsRepository.findBySettingKey(anyString())).thenReturn(Optional.empty());
        service = new StatisticsService(settingsRepository, cardRepository, editionRepository, collectionRepository);
    }

    @Test
    @DisplayName("missingByColor groups missing cards by ink color and rarity, only present combinations")
    void getStatistics_computesMissingByColor() {
        Edition edition = edition(1L, "Winterspell", "11WSP");

        Card ownedCommuneAmbre = card(1L, "Commune", "Ambre");
        Card missingRareAmbre = card(2L, "Rare", "Ambre");
        Card missingLegendaireRubis = card(3L, "Légendaire", "Rubis");
        Card ownedCommuneRubis = card(4L, "Commune", "Rubis");
        List<Card> cards = List.of(ownedCommuneAmbre, missingRareAmbre, missingLegendaireRubis, ownedCommuneRubis);

        when(editionRepository.findAll()).thenReturn(List.of(edition));
        when(cardRepository.countByEditionAndRarityIn(edition, List.of("Commune", "Inhabituelle", "Rare", "Très Rare", "Légendaire")))
                .thenReturn(4L);
        when(collectionRepository.countByEditionIdAndRarityIn(1L, List.of("Commune", "Inhabituelle", "Rare", "Très Rare", "Légendaire")))
                .thenReturn(2L);
        when(cardRepository.findByEditionOrderByCardNumberAsc(edition)).thenReturn(cards);
        when(collectionRepository.countByEditionIdAndRarity(1L, "Commune")).thenReturn(2L);
        when(collectionRepository.countByEditionIdAndRarity(1L, "Rare")).thenReturn(0L);
        when(collectionRepository.countByEditionIdAndRarity(1L, "Légendaire")).thenReturn(0L);
        when(collectionRepository.findByEditionId(1L)).thenReturn(List.of(
                owned(ownedCommuneAmbre), owned(ownedCommuneRubis)
        ));

        StatisticsDTO stats = service.getStatistics();

        assertThat(stats.getByEdition()).hasSize(1);
        EditionStatDTO editionStat = stats.getByEdition().get(0);
        List<MissingByColorDTO> missingByColor = editionStat.getMissingByColor();

        assertThat(missingByColor).hasSize(2);
        MissingByColorDTO ambre = missingByColor.stream().filter(m -> m.getInkColor().equals("Ambre")).findFirst().orElseThrow();
        assertThat(ambre.getByRarity()).hasSize(1);
        assertThat(ambre.getByRarity().get(0).getRarity()).isEqualTo("Rare");
        assertThat(ambre.getByRarity().get(0).getMissingCards()).isEqualTo(1L);

        MissingByColorDTO rubis = missingByColor.stream().filter(m -> m.getInkColor().equals("Rubis")).findFirst().orElseThrow();
        assertThat(rubis.getByRarity()).hasSize(1);
        assertThat(rubis.getByRarity().get(0).getRarity()).isEqualTo("Légendaire");
        assertThat(rubis.getByRarity().get(0).getMissingCards()).isEqualTo(1L);
    }

    @Test
    @DisplayName("tracked editions filter excludes disabled editions from missingByColor breakdown")
    void getStatistics_respectsTrackedEditionsFilter() {
        Edition tracked = edition(1L, "Winterspell", "11WSP");
        Edition untracked = edition(2L, "TFC", "1TFC");

        AppSettings filterSetting = new AppSettings("stats_enabled_sets", "1", "test");
        when(settingsRepository.findBySettingKey("stats_enabled_sets")).thenReturn(Optional.of(filterSetting));
        when(editionRepository.findAll()).thenReturn(List.of(tracked, untracked));

        Card missingCommuneAmbre = card(1L, "Commune", "Ambre");
        when(cardRepository.countByEditionAndRarityIn(tracked, List.of("Commune", "Inhabituelle", "Rare", "Très Rare", "Légendaire")))
                .thenReturn(1L);
        when(collectionRepository.countByEditionIdAndRarityIn(1L, List.of("Commune", "Inhabituelle", "Rare", "Très Rare", "Légendaire")))
                .thenReturn(0L);
        when(cardRepository.findByEditionOrderByCardNumberAsc(tracked)).thenReturn(List.of(missingCommuneAmbre));
        when(collectionRepository.countByEditionIdAndRarity(1L, "Commune")).thenReturn(0L);
        when(collectionRepository.findByEditionId(1L)).thenReturn(List.of());

        StatisticsDTO stats = service.getStatistics();

        assertThat(stats.getByEdition()).hasSize(1);
        assertThat(stats.getByEdition().get(0).getEditionId()).isEqualTo(1L);
    }

    private static Edition edition(Long id, String name, String code) {
        Edition edition = new Edition();
        edition.setId(id);
        edition.setName(name);
        edition.setCode(code);
        return edition;
    }

    private static Card card(Long id, String rarity, String inkColor) {
        Card card = new Card();
        card.setId(id);
        card.setRarity(rarity);
        card.setInkColor(inkColor);
        return card;
    }

    private static UserCollection owned(Card card) {
        UserCollection uc = new UserCollection();
        uc.setCard(card);
        uc.setQuantity(1);
        uc.setFoilQuantity(0);
        return uc;
    }
}
