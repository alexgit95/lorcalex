package com.alexgit95.service;

import com.alexgit95.dto.CardDTO;
import com.alexgit95.model.Card;
import com.alexgit95.model.UserCollection;
import com.alexgit95.repository.CardRepository;
import com.alexgit95.repository.UserCollectionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public CardDTO addCard(Long cardId, int quantity, boolean foil) {
        Card card = cardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("Card not found: " + cardId));

        Optional<UserCollection> existing = collectionRepository.findByCardId(cardId);
        UserCollection uc;
        if (existing.isPresent()) {
            uc = existing.get();
            uc.setQuantity(uc.getQuantity() + quantity);
            uc.setFoil(foil);
        } else {
            uc = new UserCollection();
            uc.setCard(card);
            uc.setQuantity(quantity);
            uc.setFoil(foil);
        }
        collectionRepository.save(uc);
        return cardService.toDTO(card, uc);
    }

    @Transactional
    public CardDTO updateQuantity(Long cardId, int quantity, Boolean foil) {
        if (quantity <= 0) {
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
        uc.setQuantity(quantity);
        if (foil != null) {
            uc.setFoil(foil);
        }
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
}
