package com.alexgit95.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CollectionValueTrendPointDTO {
    private LocalDateTime recordedAt;
    private BigDecimal totalCollectionValueEur;
}
