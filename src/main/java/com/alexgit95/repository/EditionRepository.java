package com.alexgit95.repository;

import com.alexgit95.model.Edition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EditionRepository extends JpaRepository<Edition, Long> {
    Optional<Edition> findByCode(String code);
    boolean existsByCode(String code);
}
