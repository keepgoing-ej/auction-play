package com.auction.auction_play.dto.response;

import com.auction.auction_play.domain.AuctionStatus;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder

public class AuctionDetailResponse {
    private final Long id;
    private final AuctionStatus status;
    private final Long startPrice;
    private final Long currentPrice;
    private final Long minBidAmount;
    private final LocalDateTime startAt;
    private final LocalDateTime endAt;
    private final LocalDateTime createdAt;
    private final Long bidCount;
    private final ProductSummaryResponse product;

}
