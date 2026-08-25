package com.auction.auction_play.dto.response;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ProductSummaryResponse {
    private final Long id;
    private final String name;
    private final String imageUrl;
    private final String itemCondition;
    private final String rarity;
    private final Long estimatedValue;
}
