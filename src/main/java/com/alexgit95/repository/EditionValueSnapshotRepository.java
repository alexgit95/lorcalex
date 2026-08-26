package com.alexgit95.repository;

import com.alexgit95.model.EditionValueSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface EditionValueSnapshotRepository extends JpaRepository<EditionValueSnapshot, Long> {

    List<EditionValueSnapshot> findByEditionIdOrderByRecordedAtAsc(Long editionId);

    List<EditionValueSnapshot> findByEditionIdAndRecordedAtBetweenOrderByRecordedAtAsc(
            Long editionId, LocalDateTime from, LocalDateTime to);
}
