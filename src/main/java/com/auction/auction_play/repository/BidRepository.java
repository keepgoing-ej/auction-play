package com.auction.auction_play.repository;

import com.auction.auction_play.domain.Bid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface BidRepository extends JpaRepository<Bid, Long> {
    // 추가
    long countByAuctionId(Long auctionId);
    // 수정
    @Query("select b.auction.id, count(b) from Bid b where b.auction.id in :auctionIds group by b.auction.id")
    List<Object[]> countByAuctionIds(List<Long> auctionIds);

    // 8/21 추가
    @Query("select b from Bid b join fetch b.user where b.auction.id = :auctionId")
    Page<Bid> findByAuctionIdWithUser(Long auctionId, Pageable pageable);

    @Query("select b from Bid b join fetch b.user where b.auction.id = :auctionId " +
            "order by b.amount desc, b.createdAt asc limit 1")
    Optional<Bid> findTopBid(Long auctionId);
}
