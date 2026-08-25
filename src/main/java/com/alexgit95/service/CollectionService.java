package com.alexgit95.service;

import com.alexgit95.dto.CardDTO;
import com.alexgit95.model.Card;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CollectionService {

    private final UserCollectionRepository collectionRepository;
    private final CardRepository cardRepository;
    private final CardService cardService;

    public CollectionService(UserCollectionRepository collectionRepository,
                             CardRepository cardRepository,
                             CardService cardService) {
        this.collectionRepository = collectionRepository;
        this.cardRepository = cardRepository;
        this.cardService = cardService;
    }

    @Transactional
    public CardDTO addCard(Long cardId, int quantity, int foilQuantity, boolean foil) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));

        int safeRegular = Math.max(0, quantity);
        int safeFoil = Math.max(0, foilQuantity);

        Optional<UserCollection> existing = collectionRepository.findByCardId(cardId);
        UserCollection uc;
        if (existing.isPresent()) {
            uc = existing.get();
            uc.setQuantity(Math.max(0, uc.getQuantity()) + safeRegular);
            uc.setFoilQuantity(Math.max(0, uc.getFoilQuantity()) + safeFoil);
        } else {
            uc = new UserCollection();
            uc.setCard(card);
            uc.setQuantity(safeRegular);
            uc.setFoilQuantity(safeFoil);
        }

        // Canonical invariant: foil reflects whether at least one foil copy exists.
        uc.setFoil(uc.getFoilQuantity() > 0);

        // Remove zero-quantity entries to preserve owned semantics consistency.
        if (uc.getQuantity() <= 0 && uc.getFoilQuantity() <= 0) {
            collectionRepository.findByCardId(cardId).ifPresent(collectionRepository::delete);
            return cardService.toDTO(card, null);
        }

        collectionRepository.save(uc);
        return cardService.toDTO(card, uc);
    }

    @Transactional
    public CardDTO updateQuantity(Long cardId, int quantity, int foilQuantity, Boolean foil) {
        // If both quantities are 0, remove the card
        if (quantity <= 0 && foilQuantity <= 0) {
            return removeCard(cardId);
        }
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));
        UserCollection uc = collectionRepository.findByCardId(cardId)
                .orElseGet(() -> {
                    UserCollection newUc = new UserCollection();
                    newUc.setCard(card);
                    return newUc;
                });
        uc.setQuantity(Math.max(0, quantity));
        uc.setFoilQuantity(Math.max(0, foilQuantity));
        uc.setFoil(uc.getFoilQuantity() > 0);
        collectionRepository.save(uc);
        return cardService.toDTO(card, uc);
    }

    @Transactional
    public CardDTO removeCard(Long cardId) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));
        collectionRepository.findByCardId(cardId)
                .ifPresent(collectionRepository::delete);
        return cardService.toDTO(card, null);
    }

    public List<CardDTO> getOwnedCards() {
        return collectionRepository.findAll().stream()
                .map(uc -> cardService.toDTO(uc.getCard(), uc))
                .collect(Collectors.toList());
    }

    private static final java.util.Set<Integer> ALLOWED_LIMITS = java.util.Set.of(10, 20, 25, 50);

    public List<CardDTO> getRecentCards(int limit) {
        int safeLimit = ALLOWED_LIMITS.contains(limit) ? limit : 20;
        return collectionRepository.findRecentWithCard(PageRequest.of(0, safeLimit)).stream()
                .map(uc -> cardService.toDTO(uc.getCard(), uc))
                .collect(Collectors.toList());
    }
}
