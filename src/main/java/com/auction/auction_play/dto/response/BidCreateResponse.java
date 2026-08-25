package com.auction.auction_play.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BidCreateResponse {
    private final Long bidId;
    private final Long auctionId;
    private final Long userId;
    private final Long amount;
    private final LocalDateTime createdAt;
    private final Long currentPrice;
    private final Long minBidAmount;
    private final Long myRemainingPoint;
    private final boolean isTopBidder;

}
