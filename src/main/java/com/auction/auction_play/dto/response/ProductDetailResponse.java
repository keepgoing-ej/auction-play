package com.auction.auction_play.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProductDetailResponse {
    private final Long id;
    private final String name;
    private final String description;
    private final String imageUrl;
    private final String itemCondition;
    private final String rarity;
    private final Long estimatedValue;
    private final LocalDateTime createdAt;
}
