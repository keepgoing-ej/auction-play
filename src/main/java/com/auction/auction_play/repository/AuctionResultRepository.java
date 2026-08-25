package com.auction.auction_play.repository;

import com.auction.auction_play.domain.AuctionResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionResultRepository extends JpaRepository<AuctionResult, Long> {
}
