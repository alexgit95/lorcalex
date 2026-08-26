package com.alexgit95.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class EditionDeltaDTO {
    private Long editionId;
    private String editionCode;
    private String editionName;
    private BigDecimal currentValueEur;
    private BigDecimal value7dEur;
    private BigDecimal value30dEur;
    private BigDecimal delta7dPercent;
    private BigDecimal delta30dPercent;
}
