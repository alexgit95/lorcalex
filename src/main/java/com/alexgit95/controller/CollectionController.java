package com.alexgit95.controller;

import com.alexgit95.dto.CardDTO;
import com.alexgit95.service.CollectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/collection")
public class CollectionController {

    private final CollectionService collectionService;

    public CollectionController(CollectionService collectionService) {
        this.collectionService = collectionService;
    }

    @GetMapping
    public ResponseEntity<List<CardDTO>> getOwnedCards() {
        return ResponseEntity.ok(collectionService.getOwnedCards());
    }

    @PostMapping
    public ResponseEntity<CardDTO> addCard(@RequestBody Map<String, Object> body) {
        Long cardId = Long.parseLong(String.valueOf(body.get("cardId")));
        int quantity = body.containsKey("quantity")
                ? Integer.parseInt(String.valueOf(body.get("quantity"))) : 1;
        boolean foil = body.containsKey("foil") && Boolean.parseBoolean(String.valueOf(body.get("foil")));
        return ResponseEntity.ok(collectionService.addCard(cardId, quantity, foil));
    }

    @PutMapping("/{cardId}")
    public ResponseEntity<CardDTO> updateQuantity(
            @PathVariable Long cardId,
            @RequestBody Map<String, Object> body) {
        int quantity = Integer.parseInt(String.valueOf(body.get("quantity")));
        Boolean foil = body.containsKey("foil") ? Boolean.parseBoolean(String.valueOf(body.get("foil"))) : null;
        return ResponseEntity.ok(collectionService.updateQuantity(cardId, quantity, foil));
    }

    @DeleteMapping("/{cardId}")
    public ResponseEntity<CardDTO> removeCard(@PathVariable Long cardId) {
        return ResponseEntity.ok(collectionService.removeCard(cardId));
    }
}
