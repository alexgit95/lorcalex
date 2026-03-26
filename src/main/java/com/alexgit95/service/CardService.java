package com.alexgit95.service;

import com.alexgit95.dto.CardDTO;
import com.alexgit95.model.Card;
import com.alexgit95.model.Edition;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.springframework.stereotype.Service;

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
                .sorted((a, b) -> {
                    if (a.getEdition() != null && b.getEdition() != null) {
                        int edCmp = a.getEdition().getId().compareTo(b.getEdition().getId());
                        if (edCmp != 0) return edCmp;
                    }
                    if (a.getCardNumber() != null && b.getCardNumber() != null) {
                        return a.getCardNumber().compareTo(b.getCardNumber());
                    }
                    return 0;
                })
                .map(card -> toDTO(card, ownedMap.get(card.getId())))
                .collect(Collectors.toList());
    }

    public List<CardDTO> searchCards(String query) {
        return cardRepository.searchByName(query)
                .stream()
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
        if (card.getEdition() != null) {
            dto.setEditionId(card.getEdition().getId());
            dto.setEditionName(card.getEdition().getName());
            dto.setEditionCode(card.getEdition().getCode());
        }
        dto.setOwned(uc != null);
        dto.setQuantity(uc != null ? uc.getQuantity() : 0);
        return dto;
    }
}
