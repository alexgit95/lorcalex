package com.alexgit95.service;

import com.alexgit95.dto.CollectionValueTrendDTO;
import com.alexgit95.dto.CollectionValueTrendPointDTO;
import com.alexgit95.dto.EditionDeltaDTO;
import com.alexgit95.model.Card;
import com.alexgit95.model.CollectionValueSnapshot;
import com.alexgit95.model.Edition;
import com.alexgit95.model.EditionValueSnapshot;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.CollectionValueSnapshotRepository;
import com.alexgit95.repository.EditionValueSnapshotRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CollectionValueTrendService {

    private static final String TARGET_CURRENCY = "EUR";

    private final CollectionValueSnapshotRepository collectionValueSnapshotRepository;
    private final EditionValueSnapshotRepository editionValueSnapshotRepository;
    private final UserCollectionRepository userCollectionRepository;
    private final StatisticsService statisticsService;

    public CollectionValueTrendService(CollectionValueSnapshotRepository collectionValueSnapshotRepository,
                                      EditionValueSnapshotRepository editionValueSnapshotRepository,
                                      UserCollectionRepository userCollectionRepository,
                                      StatisticsService statisticsService) {
        this.collectionValueSnapshotRepository = collectionValueSnapshotRepository;
        this.editionValueSnapshotRepository = editionValueSnapshotRepository;
        this.userCollectionRepository = userCollectionRepository;
        this.statisticsService = statisticsService;
    }

    public CollectionValueTrendDTO getTrend() {
        List<CollectionValueTrendPointDTO> points = collectionValueSnapshotRepository.findAllByOrderByRecordedAtAsc()
                .stream()
                .map(snapshot -> {
                    CollectionValueTrendPointDTO point = new CollectionValueTrendPointDTO();
                    point.setId(snapshot.getId());
                    point.setRecordedAt(snapshot.getRecordedAt());
                    point.setTotalCollectionValueEur(snapshot.getTotalCollectionValueEur());
                    return point;
                })
                .toList();

        CollectionValueTrendDTO dto = new CollectionValueTrendDTO();
        dto.setTrend(points);
        return dto;
    }

    @Transactional
    public void deleteSnapshot(Long snapshotId) {
        CollectionValueSnapshot snapshot = collectionValueSnapshotRepository.findById(snapshotId)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "Trend point introuvable"));
        collectionValueSnapshotRepository.delete(snapshot);
        editionValueSnapshotRepository.deleteByRecordedAt(snapshot.getRecordedAt());
    }

    public List<EditionDeltaDTO> getEditionDeltas() {
        List<EditionValueSnapshot> all = editionValueSnapshotRepository.findAll();
        if (all.isEmpty()) {
            return List.of();
        }

        Map<Long, List<EditionValueSnapshot>> byEdition = all.stream()
                .collect(Collectors.groupingBy(EditionValueSnapshot::getEditionId));

        List<EditionDeltaDTO> result = new ArrayList<>();
        for (Map.Entry<Long, List<EditionValueSnapshot>> entry : byEdition.entrySet()) {
            List<EditionValueSnapshot> snapshots = entry.getValue().stream()
                    .sorted((a, b) -> a.getRecordedAt().compareTo(b.getRecordedAt()))
                    .toList();

            if (snapshots.isEmpty()) {
                continue;
            }

            EditionValueSnapshot current = snapshots.get(snapshots.size() - 1);
            EditionValueSnapshot snapshot7d = findLatestSnapshotAtOrBefore(snapshots, current.getRecordedAt().minusDays(7));
            EditionValueSnapshot snapshot30d = findLatestSnapshotAtOrBefore(snapshots, current.getRecordedAt().minusDays(30));

            EditionDeltaDTO dto = new EditionDeltaDTO();
            dto.setEditionId(current.getEditionId());
            dto.setEditionCode(current.getEditionCode());
            dto.setEditionName(current.getEditionName());
            dto.setCurrentValueEur(current.getTotalValueEur());
            dto.setValue7dEur(snapshot7d != null ? snapshot7d.getTotalValueEur() : null);
            dto.setValue30dEur(snapshot30d != null ? snapshot30d.getTotalValueEur() : null);
            dto.setDelta7dPercent(computeDelta(current.getTotalValueEur(), snapshot7d != null ? snapshot7d.getTotalValueEur() : null));
            dto.setDelta30dPercent(computeDelta(current.getTotalValueEur(), snapshot30d != null ? snapshot30d.getTotalValueEur() : null));
            result.add(dto);
        }

        return result;
    }

    @Transactional
    public void persistSnapshotFromCurrentCollection() {
        Set<Long> enabledSetIds = statisticsService.resolveEnabledSetIds();
        LocalDateTime now = LocalDateTime.now();

        Map<Long, Edition> editionsById = new LinkedHashMap<>();
        Map<Long, BigDecimal> perEdition = new LinkedHashMap<>();
        BigDecimal total = BigDecimal.ZERO;

        for (UserCollection userCollection : userCollectionRepository.findAllWithCardAndEdition()) {
            Card card = userCollection.getCard();
            Edition edition = card != null ? card.getEdition() : null;
            if (card == null || edition == null) {
                continue;
            }
            if (enabledSetIds != null && !enabledSetIds.contains(edition.getId())) {
                continue;
            }
            editionsById.putIfAbsent(edition.getId(), edition);

            int quantity = safeInt(userCollection.getQuantity()) + safeInt(userCollection.getFoilQuantity());
            if (quantity <= 0) {
                continue;
            }

            BigDecimal price = card.getMarketPrice();
            if (price == null || !TARGET_CURRENCY.equalsIgnoreCase(card.getPriceCurrency())) {
                continue;
            }

            BigDecimal lineValue = price.multiply(BigDecimal.valueOf(quantity));
            total = total.add(lineValue);
            perEdition.merge(edition.getId(), lineValue, BigDecimal::add);
        }

        CollectionValueSnapshot globalSnapshot = new CollectionValueSnapshot();
        globalSnapshot.setRecordedAt(now);
        globalSnapshot.setTotalCollectionValueEur(total.setScale(2, RoundingMode.HALF_UP));
        globalSnapshot.setCurrency(TARGET_CURRENCY);
        globalSnapshot.setSource("PRICING_SYNC");
        collectionValueSnapshotRepository.save(globalSnapshot);

        List<EditionValueSnapshot> editionSnapshots = new ArrayList<>();
        for (Map.Entry<Long, BigDecimal> entry : perEdition.entrySet()) {
            Edition edition = editionsById.get(entry.getKey());
            EditionValueSnapshot snapshot = new EditionValueSnapshot();
            snapshot.setRecordedAt(now);
            snapshot.setEditionId(entry.getKey());
            if (edition != null) {
                snapshot.setEditionCode(edition.getCode());
                snapshot.setEditionName(edition.getName());
            }
            snapshot.setTotalValueEur(entry.getValue().setScale(2, RoundingMode.HALF_UP));
            editionSnapshots.add(snapshot);
        }
        if (!editionSnapshots.isEmpty()) {
            editionValueSnapshotRepository.saveAll(editionSnapshots);
        }
    }

    private BigDecimal computeDelta(BigDecimal current, BigDecimal baseline) {
        if (current == null || baseline == null || baseline.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }

        BigDecimal delta = current.subtract(baseline)
                .divide(baseline, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
        return delta.setScale(2, RoundingMode.HALF_UP);
    }

    private EditionValueSnapshot findLatestSnapshotAtOrBefore(List<EditionValueSnapshot> snapshots, LocalDateTime threshold) {
        EditionValueSnapshot selected = null;
        for (EditionValueSnapshot snapshot : snapshots) {
            if (!snapshot.getRecordedAt().isAfter(threshold)) {
                selected = snapshot;
            }
        }
        return selected;
    }

    private static int safeInt(Integer value) {
        return value != null ? Math.max(0, value) : 0;
    }
}
