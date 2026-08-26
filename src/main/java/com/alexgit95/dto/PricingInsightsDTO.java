package com.alexgit95.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class PricingInsightsDTO {
    private String currency;
    private BigDecimal totalCollectionValueEur;
    private int excludedNoPrice;
    private int excludedNonEur;
    private List<CardDTO> latestPricedCards;
    private List<PricingEditionValuationDTO> editionValuations;
}
