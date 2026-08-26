package com.alexgit95.repository;

import com.alexgit95.model.UserCollection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCollectionRepository extends JpaRepository<UserCollection, Long> {

    Optional<UserCollection> findByCardId(Long cardId);

    boolean existsByCardId(Long cardId);

    @Query("SELECT uc FROM UserCollection uc JOIN uc.card c WHERE c.edition.id = :editionId")
    List<UserCollection> findByEditionId(@Param("editionId") Long editionId);

    long count();

    @Query("SELECT COUNT(uc) FROM UserCollection uc JOIN uc.card c WHERE c.edition.id = :editionId AND (uc.quantity > 0 OR uc.foilQuantity > 0)")
    long countByEditionId(@Param("editionId") Long editionId);

    @Query("SELECT COUNT(uc) FROM UserCollection uc JOIN uc.card c WHERE c.edition.id = :editionId AND c.rarity = :rarity AND (uc.quantity > 0 OR uc.foilQuantity > 0)")
    long countByEditionIdAndRarity(@Param("editionId") Long editionId, @Param("rarity") String rarity);

    @Query("SELECT COUNT(uc) FROM UserCollection uc WHERE uc.card.rarity IN :rarities AND (uc.quantity > 0 OR uc.foilQuantity > 0)")
    long countByCardRarityIn(@Param("rarities") List<String> rarities);

    @Query("SELECT COUNT(uc) FROM UserCollection uc WHERE uc.card.edition.id = :editionId AND uc.card.rarity IN :rarities AND (uc.quantity > 0 OR uc.foilQuantity > 0)")
    long countByEditionIdAndRarityIn(@Param("editionId") Long editionId, @Param("rarities") List<String> rarities);

    @Query("SELECT uc FROM UserCollection uc JOIN FETCH uc.card")
    List<UserCollection> findAllWithCard();

    @Query("SELECT uc FROM UserCollection uc JOIN FETCH uc.card c LEFT JOIN FETCH c.edition")
    List<UserCollection> findAllWithCardAndEdition();

    @Query("SELECT uc FROM UserCollection uc JOIN FETCH uc.card c LEFT JOIN FETCH c.edition ORDER BY uc.lastAddedAt DESC")
    List<UserCollection> findRecentWithCard(Pageable pageable);
}
