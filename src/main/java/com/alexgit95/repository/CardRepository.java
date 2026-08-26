package com.alexgit95.repository;

import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface CardRepository extends JpaRepository<Card, Long> {

    List<Card> findByEditionOrderByCardNumberAsc(Edition edition);

    List<Card> findByEditionIdOrderByCardNumberAsc(Long editionId);

    Optional<Card> findByCardNumberAndEdition(Integer cardNumber, Edition edition);

    Optional<Card> findByCardNumberAndEditionId(Integer cardNumber, Long editionId);

    Optional<Card> findByExternalId(String externalId);

    @Query("SELECT c FROM Card c JOIN c.edition e WHERE e.code = :editionCode AND c.cardNumber = :cardNumber")
    Optional<Card> findByEditionCodeAndCardNumber(@Param("editionCode") String editionCode,
                                                  @Param("cardNumber") Integer cardNumber);

    List<Card> findByExternalIdIn(Collection<String> externalIds);

    @Query("SELECT c FROM Card c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Card> searchByName(@Param("query") String query);

    long countByEdition(Edition edition);

    long countByEditionId(Long editionId);

    long countByRarityIn(List<String> rarities);

    long countByEditionAndRarityIn(Edition edition, List<String> rarities);

    List<Card> findByImageHashIsNull();

    List<Card> findByLastPriceAtIsNullOrderByIdAsc();

    List<Card> findByMarketPriceIsNullOrderByIdAsc();

    List<Card> findByMarketPriceIsNotNullAndLastPriceAtBeforeOrderByLastPriceAtAscIdAsc(LocalDateTime before);

    List<Card> findByMarketPriceIsNotNullAndLastPriceAtIsNotNullOrderByLastPriceAtDescIdDesc();

    List<Card> findByLastPriceAtIsNotNullOrderByLastPriceAtAscIdAsc();

    List<Card> findByLastPriceAtIsNotNullOrderByLastPriceAtDescIdDesc(Pageable pageable);

    long countByLastPriceAtIsNull();

    long countByMarketPriceIsNull();

    long countByLastPriceAtIsNotNull();
}
