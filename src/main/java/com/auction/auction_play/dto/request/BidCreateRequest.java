package com.auction.auction_play.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BidCreateRequest {

    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @NotNull(message = "입찰 금액은 필수입니다.")
    @Min(value = 1, message = "입찰 금액은 1 이상이어야 합니다.")
    private Long amount;
}