package com.alexgit95.dto;

import lombok.Data;
import java.util.List;

@Data
public class StatisticsDTO {
    private long totalCards;
    private long ownedCards;
    private long missingCards;
    private double completionPercentage;
    private List<EditionStatDTO> byEdition;
}
