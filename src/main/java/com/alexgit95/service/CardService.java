package com.alexgit95.service;

import com.alexgit95.dto.CardDTO;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CardService {

    private final CardRepository cardRepository;
    private final EditionRepository editionRepository;
    private final UserCollectionRepository collectionRepository;

    public CardService(CardRepository cardRepository,
                       EditionRepository editionRepository,
                       UserCollectionRepository collectionRepository) {
        this.cardRepository = cardRepository;
        this.editionRepository = editionRepository;
        this.collectionRepository = collectionRepository;
    }

    public List<CardDTO> getCardsByEdition(Long editionId) {
        List<Card> cards = cardRepository.findByEditionIdOrderByCardNumberAsc(editionId);
        List<UserCollection> ownedCards = collectionRepository.findByEditionId(editionId);

        Map<Long, UserCollection> ownedMap = ownedCards.stream()
                .collect(Collectors.toMap(uc -> uc.getCard().getId(), uc -> uc));

        return cards.stream()
                .map(card -> toDTO(card, ownedMap.get(card.getId())))
                .collect(Collectors.toList());
    }

    public List<CardDTO> getAllCardsWithCollection() {
        List<Card> cards = cardRepository.findAll();
        List<UserCollection> ownedCards = collectionRepository.findAll();

        Map<Long, UserCollection> ownedMap = ownedCards.stream()
                .collect(Collectors.toMap(uc -> uc.getCard().getId(), uc -> uc));

        return cards.stream()
                .sorted(Comparator
                        .comparingInt((Card c) -> {
                            Edition e = c.getEdition();
                            return (e != null && e.getSetNumber() != null) ? e.getSetNumber() : Integer.MAX_VALUE;
                        })
                        .thenComparingInt(c -> c.getCardNumber() != null ? c.getCardNumber() : Integer.MAX_VALUE))
                .map(card -> toDTO(card, ownedMap.get(card.getId())))
                .collect(Collectors.toList());
    }

    public List<CardDTO> searchCards(String query) {
        return cardRepository.searchByName(query)
                .stream()
                .sorted(Comparator
                        .comparingInt((Card c) -> {
                            Edition e = c.getEdition();
                            return (e != null && e.getSetNumber() != null) ? e.getSetNumber() : Integer.MAX_VALUE;
                        })
                        .thenComparingInt(c -> c.getCardNumber() != null ? c.getCardNumber() : Integer.MAX_VALUE))
                .map(card -> {
                    Optional<UserCollection> uc = collectionRepository.findByCardId(card.getId());
                    return toDTO(card, uc.orElse(null));
                })
                .collect(Collectors.toList());
    }

    public Optional<CardDTO> getCardById(Long id) {
        return cardRepository.findById(id)
                .map(card -> {
                    Optional<UserCollection> uc = collectionRepository.findByCardId(id);
                    return toDTO(card, uc.orElse(null));
                });
    }

    /**
     * Returns lightweight fingerprint data for all cards with a computed hash.
     */
    public List<Map<String, Object>> getAllFingerprints() {
        return cardRepository.findAll().stream()
                .filter(c -> c.getImageHash() != null)
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "n", c.getCardNumber() != null ? c.getCardNumber() : 0,
                        "s", c.getEdition() != null && c.getEdition().getCode() != null
                                ? c.getEdition().getCode() : "",
                        "sn", c.getEdition() != null && c.getEdition().getSetNumber() != null
                                ? c.getEdition().getSetNumber() : 0,
                        "h", Long.toHexString(c.getImageHash())
                ))
                .toList();
    }

    public Optional<CardDTO> setWanted(Long cardId, boolean wanted) {
        return cardRepository.findById(cardId)
                .map(card -> {
                    card.setWanted(wanted);
                    cardRepository.save(card);
                    Optional<UserCollection> uc = collectionRepository.findByCardId(cardId);
                    return toDTO(card, uc.orElse(null));
                });
    }

    public CardDTO toDTO(Card card, UserCollection uc) {
        CardDTO dto = new CardDTO();
        dto.setId(card.getId());
        dto.setName(card.getName());
        dto.setCardNumber(card.getCardNumber());
        dto.setRarity(card.getRarity());
        dto.setCost(card.getCost());
        dto.setInkColor(card.getInkColor());
        dto.setType(card.getType());
        dto.setSubtypes(card.getSubtypes());
        dto.setBodyText(card.getBodyText());
        dto.setFlavorText(card.getFlavorText());
        dto.setImageUrl(card.getImageUrl());
        dto.setArtist(card.getArtist());
        dto.setInkable(card.getInkable());
        dto.setMarketPrice(card.getMarketPrice());
        dto.setPriceCurrency(card.getPriceCurrency());
        dto.setPriceSource(card.getPriceSource());
        dto.setLastPriceAt(card.getLastPriceAt());
        dto.setLastPriceStatus(card.getLastPriceStatus());
        dto.setWanted(Boolean.TRUE.equals(card.getWanted()));
        if (card.getEdition() != null) {
            dto.setEditionId(card.getEdition().getId());
            dto.setEditionName(card.getEdition().getName());
            dto.setEditionCode(card.getEdition().getCode());
            dto.setEditionSetNumber(card.getEdition().getSetNumber());
        }
        int quantity = uc != null && uc.getQuantity() != null ? uc.getQuantity() : 0;
        int foilQuantity = uc != null && uc.getFoilQuantity() != null ? uc.getFoilQuantity() : 0;
        dto.setOwned(quantity > 0 || foilQuantity > 0);
        dto.setQuantity(quantity);
        dto.setFoilQuantity(foilQuantity);
        dto.setFoil(uc != null && Boolean.TRUE.equals(uc.getFoil()));
        dto.setFirstAddedAt(uc != null ? uc.getFirstAddedAt() : null);
        dto.setLastAddedAt(uc != null ? uc.getLastAddedAt() : null);
        return dto;
    }
}

