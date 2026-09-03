package com.alexgit95.service;

import com.alexgit95.dto.CollectionValueTrendDTO;
import com.alexgit95.dto.EditionDeltaDTO;
import com.alexgit95.model.CollectionValueSnapshot;
import com.alexgit95.model.EditionValueSnapshot;
import com.alexgit95.repository.CollectionValueSnapshotRepository;
import com.alexgit95.repository.EditionValueSnapshotRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CollectionValueTrendServiceTest {

    @Mock private CollectionValueSnapshotRepository collectionValueSnapshotRepository;
    @Mock private EditionValueSnapshotRepository editionValueSnapshotRepository;
    @Mock private UserCollectionRepository userCollectionRepository;
    @Mock private StatisticsService statisticsService;

    private CollectionValueTrendService service;

    @BeforeEach
    void setUp() {
        service = new CollectionValueTrendService(collectionValueSnapshotRepository, editionValueSnapshotRepository,
                userCollectionRepository, statisticsService);
    }

    @Test
    @DisplayName("returns ordered global trend and edition deltas")
    void returnsOrderedTrendAndEditionDeltas() {
        LocalDateTime referenceTime = LocalDateTime.now();

        CollectionValueSnapshot snapshot0 = new CollectionValueSnapshot();
        snapshot0.setId(1L);
        snapshot0.setRecordedAt(referenceTime.minusDays(30));
        snapshot0.setTotalCollectionValueEur(new BigDecimal("100.00"));
        snapshot0.setCurrency("EUR");
        snapshot0.setSource("PRICING_SYNC");

        CollectionValueSnapshot snapshot1 = new CollectionValueSnapshot();
        snapshot1.setId(2L);
        snapshot1.setRecordedAt(referenceTime.minusDays(7));
        snapshot1.setTotalCollectionValueEur(new BigDecimal("120.00"));
        snapshot1.setCurrency("EUR");
        snapshot1.setSource("PRICING_SYNC");

        CollectionValueSnapshot snapshot2 = new CollectionValueSnapshot();
        snapshot2.setId(3L);
        snapshot2.setRecordedAt(referenceTime);
        snapshot2.setTotalCollectionValueEur(new BigDecimal("150.00"));
        snapshot2.setCurrency("EUR");
        snapshot2.setSource("PRICING_SYNC");

        when(collectionValueSnapshotRepository.findAllByOrderByRecordedAtAsc()).thenReturn(List.of(snapshot0, snapshot1, snapshot2));

        EditionValueSnapshot editionSnapshotNow = new EditionValueSnapshot();
        editionSnapshotNow.setId(10L);
        editionSnapshotNow.setRecordedAt(referenceTime);
        editionSnapshotNow.setEditionId(99L);
        editionSnapshotNow.setEditionCode("TFC");
        editionSnapshotNow.setEditionName("TFC");
        editionSnapshotNow.setTotalValueEur(new BigDecimal("150.00"));

        EditionValueSnapshot editionSnapshot7d = new EditionValueSnapshot();
        editionSnapshot7d.setId(11L);
        editionSnapshot7d.setRecordedAt(referenceTime.minusDays(7));
        editionSnapshot7d.setEditionId(99L);
        editionSnapshot7d.setEditionCode("TFC");
        editionSnapshot7d.setEditionName("TFC");
        editionSnapshot7d.setTotalValueEur(new BigDecimal("120.00"));

        EditionValueSnapshot editionSnapshot30d = new EditionValueSnapshot();
        editionSnapshot30d.setId(12L);
        editionSnapshot30d.setRecordedAt(referenceTime.minusDays(30));
        editionSnapshot30d.setEditionId(99L);
        editionSnapshot30d.setEditionCode("TFC");
        editionSnapshot30d.setEditionName("TFC");
        editionSnapshot30d.setTotalValueEur(new BigDecimal("100.00"));

        when(editionValueSnapshotRepository.findAll()).thenReturn(List.of(editionSnapshotNow, editionSnapshot7d, editionSnapshot30d));

        CollectionValueTrendDTO trend = service.getTrend();
        List<EditionDeltaDTO> deltas = service.getEditionDeltas();

        assertThat(trend.getTrend()).hasSize(3);
        assertThat(trend.getTrend().get(0).getId()).isEqualTo(1L);
        assertThat(trend.getTrend().get(0).getTotalCollectionValueEur()).isEqualByComparingTo("100.00");
        assertThat(trend.getTrend().get(2).getTotalCollectionValueEur()).isEqualByComparingTo("150.00");
        assertThat(deltas).isNotEmpty();
        assertThat(deltas.get(0).getDelta7dPercent()).isEqualByComparingTo("25.00");
        assertThat(deltas.get(0).getDelta30dPercent()).isEqualByComparingTo("50.00");
    }

    @Test
    @DisplayName("deleteSnapshot deletes the collection snapshot and all edition snapshots sharing its recordedAt")
    void deleteSnapshot_deletesCollectionSnapshotAndCorrelatedEditionSnapshots() {
        LocalDateTime recordedAt = LocalDateTime.now();
        CollectionValueSnapshot snapshot = new CollectionValueSnapshot();
        snapshot.setId(5L);
        snapshot.setRecordedAt(recordedAt);
        snapshot.setTotalCollectionValueEur(new BigDecimal("200.00"));

        when(collectionValueSnapshotRepository.findById(5L)).thenReturn(java.util.Optional.of(snapshot));

        service.deleteSnapshot(5L);

        org.mockito.Mockito.verify(collectionValueSnapshotRepository).delete(snapshot);
        org.mockito.Mockito.verify(editionValueSnapshotRepository).deleteByRecordedAt(recordedAt);
    }

    @Test
    @DisplayName("deleteSnapshot throws a not-found error for a non-existent snapshot id")
    void deleteSnapshot_throwsNotFoundForUnknownId() {
        when(collectionValueSnapshotRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.deleteSnapshot(999L))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class);

        org.mockito.Mockito.verify(editionValueSnapshotRepository, org.mockito.Mockito.never()).deleteByRecordedAt(org.mockito.ArgumentMatchers.any());
    }
}
