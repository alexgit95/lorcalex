package com.alexgit95.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RarityCountDTO {
    private String rarity;
    private long missingCards;
}
