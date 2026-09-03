package com.alexgit95.dto;

import lombok.Data;

import java.util.List;

@Data
public class CollectionValueTrendDTO {
    private List<CollectionValueTrendPointDTO> trend;
}

