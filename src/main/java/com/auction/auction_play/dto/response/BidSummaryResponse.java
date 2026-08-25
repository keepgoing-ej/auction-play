package com.auction.auction_play.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class BidSummaryResponse {
    private final Long bidId;
    private final Long amount;
    private final LocalDateTime createdAt;
    private final UserSummaryResponse bidder;
}
