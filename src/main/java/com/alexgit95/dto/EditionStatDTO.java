package com.alexgit95.dto;

import lombok.Data;
import java.util.List;

@Data
public class EditionStatDTO {
    private Long editionId;
    private String editionName;
    private String editionCode;
    private long totalCards;
    private long ownedCards;
    private long missingCards;
    private double completionPercentage;
    private List<RarityStatDTO> byRarity;
}
