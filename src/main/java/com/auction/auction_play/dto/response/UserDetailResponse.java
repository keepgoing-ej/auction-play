package com.auction.auction_play.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class UserDetailResponse {
    private final Long id;
    private final String email;
    private final String nickname;
    private Long point;
    private LocalDateTime createdAt;
}
