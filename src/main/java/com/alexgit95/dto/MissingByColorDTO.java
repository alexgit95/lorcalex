package com.alexgit95.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MissingByColorDTO {
    private String inkColor;
    private List<RarityCountDTO> byRarity;
}
