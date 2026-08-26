package com.alexgit95.controller;

import com.alexgit95.dto.PricingInsightsDTO;
import com.alexgit95.service.PricingInsightsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pricing")
public class PricingController {

    private final PricingInsightsService pricingInsightsService;

    public PricingController(PricingInsightsService pricingInsightsService) {
        this.pricingInsightsService = pricingInsightsService;
    }

    @GetMapping("/insights")
    public ResponseEntity<PricingInsightsDTO> getInsights() {
        return ResponseEntity.ok(pricingInsightsService.getInsights());
    }
}
