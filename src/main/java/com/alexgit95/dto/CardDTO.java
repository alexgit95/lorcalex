package com.alexgit95.dto;

import lombok.Data;

@Data
public class CardDTO {
    private Long id;
    private String name;
    private Integer cardNumber;
    private String rarity;
    private Integer cost;
    private String inkColor;
    private String type;
    private String subtypes;
    private String bodyText;
    private String flavorText;
    private String imageUrl;
    private String artist;
    private Boolean inkable;
    private java.math.BigDecimal marketPrice;
    private String priceCurrency;
    private String priceSource;
    private java.time.LocalDateTime lastPriceAt;
    private String lastPriceStatus;
    private Long editionId;
    private String editionName;
    private String editionCode;
    private Integer editionSetNumber;
    // Collection info (null if not fetched with collection context)
    private Boolean owned;
    private Integer quantity;
    private Integer foilQuantity;
    private Boolean foil;
    private java.time.LocalDateTime firstAddedAt;
    private java.time.LocalDateTime lastAddedAt;
}
