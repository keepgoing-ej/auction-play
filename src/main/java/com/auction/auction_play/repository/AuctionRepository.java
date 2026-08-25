package com.auction.auction_play.repository;

import com.auction.auction_play.domain.Auction;
import com.auction.auction_play.domain.AuctionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AuctionRepository extends JpaRepository<Auction, Long> {

    boolean existsByProductId(Long productId);
    /* Page<Auction> findByStatus(AuctionStatus status, Pageable pageable);*/

    //쿼리로 개선
    @Query(value = "select a from Auction a join fetch a.product",
            countQuery = "select count(a) from Auction a")
    Page<Auction> findAllWithProduct(Pageable pageable);

    @Query(value = "select a from Auction a join fetch a.product where a.status = :status",
            countQuery = "select count(a) from Auction a where a.status = :status")
    Page<Auction> findByStatusWithProduct(AuctionStatus status, Pageable pageable);

    @Query("select a from Auction a join fetch a.product where a.id = :id")
    Optional<Auction> findByIdWithProduct(Long id);

    // 8/24 추가

    @Query("select a from Auction a where a.status = :status and a.startAt <= :now")
    List<Auction> findStartTargets(AuctionStatus status, LocalDateTime now);

    @Query("select a from Auction a join fetch a.product " +
            "where a.status = :status and a.endAt <= :now")
    List<Auction> findCloseTargets(AuctionStatus status, LocalDateTime now);

    // 8/25 Lock 추가
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Auction a join fetch a.product where a.id = :id")
    Optional<Auction> findByIdForUpdate(Long id);
}