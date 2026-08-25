package com.auction.auction_play.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ProductUpdateRequest {

    @Size(max = 100, message = "상품명은 100자를 초과할 수 없습니다.")
    private String name;

    @Size(max = 1000, message = "설명은 1000자를 초과할 수 없습니다.")
    private String description;

    @Size(max = 500, message = "이미지 URL은 500자를 초과할 수 없습니다.")
    private String imageUrl;

    @Size(max = 20)
    private String itemCondition;

    @Size(max = 20)
    private String rarity;

    @Min(value = 1, message = "예상 가치는 1 이상이어야 합니다.")
    private Long estimatedValue;
}