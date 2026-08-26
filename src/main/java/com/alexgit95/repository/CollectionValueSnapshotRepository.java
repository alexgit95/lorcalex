package com.alexgit95.repository;

import com.alexgit95.model.CollectionValueSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CollectionValueSnapshotRepository extends JpaRepository<CollectionValueSnapshot, Long> {

    List<CollectionValueSnapshot> findAllByOrderByRecordedAtAsc();
}
