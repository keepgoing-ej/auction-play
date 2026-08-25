package com.auction.auction_play.dto.response;

import com.auction.auction_play.domain.AuctionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuctionSummaryResponse {

    private final Long id;
    private final AuctionStatus status;
    private final Long currentPrice;
    private final LocalDateTime endAt;
    private final Long bidCount;
    private final ProductSummaryResponse product;
}