package com.auction.auction_play.service;


import com.auction.auction_play.domain.*;
import com.auction.auction_play.dto.request.BidCreateRequest;
import com.auction.auction_play.dto.response.BidCreateResponse;
import com.auction.auction_play.dto.response.BidSummaryResponse;
import com.auction.auction_play.dto.response.PageResponse;
import com.auction.auction_play.dto.response.UserSummaryResponse;
import com.auction.auction_play.exception.BusinessException;
import com.auction.auction_play.exception.ErrorCode;
import com.auction.auction_play.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BidService {
    private static final long MIN_BID_INCREMENT = 1000L;

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final UserRepository userRepository;
    private final ProductViewRepository productViewRepository;
    private final PointTransactionRepository pointTransactionRepository;

    // A-04 입찰
    @Transactional
    public BidCreateResponse bid(Long auctionId, Long userId, BidCreateRequest request) {

        // 1-4 경매인증
        // Lock
        Auction auction = auctionRepository.findByIdForUpdate(auctionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_NOT_FOUND));

        if (auction.getStatus() == AuctionStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.AUCTION_CANCELLED);
        }

        LocalDateTime now = LocalDateTime.now();

        if(now.isBefore(auction.getStartAt())) {
            throw new BusinessException(ErrorCode.AUCTION_NOT_STARTED);
        }

        if(!now.isBefore(auction.getEndAt())) {
            throw new BusinessException((ErrorCode.AUCTION_CLOSED));
        }
        // ===== 5. 사용자 검증 =====
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // ===== 6. 상품 조회 기록 검증 (이 서비스 고유 규칙) =====
        Long productId = auction.getProduct().getId();
        if (!productViewRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_VIEWED);
        }

        // ===== 7~8. 금액 검증 =====
        Long amount = request.getAmount();
        if (amount <= auction.getCurrentPrice()) {
            throw new BusinessException(ErrorCode.INVALID_BID_AMOUNT);
        }
        if (amount < auction.getCurrentPrice() + MIN_BID_INCREMENT) {
            throw new BusinessException(ErrorCode.INVALID_BID_AMOUNT);
        }

        // ===== 9. 포인트 검증 =====
        if (user.getPoint() < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_POINT);
        }

        // ===== 10. 이전 최고 입찰자 환급 =====
        Optional<Bid> previousTopBid = bidRepository.findTopBid(auctionId);

        if (previousTopBid.isPresent()) {
            Bid prev = previousTopBid.get();
            User prevBidder = prev.getUser();

            prevBidder.addPoint(prev.getAmount());

            pointTransactionRepository.save(PointTransaction.builder()
                    .user(prevBidder)
                    .type(PointTransactionType.REFUND)
                    .amount(prev.getAmount())
                    .balanceAfter(prevBidder.getPoint())
                    .build());
        }

        // ===== 11. 포인트 보류 → 입찰 저장 → 현재가 갱신 =====
        user.deductPoint(amount);

        pointTransactionRepository.save(PointTransaction.builder()
                .user(user)
                .type(PointTransactionType.BID_HOLD)
                .amount(-amount)
                .balanceAfter(user.getPoint())
                .build());

        Bid savedBid = bidRepository.save(Bid.builder()
                .auction(auction)
                .user(user)
                .amount(amount)
                .build());

        auction.updateCurrentPrice(amount);

        return BidCreateResponse.builder()
                .bidId(savedBid.getId())
                .auctionId(auction.getId())
                .userId(user.getId())
                .amount(amount)
                .createdAt(savedBid.getCreatedAt())
                .currentPrice(auction.getCurrentPrice())
                .minBidAmount(auction.getCurrentPrice() + MIN_BID_INCREMENT)
                .myRemainingPoint(user.getPoint())
                .isTopBidder(true)
                .build();
    }

    // A-05. 입찰 이력
    public PageResponse<BidSummaryResponse> getBids(Long auctionId, Pageable pageable) {

        if (!auctionRepository.existsById(auctionId)) {
            throw new BusinessException(ErrorCode.AUCTION_NOT_FOUND);
        }

        Page<BidSummaryResponse> page = bidRepository
                .findByAuctionIdWithUser(auctionId, pageable)
                .map(this::toSummaryResponse);

        return PageResponse.from(page);
    }

    private BidSummaryResponse toSummaryResponse(Bid bid) {
        return BidSummaryResponse.builder()
                .bidId(bid.getId())
                .amount(bid.getAmount())
                .createdAt(bid.getCreatedAt())
                .bidder(UserSummaryResponse.builder()
                        .userId(bid.getUser().getId())
                        .nickname(bid.getUser().getNickname())
                        .build())
                .build();
    }

}