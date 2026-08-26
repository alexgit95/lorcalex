package com.alexgit95.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class PricingEditionValuationDTO {
    private Long editionId;
    private String editionName;
    private String editionCode;
    private Integer editionSetNumber;
    private BigDecimal totalValueEur;
}
