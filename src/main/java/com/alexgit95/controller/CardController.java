package com.alexgit95.controller;

import com.alexgit95.dto.CardDTO;
import com.alexgit95.model.Edition;
import com.alexgit95.repository.EditionRepository;
import com.alexgit95.service.CardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CardController {

    private final CardService cardService;
    private final EditionRepository editionRepository;

    public CardController(CardService cardService, EditionRepository editionRepository) {
        this.cardService = cardService;
        this.editionRepository = editionRepository;
    }

    @GetMapping("/editions")
    public ResponseEntity<List<Map<String, Object>>> getEditions() {
        List<Map<String, Object>> editions = editionRepository.findAll().stream()
                .sorted(Comparator.comparingInt(e -> (e.getSetNumber() != null ? e.getSetNumber() : Integer.MAX_VALUE)))
                .map(e -> Map.<String, Object>of(
                        "id", e.getId(),
                        "name", e.getName(),
                        "code", e.getCode() != null ? e.getCode() : "",
                        "setNumber", e.getSetNumber() != null ? e.getSetNumber() : 0,
                        "totalCards", e.getTotalCards() != null ? e.getTotalCards() : 0,
                        "releaseDate", e.getReleaseDate() != null ? e.getReleaseDate() : "",
                        "logoUrl", e.getLogoUrl() != null ? e.getLogoUrl() : ""
                ))
                .toList();
        return ResponseEntity.ok(editions);
    }

    @GetMapping("/cards")
    public ResponseEntity<List<CardDTO>> getCards(
            @RequestParam(required = false) Long editionId,
            @RequestParam(required = false) String q) {

        if (q != null && !q.isBlank()) {
            return ResponseEntity.ok(cardService.searchCards(q));
        }
        if (editionId != null) {
            return ResponseEntity.ok(cardService.getCardsByEdition(editionId));
        }
        return ResponseEntity.ok(cardService.getAllCardsWithCollection());
    }

    @GetMapping("/cards/{id}")
    public ResponseEntity<CardDTO> getCard(@PathVariable Long id) {
        return cardService.getCardById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cards/lookup")
    public ResponseEntity<List<CardDTO>> lookupCard(
            @RequestParam Integer number,
            @RequestParam(required = false) Long editionId) {
        if (editionId != null) {
            return cardService.getCardsByEdition(editionId).stream()
                    .filter(c -> number.equals(c.getCardNumber()))
                    .findFirst()
                    .map(c -> ResponseEntity.ok(List.of(c)))
                    .orElse(ResponseEntity.ok(List.of()));
        }
        List<CardDTO> results = cardService.getAllCardsWithCollection().stream()
                .filter(c -> number.equals(c.getCardNumber()))
                .toList();
        return ResponseEntity.ok(results);
    }

    /**
     * Returns lightweight fingerprint data for all cards that have a computed hash.
     * Used by the client-side scanner for full-card recognition.
     * Response: [{id, n (cardNumber), s (setCode/editionCode), h (hex hash)}]
     */
    @GetMapping("/cards/fingerprints")
    public ResponseEntity<List<Map<String, Object>>> getFingerprints() {
        List<Map<String, Object>> fingerprints = cardService.getAllFingerprints();
        return ResponseEntity.ok(fingerprints);
    }
}

