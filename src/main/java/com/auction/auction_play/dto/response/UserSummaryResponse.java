package com.auction.auction_play.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserSummaryResponse {
    private final Long userId;
    private final String nickname;
}
