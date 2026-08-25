package com.auction.auction_play.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProductViewResponse {
    private final Long productId;
    private final Long userId;
    private final boolean viewed;
}
