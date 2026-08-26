package com.alexgit95.controller;

import com.alexgit95.dto.CollectionValueTrendDTO;
import com.alexgit95.dto.EditionDeltaDTO;
import com.alexgit95.dto.PricingInsightsDTO;
import com.alexgit95.service.CollectionValueTrendService;
import com.alexgit95.service.PricingInsightsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final PricingInsightsService pricingInsightsService;
    private final CollectionValueTrendService collectionValueTrendService;

    public PricingController(PricingInsightsService pricingInsightsService,
                            CollectionValueTrendService collectionValueTrendService) {
        this.pricingInsightsService = pricingInsightsService;
        this.collectionValueTrendService = collectionValueTrendService;
    }

    @GetMapping("/insights")
    public ResponseEntity<PricingInsightsDTO> getInsights() {
        return ResponseEntity.ok(pricingInsightsService.getInsights());
    }

    @DeleteMapping("/cards/{cardId}/price")
    public ResponseEntity<com.alexgit95.dto.CardDTO> removePrice(@PathVariable Long cardId) {
        return ResponseEntity.ok(pricingInsightsService.removePrice(cardId));
    }

    @GetMapping("/trend")
    public ResponseEntity<CollectionValueTrendDTO> getTrend() {
        return ResponseEntity.ok(collectionValueTrendService.getTrend());
    }

    @GetMapping("/edition-deltas")
    public ResponseEntity<List<EditionDeltaDTO>> getEditionDeltas() {
        return ResponseEntity.ok(collectionValueTrendService.getEditionDeltas());
    }
}
