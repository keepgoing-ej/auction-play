package com.auction.auction_play.repository;

import com.auction.auction_play.domain.ProductView;
import com.auction.auction_play.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductViewRepository extends JpaRepository<ProductView, Long> {
    // 8/21 추가
    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
