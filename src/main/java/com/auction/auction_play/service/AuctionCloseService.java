package com.auction.auction_play.service;

import com.auction.auction_play.domain.*;
import com.auction.auction_play.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuctionCloseService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionResultRepository auctionResultRepository;
    private final PointTransactionRepository pointTransactionRepository;

    // ===== 대상 조회 (트랜잭션 밖) =====

    @Transactional(readOnly = true)
    public List<Long> findStartTargetIds(LocalDateTime now) {
        return auctionRepository
                .findStartTargets(AuctionStatus.SCHEDULED, now)
                .stream()
                .map(Auction::getId)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Long> findCloseTargetIds(LocalDateTime now) {
        return auctionRepository
                .findCloseTargets(AuctionStatus.RUNNING, now)
                .stream()
                .map(Auction::getId)
                .toList();
    }

    // ===== 건별 처리 (각각 트랜잭션) =====

    @Transactional
    public void startOne(Long auctionId) {
        Auction auction = auctionRepository.findById(auctionId).orElse(null);

        if (auction == null || auction.getStatus() != AuctionStatus.SCHEDULED) {
            return;                                  // 이미 처리됐으면 조용히 넘어감
        }

        auction.start();
        log.info("경매 시작 — id={}", auctionId);
    }

    @Transactional
    public void closeOne(Long auctionId) {
        Auction auction = auctionRepository.findByIdWithProduct(auctionId).orElse(null);

        if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) {
            return;                                  // 중복 처리 방지
        }

        Optional<Bid> topBid = bidRepository.findTopBid(auctionId);

        if (topBid.isPresent()) {
            closeWithWinner(auction, topBid.get());
        } else {
            closeWithoutWinner(auction);
        }
    }

    // ===== 낙찰 =====

    private void closeWithWinner(Auction auction, Bid topBid) {
        User winner = topBid.getUser();
        Long finalPrice = topBid.getAmount();
        Long estimatedValue = auction.getProduct().getEstimatedValue();

        auction.close();

        auctionResultRepository.save(AuctionResult.builder()
                .auction(auction)
                .winner(winner)
                .finalPrice(finalPrice)
                .estimatedValue(estimatedValue)
                .profit(estimatedValue - finalPrice)
                .build());

        // 보류 상태를 구매로 확정 — 잔액 변동 없음
        pointTransactionRepository.save(PointTransaction.builder()
                .user(winner)
                .type(PointTransactionType.PURCHASE)
                .amount(0L)
                .balanceAfter(winner.getPoint())
                .build());

        log.info("낙찰 — id={}, 낙찰자={}, 낙찰가={}, 손익={}",
                auction.getId(), winner.getNickname(), finalPrice, estimatedValue - finalPrice);
    }

    // ===== 유찰 =====

    private void closeWithoutWinner(Auction auction) {
        auction.cancel();

        auctionResultRepository.save(AuctionResult.builder()
                .auction(auction)
                .winner(null)
                .finalPrice(null)
                .estimatedValue(auction.getProduct().getEstimatedValue())
                .profit(null)
                .build());

        log.info("유찰 — id={}, 입찰 없음", auction.getId());
    }
}