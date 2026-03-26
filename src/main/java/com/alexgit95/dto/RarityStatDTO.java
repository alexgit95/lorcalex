package com.alexgit95.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RarityStatDTO {
    private String rarity;
    private long totalCards;
    private long ownedCards;
    private long missingCards;
}
