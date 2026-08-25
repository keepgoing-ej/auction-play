package com.auction.auction_play.service;

import com.auction.auction_play.domain.Product;
import com.auction.auction_play.domain.ProductView;
import com.auction.auction_play.domain.User;
import com.auction.auction_play.dto.response.ProductViewResponse;
import com.auction.auction_play.exception.BusinessException;
import com.auction.auction_play.exception.ErrorCode;
import com.auction.auction_play.repository.ProductRepository;
import com.auction.auction_play.repository.ProductViewRepository;
import com.auction.auction_play.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductViewService {

    private final ProductViewRepository productViewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    // P-06. 조회 인정
    @Transactional
    public ProductViewResponse recordView(Long productId, Long userId) {

        // 이미 기록이 있으면 아무것도 하지 않고 성공 응답 (멱등)
        if (productViewRepository.existsByUserIdAndProductId(userId, productId)) {
            return toResponse(productId, userId);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        ProductView productView = ProductView.builder()
                .user(user)
                .product(product)
                .build();

        productViewRepository.save(productView);

        return toResponse(productId, userId);
    }

    private ProductViewResponse toResponse(Long productId, Long userId) {
        return ProductViewResponse.builder()
                .productId(productId)
                .userId(userId)
                .viewed(true)
                .build();
    }
}